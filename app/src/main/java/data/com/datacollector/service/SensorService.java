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
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import data.com.datacollector.model.Const;
import data.com.datacollector.model.PPGData;
import data.com.datacollector.model.SensorData;
import data.com.datacollector.model.classifiers.NN;
import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Notifications;
import data.com.datacollector.utility.Util;
import data.com.datacollector.view.HomeActivity;

import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_DATA_AND_STOP;
import static data.com.datacollector.model.Const.DIET_MODEL_LABELS;
import static data.com.datacollector.model.Const.PREDICTION_INTERVAL;
import static data.com.datacollector.model.Const.SAMPLES_PER_SENSOR;
import static data.com.datacollector.model.Const.SENSOR_DATA_MIN_INTERVAL_NANOS;
import static data.com.datacollector.model.Const.SENSOR_QUEUE_LATENCY;

/**
 * Service to enable SensorManager's sensor data collection
 * In the current implementation data related to Accelerometer, Gyroscope and Heart
 * rate Sensors are being collected.
 * This implementation can be further extended to collect ddata related to other sensors
 * supported by SensorManager.
 */
public class SensorService extends Service implements SensorEventListener{
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

    //Handler that periodically runs code for predictions
    private Handler predictionHandler = new Handler();

    //We force the app to stay up so we can get sensor data
    private PowerManager.WakeLock mWakeLock = null;

    //Prediction variables
    private NN nnClassifier = null;
    private double features[];
    private double prob[];
    private double sortedArray[];
    private double highestProb = 0;
    private int predictedLblId = 0;
    public static double previousFeedbackRequestTimestamp = 0;

    /*
     * This runs the given classifier every X seconds to issue predictions
     */
    private final Runnable predictionRunnable = new Runnable(){//Thread that will run the prediction
        public void run(){
            Log.d(TAG, "run: predictionRunnable called");
            if(nnClassifier == null){
                Log.d(TAG, "run: There was an error and the logistic regression model is not set, cancelling further predictions");
                Toast.makeText(SensorService.this,"There was an unexpected error with the model prediction. Please, contact the researcher",Toast.LENGTH_LONG).show();
            } else {
                //Make the prediction and determine if we need feedback
                try {
                    //Get snapshot of data (Previous window)
                    int samplesToUse = SAMPLES_PER_SENSOR;
                    if(listAccelData.size() > samplesToUse) {
                        //If we have enough data, then we start the predictions

                        List<SensorData> accWindowData = listAccelData.subList(listAccelData.size()-samplesToUse, listAccelData.size());
                        List<SensorData> gyroWindowData = listGyroData.subList(listGyroData.size()-samplesToUse, listGyroData.size());
                        makePrediction(accWindowData, gyroWindowData);

                    }else{
                        Log.d(TAG, "run: Not enough data so prediction is not made");
                    }
                } catch (Exception e) {
                    Log.d(TAG, "run: There was an exception so the prediction could not be made: "+e.getMessage());
                    e.printStackTrace();
                }

                //This issues another prediction task within the interval
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

    /**
     * Obtains the raw data from the sensors and computes the model's output
     * @param accWindowData The data that will be fed into the NN
     * @param gyroWindowData The data that will be fed into the NN
     * @throws Exception
     */
    private void makePrediction(List<SensorData> accWindowData, List<SensorData> gyroWindowData) throws Exception{

        //TODO: Make sure the data is in the expected format
        //features = logisticRegression.getFeatures(windowData);//Obtain the features that will be used for the training of our models
        String timestamp = accWindowData.get(accWindowData.size()-1).getTimestamp();
        prob = nnClassifier.predict(accWindowData, gyroWindowData); //We get the probabilities of this features belonging to each label


        sortedArray = new double[prob.length];

        //We have sorted the labels according to their probabilities
        Util.ArrayIndexComparator comparator = new Util.ArrayIndexComparator(prob);
        Integer[] orderedIndexes = comparator.createIndexArray();
        Arrays.sort(orderedIndexes, comparator);
        int[] orderedIndexesPrim= new int[orderedIndexes.length];
        for (int i=0;i<orderedIndexes.length;i++){
            orderedIndexesPrim[i] = orderedIndexes[i];
        }
        //Creating the ordered array
        for (int i=0; i<prob.length; i++){
            sortedArray[i] = prob[orderedIndexes[i]];
        }

        highestProb = sortedArray[0];
        predictedLblId = orderedIndexes[0];

        Log.d(TAG, "makePrediction: PROBABILITY: " + highestProb);
        Log.d(TAG, "makePrediction: PREDICTED:   " + DIET_MODEL_LABELS[predictedLblId]);

        //If the label is "None" the question structure should change
        String question = predictedLblId == 0 ? "No posture?":"Are you " + DIET_MODEL_LABELS[predictedLblId] + "?";

        //Adding prediction to save afterwards
        String prediction = timestamp + ",";
        for (int i = 0; i < prob.length; i++) {
            prediction += String.valueOf(prob[i]);
            if (i + 1 < prob.length) {
                prediction += ",";
            }
        }

        //TODO: Finish this prediction add
        //predictions.add(prediction);
        //SavePredictionDataInBackground backgroundSave = new SavePredictionDataInBackground(SensorService.this, timestamp, prob);
        //backgroundSave.execute();
        //requestFeedback(highestProb < MODEL_PROB_THRESHOLD_FOR_FEEDBACK, question, timestamp, orderedIndexesPrim);

    }

    public SensorService() {
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
        if(nnClassifier == null){
            initNNClassifier();
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

    public void startPredictionTask() {
        predictionRunnable.run();
    }

    public void stopPredictionTask() {
        predictionHandler.removeCallbacks(predictionRunnable);
    }

    public void initNNClassifier(){
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/models";
        nnClassifier = new NN(path + "/nn.tflite");
        try {
            nnClassifier.setLabels(DIET_MODEL_LABELS);
            nnClassifier.setModel();
        } catch (Exception e) {
            Log.d(TAG, "onCreate: There was an error setting up the NN model: " + e.getMessage());
            nnClassifier = null;
            e.printStackTrace();
        }
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
        throw new UnsupportedOperationException("Not yet implemented");
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
