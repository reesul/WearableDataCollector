package data.com.datacollector.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import data.com.datacollector.model.PPGData;
import data.com.datacollector.model.SensorData;
import data.com.datacollector.receiver.DataCollectReceiver;
import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Util;

import static data.com.datacollector.model.Const.ALARM_SENSOR_DATA_SAVE_INTERVAL;
import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_ALARM_RECEIVED;
import static data.com.datacollector.model.Const.EPSILON_GYRO;
import static data.com.datacollector.model.Const.EPSILON_ACC;
import static data.com.datacollector.model.Const.SENSOR_DATA_MIN_INTERVAL_NANOS;
import static data.com.datacollector.model.Const.SENSOR_DATA_MIN_INTERVAL;
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

    public SensorService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        initSensor();
        //setRepeatingAlarm();  //this is already included in the BLE service

        isServiceRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getBooleanExtra("save_data", false))
            saveDataToFile();

        return super.onStartCommand(intent, flags, startId);
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

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL, SENSOR_QUEUE_LATENCY );


        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_NORMAL, SENSOR_QUEUE_LATENCY);


        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE),
                SensorManager.SENSOR_DELAY_NORMAL, SENSOR_QUEUE_LATENCY);


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

        savePPGData(event.values[0]);


    }

    /**
     * saves PPG data to local List object.
     * @param heartRate
     */
    private void savePPGData(float heartRate){
        Log.d(TAG, "savePPGData:: heart rate: "+heartRate);
        PPGData ppgData = new PPGData(heartRate, Util.getTime(System.currentTimeMillis()));
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
            saveGyroData(axis_x, axis_y, axis_z);
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
            saveAccelerometerData(x, y, z);
        //}
    }

    /**
     * saves Accelerometer data to local List object.
     * @param acc_x
     * @param acc_y
     * @param acc_z
     */
    private void saveAccelerometerData(float acc_x, float acc_y, float acc_z){
        //Log.v(TAG, "saveAccelerometerData:: acc_x: "+acc_x+" acc_y: "+acc_y+" acc_z: "+acc_z);
        SensorData sensorData = new SensorData(acc_x, acc_y, acc_z, Util.getTime(System.currentTimeMillis()));
        listAccelData.add(sensorData);
    }

    /**
     * saves Gyroscope data to local List object.
     * @param axis_x
     * @param axis_y
     * @param axis_z
     */
    private void saveGyroData(float axis_x, float axis_y, float axis_z){
        //Log.v(TAG, "saveGyroData:: axis_x: "+axis_x+" axis_y: "+axis_y+" axis_z: "+axis_z);
        SensorData sensorData = new SensorData(axis_x, axis_y, axis_z, Util.getTime(System.currentTimeMillis()));
        listGyroData.add(sensorData);
    }

    /*
     * sets repeating alarm to save data after every minute
     */       //alarm moved to BLE service

    private void setRepeatingAlarm(){
        Intent intent = new Intent(this, DataCollectReceiver.class);
        intent.putExtra(BROADCAST_DATA_SAVE_ALARM_RECEIVED, true);
        pendingIntent = PendingIntent.getBroadcast(
                this.getApplicationContext(), 234324243, intent, 0);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                + ALARM_SENSOR_DATA_SAVE_INTERVAL, ALARM_SENSOR_DATA_SAVE_INTERVAL, pendingIntent);
    }

    /**
     * called every minute to save data initially stored in local object to File in memory.
     */
    private void saveDataToFile(){
        Log.d(TAG, "saveDataToFile");

        List<SensorData> tempGyroList = new ArrayList<>(listGyroData);
        List<SensorData> tempAccelerList = new ArrayList<>(listAccelData);
        List<PPGData> tempPPGList = new ArrayList<>(listPPGData);

        FileUtil.saveGyroNAcceleroDataToFile(this, tempAccelerList, tempGyroList);
        FileUtil.savePPGDataToFile(this, tempPPGList);

        //clear local copy of data, since data has been stored in memory.
        listGyroData.clear(); listAccelData.clear(); listPPGData.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        sensorManager.unregisterListener(this);
        isServiceRunning = false;
        //alarmManager.cancel(pendingIntent);   //alarm manager now in BLE service
    }
}
