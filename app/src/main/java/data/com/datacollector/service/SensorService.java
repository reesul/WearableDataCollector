package data.com.datacollector.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import data.com.datacollector.interfaces.ServiceStatusInterface;
import data.com.datacollector.model.Const;
import data.com.datacollector.model.PPGData;
import data.com.datacollector.model.SensorData;
import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Notifications;
import data.com.datacollector.utility.Util;
import data.com.datacollector.utility.predictionModels.LogisticRegression;
import data.com.datacollector.view.HomeActivity;

import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_DATA_AND_STOP;
import static data.com.datacollector.model.Const.DEFAULT_ACTIVITIES_LIST_TEXT;
import static data.com.datacollector.model.Const.DEVICE_ID;
import static data.com.datacollector.model.Const.SENSOR_DATA_MIN_INTERVAL_NANOS;
import static data.com.datacollector.model.Const.SENSOR_QUEUE_LATENCY;

/**
 * Service to enable SensorManager's sensor data collection
 * In the current implementation data related to Accelerometer, Gyroscope and Heart
 * rate Sensors are being collected.
 * This implementation can be further extended to collect ddata related to other sensors
 * supported by SensorManager.
 */
public class SensorService extends Service implements SensorEventListener, ServiceStatusInterface {
    private final String TAG = "DC_SensorService";

    /** Android SensorManager, to access data for Accelerometer, Gyroscope and Heart rate sensors*/
    private SensorManager sensorManager;

    /** vars to store time when last the concerned sensor data was stored(locally). This helps
     * in keeping data collection frequency in check.*/
    private long lastUpdateAccel = 0, lastUpdateGyro = 0, lastUpdatePPG = 0;

    /** local object for list of data of concerned type; starts*/
    List<SensorData> listGyroData = new ArrayList<>();
    List<SensorData> listAccelData = new ArrayList<>();
    List<PPGData> listPPGData = new ArrayList<>();
    /** local object for list of data of concerned type; ends*/

    private AlarmManager alarmManager;

    /** pending Intent to be used with alarm manager*/
    private PendingIntent pendingIntent;

    public static boolean isServiceRunning = false;

    //Service worker thread variables based on android guidelines
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    //We force the app to stay up so we can get sensor data
    private PowerManager.WakeLock mWakeLock = null;

    //Handler that periodically runs code for predictions
    private Handler predictionHandler = new Handler();

    //TODO: Verify, if this drains battery, then we can do a check on the sensor average values, if it changes a lot then trigger it, if not do not.
    private int PREDICTION_INTERVAL = 5000; //Milliseconds between each prediction
    private LogisticRegression logisticRegression = null; //The model to be used
    private int FEATURES = 3; //The number of features to be computed
    private int LABELS = 9; //Labels to be predicted
    private int WINDOW_SIZE = 2; //Window size for prediction
    private int AVERAGE_SAMPLING_RATE = 25; //Average number of samples being sampled at every second
    private double MODEL_PROB_THRESHOLD_FOR_FEEDBACK = 0.6; //The minimum probability that the model has to get in a prediction
    private Random r = new Random();
    private int percentageOfTimesForRandomConfirmation = 10; //When the prediction is above the threshold, with what probability will we request feedback anyways
    private double features[];
    double prob[];
    double higherProb = 0;
    int higherLblId = 0;
    /**
     * Thread that issues the prediction task every PREDICTION_INTERVAL seconds
     */

    //TODO: If the app is minimized, etc. is this still running?
    private final Runnable predictionRunnable = new Runnable(){//Thread that will run the prediction
        public void run(){
            if(logisticRegression == null){
                Log.d(TAG, "run: There was an error and the logistic regression model is not set, cancelling further predictions");
            } else {
                //Make the prediction and determine if we need feedback
                try {
                    //Get snapshot of data (Previous window)
                    int samplesToUse = WINDOW_SIZE * AVERAGE_SAMPLING_RATE;
                    if(listAccelData.size() > samplesToUse) {

                        List<SensorData> windowData = listAccelData.subList(listAccelData.size()-samplesToUse, listAccelData.size());

                        //TODO: Get the three or four highest values and its probability. From that, if the prob of the highest is below a TH then ask for feedback. We could use the other two to show
                        //TODO: only a few of the labels (the closer)
                        //TODO: Use two models. With and without context.
                        //TODO: Think how to store the features (context / no-context) in files

                        features = logisticRegression.getFeatures(windowData);
                        prob = logisticRegression.predict(features);
                        higherProb = 0;
                        higherLblId = 0;
                        for (int lblId = 0; lblId<LABELS; lblId++){
                            if(prob[lblId]>higherProb){
                                higherProb = prob[lblId];
                                higherLblId = lblId;
                            }
                        }
                        Log.d(TAG, "run: PROBABILITY: " + higherProb);
                        Log.d(TAG, "run: PREDICTED:   " + DEFAULT_ACTIVITIES_LIST_TEXT[higherLblId]);

                        //Verifying threshold
                        if(higherProb<MODEL_PROB_THRESHOLD_FOR_FEEDBACK){
                            Log.d(TAG, "run: Model's probability output below threshold. Requesting feedback");
                            //Request feedback
                            //TODO: Change this to save the features
                            //TODO: What happens if multiple notifications are overlapped?
                            Notifications.requestFeedback(SensorService.this,"Are you " + DEFAULT_ACTIVITIES_LIST_TEXT[higherLblId] ,DEFAULT_ACTIVITIES_LIST_TEXT[higherLblId],features);
                            //TODO: Stop predictions until this information is submitted
                            //TODO: Set up a time limit for the feedback response (If after X seconds no response is given, ignore this feedback session).
                            //TODO: Add NONE class (none of the postures)
                        }else{
                            //Every once in a while prompt for confirmation. Lets say, with 10% of probability we will ask for feedback even if we are above the threshold
                            int randN = r.nextInt(99)+1;
                            if(randN<percentageOfTimesForRandomConfirmation){
                                Log.d(TAG, "run: Random confirmation needed ");
                                Notifications.requestFeedback(SensorService.this,"Are you " + DEFAULT_ACTIVITIES_LIST_TEXT[higherLblId] ,DEFAULT_ACTIVITIES_LIST_TEXT[higherLblId],features);
                            }

                        }

                    }else{
                        Log.d(TAG, "run: Not enough data so prediction is not made");
                    }
                } catch (Exception e) {
                    Log.d(TAG, "run: There was an execption so the prediction could not be made: "+e.getMessage());
                    e.printStackTrace();
                }

                //This issues another prediction task
                predictionHandler.postDelayed(predictionRunnable, PREDICTION_INTERVAL);
            }
        }
    };


    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: called from ServiceHandler, worker thread");
            //The alarm for data saving to files is already included in the BLE service
            initSensor();
            isServiceRunning = true;

            if(mWakeLock == null){
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SensorWakeLock");
            }

            mWakeLock.acquire();
            Log.d(TAG, "handleMessage: called from ServiceHandler, worker thread finished setup");
        }
    }

    public SensorService() {
    }

    @Override
    public boolean isServiceRunning() {
        return isServiceRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        // Start up the thread running the service and in background priority to not disrupt the UI
        Log.d(TAG, "onCreate: Creating thread");
        HandlerThread thread = new HandlerThread("SensorServiceThread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Log.d(TAG, "onCreate: Finished creating thread");

        Log.d(TAG, "onCreate: Creating predictive model");
        initLRModel();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        //initialize the timestamp reference so that sensor events have accurate timestamps
        //TODO: Is this synchronous? Can we ensure that will this always be executed before getting our actual sensor data?
        Util.initTimeStamps(this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: The SensorService has started");
        Log.d(TAG, "onStartCommand: Intent " + intent);

        /*If the system kills our service, because START_STICKY property, the system restarts our service
         with a null intent unless there are pending intents to start the service which is NOT our case
         since we do not pending intents to start our service. but this is important to consider if there is
         any bug */
        if(intent != null) {

            //When the alarm fires, it broadcast a message which is received by the DataCollectReceiver
            //which then starts the services and this method is called. Here, we validate if we were
            //called to start the service, or to save the data.
            if (intent.getBooleanExtra("save_data", false)) {
                //The service is still running, we only save data
                Log.d(TAG, "onStartCommand: intent with save_data");
                if (!FileUtil.lastUploadResult)
                    Log.d(TAG, "last attempt to upload data failed");
                saveDataToFile(intent.getBooleanExtra(BROADCAST_DATA_SAVE_DATA_AND_STOP, false));
            }else{
                //Start from activity
                Log.d(TAG, "onStartCommand: intent without save_data");
                startService(startId);
            }
        }else{
            //Start due STICKY property
            Log.d(TAG, "onStartCommand: intent null");
            startService(startId);

        }

        if (Util.getStartTime()==0)
            //ensure that the timestamp reference point is set up, will be zero if not initialized
            Util.initTimeStamps(this);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    public void startPredictionTask() {
        predictionRunnable.run();
    }

    public void stopPredictionTask() {
        predictionHandler.removeCallbacks(predictionRunnable);
    }

    public void initLRModel(){
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCmodels";
        logisticRegression = new LogisticRegression(FEATURES, LABELS);
        try {
            logisticRegression.setCoefficients(path, "latest.txt");
            logisticRegression.setLabels(DEFAULT_ACTIVITIES_LIST_TEXT);
        } catch (Exception e) {
            Log.d(TAG, "onCreate: There was an error setting up the logisticRegression model: " + e.getMessage());
            logisticRegression = null;
            e.printStackTrace();
        }
    }

    /**
     * Starts our service and creates the notification. This method can be called when the user
     * starts the service on when it is started by the system
     *
     * @param startId
     */
    public void startService(int startId) {
        startForeground(Notifications.NOTIFICATION_ID_RUNNING_SERVICES, Notifications.getServiceRunningNotification(getApplicationContext(), HomeActivity.class));
        sendMessageToWorkerThread(startId);
        startPredictionTask();
        if(logisticRegression == null){
            initLRModel();
        }
    }

    /**
     * This is how we actually start our service in our worker thread
     * @param startId The start id of the service
     */
    public void sendMessageToWorkerThread(int startId){
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Log.d(TAG, "onStartCommand: Sending start request to working thread");
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);
        Log.d(TAG, "onStartCommand: Sent start request to working thread");
    }

    /**
     * initializes sensor objects and related listeners
     */
    private void initSensor(){
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //unneccessary, we will always want to save the first data
        //lastUpdateAccel = lastUpdateGyro = lastUpdatePPG = System.currentTimeMillis();
        registerListeners();
    }

    /**
     * Registers listeners to receive data from the required sensors supported by SensorEventListener
     */
    private void registerListeners(){

        //!!!Sensor delay can also be expressed as an int in microseconds, and can use an extra int to give max queue time
        //sampling period is considered a suggestion, and must be in microseconds

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                (int)Const.SENSOR_DATA_MIN_INTERVAL*1000, SENSOR_QUEUE_LATENCY );


        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                (int)Const.SENSOR_DATA_MIN_INTERVAL*1000, SENSOR_QUEUE_LATENCY);


        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE),
                (int)Const.SENSOR_DATA_MIN_INTERVAL*1000, SENSOR_QUEUE_LATENCY);


        /* //This is for raw PPG data on Polar watches, not implemented
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(65545),
                SensorManager.SENSOR_DELAY_NORMAL);
        */
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {



        switch(event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                processAccelerometerData(event);
                break;

            case Sensor.TYPE_GYROSCOPE:
                processGyroData(event);
                break;


          //TODO: enable this code if PPG data should be used
            case Sensor.TYPE_HEART_RATE:
                //check if sensor is not in touch with user or sensor status itself is unreliable, then ignore!
                if(event.accuracy == SensorManager.SENSOR_STATUS_NO_CONTACT || event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
                    return;


                processHeartRateData(event);
                break;

            /*
            case 65545:
                Log.d(TAG, "onSensorChanged:: PPG: "+event.values[0]);
                break;
            */
            default:
                Log.d(TAG, "onSensorChanged:: "+event.values[0]);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * to be called by onSensorChanged when PPG data is received.
     * It forwards the data for saving the same in the Sensor list.
     * @param event
     */
    private void processHeartRateData(SensorEvent event){
        //interval between two data entry should be min SENSOR_DATA_MIN_INTERVAL
        if ((event.timestamp - lastUpdatePPG) < SENSOR_DATA_MIN_INTERVAL_NANOS) {
            return;
        }
        lastUpdatePPG = event.timestamp;
        long timestamp = Util.getStartTime() + event.timestamp/Const.NANOS_TO_MILLIS;
        savePPGData(event.values[0], timestamp);


    }

    /**
     * saves PPG data to local List object.
     * @param heartRate
     */
    private void savePPGData(float heartRate, long timestamp){
        //Log.d(TAG, "savePPGData:: heart rate: "+heartRate);
        PPGData ppgData = new PPGData(heartRate, Util.getTimeMillis(timestamp));
        listPPGData.add(ppgData);
    }

    /**
     * to be called by onSensorChanged when Gyroscope data is received.
     * It processes received data to see if time interval b/w last and curr data is apt. and
     * sensitivity of data is big enough to be allowed to save.
     * @param event
     */
    private void processGyroData(SensorEvent event){
        //interval between two data entry should be min SENSOR_DATA_MIN_INTERVAL which is millis
        if ((event.timestamp - lastUpdateGyro) < SENSOR_DATA_MIN_INTERVAL_NANOS) { //multiply to convert to millis from nanos
            return;
        }


        float[] values = event.values;

        float axis_x = values[0];
        float axis_y = values[1];
        float axis_z = values[2];

        //float gyroscopeRotationVelocity = (float)Math.sqrt(axis_x * axis_x + axis_y * axis_y + axis_z * axis_z);
        //magnitude of gyroscope reading thresholded

        //if(gyroscopeRotationVelocity > EPSILON){
            lastUpdateGyro = event.timestamp;
            long timestamp = Util.getStartTime() + event.timestamp/Const.NANOS_TO_MILLIS;
            saveGyroData(axis_x, axis_y, axis_z, timestamp);
        //}
    }

    /**
     * to be called by onSensorChanged when Accelerometer data is received.
     * It processes received data to see if time interval b/w last and curr data is apt. and
     * sensitivity of data is big enough to be allowed to save.
     * @param event
     */
    private void processAccelerometerData(SensorEvent event) {
        //interval between two data entry should be min SENSOR_DATA_MIN_INTERVAL
        if ((event.timestamp - lastUpdateAccel) < SENSOR_DATA_MIN_INTERVAL_NANOS) {
            return;
        }

        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];

        //float accelationSquareRoot = (x * x + y * y + z * z)
        //        / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);

        //if (accelationSquareRoot >= EPSILON_ACC){

            lastUpdateAccel = event.timestamp;
            long timestamp = Util.getStartTime() + event.timestamp/Const.NANOS_TO_MILLIS;
            saveAccelerometerData(x, y, z, timestamp);
        //}
    }

    /**
     * saves Accelerometer data to local List object.
     * @param acc_x
     * @param acc_y
     * @param acc_z
     * @param timestamp
     */
    private void saveAccelerometerData(float acc_x, float acc_y, float acc_z, long timestamp){
        //Log.v(TAG, "saveAccelerometerData:: acc_x: "+acc_x+" acc_y: "+acc_y+" acc_z: "+acc_z);
        //SensorData sensorData = new SensorData(acc_x, acc_y, acc_z, Util.getTimeMillis(System.currentTimeMillis()));
        SensorData sensorData = new SensorData(acc_x, acc_y, acc_z, Util.getTimeMillis(timestamp));
        listAccelData.add(sensorData);
    }

    /**
     * saves Gyroscope data to local List object.
     * @param axis_x
     * @param axis_y
     * @param axis_z
     * @param timestamp
     */
    private void saveGyroData(float axis_x, float axis_y, float axis_z, long timestamp){
        //Log.v(TAG, "saveGyroData:: axis_x: "+axis_x+" axis_y: "+axis_y+" axis_z: "+axis_z);
        //SensorData sensorData = new SensorData(axis_x, axis_y, axis_z, Util.getTimeMillis(System.currentTimeMillis()));
        SensorData sensorData = new SensorData(axis_x, axis_y, axis_z, Util.getTimeMillis(timestamp));
        listGyroData.add(sensorData);
    }

    /**
     * called every minute to save data initially stored in local object to File in memory.
     */
    private void saveDataToFile(boolean stop){
        Log.d(TAG, "saveDataToFile");

        List<SensorData> tempGyroList = new ArrayList<>(listGyroData);
        List<SensorData> tempAccelerList = new ArrayList<>(listAccelData);
        List<PPGData> tempPPGList = new ArrayList<>(listPPGData);

        //Should create right away since we already have a copy to allow for new samples to come
        listGyroData.clear(); listAccelData.clear(); listPPGData.clear();

        SaveDataInBackground backgroundSave = new SaveDataInBackground(SensorService.this, stop);
        backgroundSave.execute(tempAccelerList, tempGyroList, tempPPGList);
        Log.d(TAG, "saveDataToFile: Saving files asynchronously");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        sensorManager.unregisterListener(this);
        isServiceRunning = false;
        mWakeLock.release();
        stopPredictionTask();
        Log.d(TAG, "onDestroy: Finished");
        //alarmManager.cancel(pendingIntent);   //alarm manager now in BLE service
    }

    public static class SaveDataInBackground extends AsyncTask<List, Integer, Void> {

        boolean stopServiceAfterFinish;
        private WeakReference<SensorService> serviceReference;

        SaveDataInBackground(SensorService context, boolean stop){
            serviceReference = new WeakReference<>(context);
            stopServiceAfterFinish = stop;
        }

        protected Void doInBackground(List... lists) {
            // get a reference to the activity if it is still there
            SensorService service = serviceReference.get();
            if (service != null){
                Log.d(service.TAG, "doInBackground: About to save IMU files in background");
                FileUtil.saveGyroNAcceleroDataToFile(service, (List<SensorData>)lists[0], (List<SensorData>)lists[1]);
                FileUtil.savePPGDataToFile(service, (List<PPGData>)lists[2]);

                ((List<SensorData>)lists[0]).clear(); ((List<SensorData>)lists[1]).clear(); ((List<SensorData>)lists[2]).clear();
            }


            return null;
        }

        protected void onPostExecute(Void v) {
            SensorService service = serviceReference.get();
            if (service != null) {
                Log.d(service.TAG, "onPostExecute: Saved the files asynchronously");
                //Once, we have finished saving the files, we send the broadcast message to stop the services
                if (stopServiceAfterFinish) {
                    Log.d(service.TAG, "onPostExecute: Stopping after save. Broadcasting message");
                    Intent intent = new Intent(BROADCAST_DATA_SAVE_DATA_AND_STOP);
                    LocalBroadcastManager.getInstance(service).sendBroadcast(intent);
                }
            }
        }
    }
}
