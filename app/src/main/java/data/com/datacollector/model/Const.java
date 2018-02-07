package data.com.datacollector.model;

import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.provider.Settings.Secure.ANDROID_ID;
import static java.security.AccessController.getContext;

/**
 * class for storing constants to be used across the codebase.
 * Created by ritu on 10/29/17.
 */


public class Const {
    //Used by Gyroscope Sensor data save procedure to ensure sensor data is above a threshold
    public static final float EPSILON_GYRO = 3.0f;
    /* Used by Accelerometer data save procedure to ensure sensor data is above a threshold (in g = ~9.8 m/s^2)*/
    public static final float EPSILON_ACC = 1.4f;

    //Activities configuration
    public static final ActivitiesList.ActivitiesSource ACTIVITIES_LIST_SOURCE = ActivitiesList.ActivitiesSource.DEFAULT; //Determines Where should the app look for the list activities.
    public static final String DEFAULT_ACTIVITIES_LIST_TEXT[] = {"Activity 1", "Activity 2",
            "Activity 3", "Activity 4", "Activity 5","Activity 6", "Activity 7","Activity 8",
            "Activity 9","Activity 10"}; //The default list of activities if not obtained from a server
    public static final String DEFAULT_ACTIVITIES_LIST_TAG[] = {"ACT1", "ACT2", "ACT3", "ACT4", "ACT5", "ACT6",
            "ACT7", "ACT8", "ACT9", "ACT10",};


    //File names
    public static final String FILE_NAME_ACCELEROMETER = "accelerometer_data.txt";
    public static final String FILE_NAME_GYROSCOPE = "gyroscope_data.txt";
    public static final String FILE_NAME_BLE = "ble_data.txt";
    public static final String FILE_NAME_PPG = "ppg_data.txt";
    public static final String FILE_NAME_ACTIVITY_TAG = "activityTag_data.txt";

    /* Take the last 8 digits of the device's serial number as a unique identifier  */
    public static final String DEVICE_ID = (Build.SERIAL).substring(Build.SERIAL.length() - 8);

    //Broadcasts
    public static final String BROADCAST_DATA_SAVE_ALARM_RECEIVED = "BROADCAST_DATA_SAVE_ALARM_RECEIVED";

    //Server data
    public static final String SERVER_ADDRESS  = "192.168.1.104";
    public static final String SERVER_PORT = "9000";
    //This address may need to change based on where the server is setup (use command "ifconfig" in terminal to find current IP/inet address)
    public static final String BASE_SERVER_URL = "http://" + SERVER_ADDRESS + ":" + SERVER_PORT + "/file/";
    //below is old static URL, instead use combination of other constants
    //public static final String BASE_SERVER_URL = "http://192.168.1.104:9000/file/";//"http://23663c85.ngrok.io/file/";//http://10.222.208.252:9000/file/";//"http://192.168.0.103:9000/file/"; //"http://10.222.208.252/file/";


    /** Interval between data save trigger to file in memory. The Interval is used by AlarmManager to schedule repeat braodcast after each interval defined in the Const. */
    public static final int ALARM_SENSOR_DATA_SAVE_INTERVAL = 5 * 60 * 1000;


    //Sensor data collection constants
    /** Min, Interval between two data collection. Esp. Gyro
     *  and Acceleremoter and Heart Rate sensors throw a lot of data withing a sec when sensor detects corr. action.
     *  This limit would ensure at the same time limited data is being stored.
     *  Value in milliseconds*/
    public static final long SENSOR_DATA_MIN_INTERVAL = 100; //10Hz
    /* Conversion from nanoseconds to milliseconds (SensorEvents have timestamp in ns that is used for downsampling) */
    public static final long NANOS_TO_MILLIS = 1000000;
    /* Min interval between sensor (Acc, Gyro, and heart rate) samples that are saved in ns*/
    public static final long SENSOR_DATA_MIN_INTERVAL_NANOS = SENSOR_DATA_MIN_INTERVAL*NANOS_TO_MILLIS;
    /* These are the maximum number of Data entries that will be entertained while saving data after each minute.*/
    public static final long MAX_PER_MIN_SENSOR_DATA_ALLOWED = ALARM_SENSOR_DATA_SAVE_INTERVAL/SENSOR_DATA_MIN_INTERVAL * 2;
    /* Sensor event latency in microseconds: events are kept in a FIFO queue to save battery  */
    public static final int SENSOR_QUEUE_LATENCY = 5000000;

    //BLE Scan constants
    /* Time after which BLE Scan would stop itself after being started*/
    public static final long BLE_SCAN_STOP_TIME = 12 * 1000;
    /* Time after which BLE Scan will start itself after being stopped; created for duty cycle fo 25% */
    public static final long BLE_SCAN_START_TIME = BLE_SCAN_STOP_TIME * 3;
    /* number of times the LE service will cycle the BLE scan on and off within the save data interval - truncated down to nearest int */
    public static final int NUM_BLE_CYCLES = (int)(ALARM_SENSOR_DATA_SAVE_INTERVAL / (BLE_SCAN_START_TIME + BLE_SCAN_STOP_TIME));




}
