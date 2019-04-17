package data.com.datacollector.model;

import android.os.Build;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * class for storing constants to be used across the codebase.
 * Created by ritu on 10/29/17.
 */


public class Const {

    //Determines how will the system attempt to transfer the data
    public static final int TM_HTTP = 1; //HTTP transfer files
    public static final int TM_BT = 2; //Bluetooth transfer files
    public static final int TM_USB = 3; //Bluetooth transfer files
    public static final int SELECTED_TRANSFER_METHOD = TM_USB;

    //Used by Gyroscope Sensor data save procedure to ensure sensor data is above a threshold
    public static final float EPSILON_GYRO = 3.0f;
    /* Used by Accelerometer data save procedure to ensure sensor data is above a threshold (in g = ~9.8 m/s^2)*/
    public static final float EPSILON_ACC = 1.4f;

    //Activities configuration
    public static final ActivitiesList.ActivitiesSource ACTIVITIES_LIST_SOURCE = ActivitiesList.ActivitiesSource.DEFAULT; //Determines Where should the app look for the list activities.
    public static final String DEFAULT_ACTIVITIES_LIST_TEXT[] = {"New step", "Arm argentometer", "Cycle", "Treadmill", "Elliptical", "Walking",
            "Eating", "Sitting", "Standing", "Lying down", "Biking"}; //The default list of activities if not obtained from a server

    //Bluetooth file transfer
    //This is the SPP UUIS which is also set up on the server
    public static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    //We assume the paired device that hosts the BT server has this name
    public static String HOST_MACHINE_BT_NAME = "intel-desktop";

    //Current activity label
    public static final String EXTRA_ACTIVITY_LABEL = "EXTRA_ACTIVITY_LABEL";
    public static final String EXTRA_ACTIVITY_LABEL_REMINDING_TIME = "EXTRA_ACTIVITY_LABEL_REMINDING_TIME";
    public static final String ACTION_REMINDER_NOTIFICATION = "ACTION_REMINDER_NOTIFICATION";
    public static final String ACTION_REMINDER_NOTIFICATION_INTERVAL = "ACTION_REMINDER_NOTIFICATION_INTERVAL";

    //Feedback activity
    public static final String EXTRA_FEEDBACK_QUESTION = "EXTRA_FEEDBACK_QUESTION";
    public static final String EXTRA_FEEDBACK_PREDICTED_LABEL = "EXTRA_FEEDBACK_PREDICTED_LABEL";
    public static final String EXTRA_FEEDBACK_PREDICTION_START_LBL = "EXTRA_FEEDBACK_PREDICTION_START_LBL";
    public static final String EXTRA_FEEDBACK_PREDICTION_END_LBL = "EXTRA_FEEDBACK_PREDICTION_END_LBL";
    public static final String EXTRA_FEEDBACK_VIBRATE = "EXTRA_FEEDBACK_VIBRATE";

    //Pending intent IDs
    public static final int PENDING_INTENT_CODE_NOTIFICATION = 123323098;

    //File names
    public static final String FILE_NAME_ACCELEROMETER = "accelerometer_data.txt";
    public static final String FILE_NAME_GYROSCOPE = "gyroscope_data.txt";
    public static final String FILE_NAME_BLE = "ble_data.txt";
    public static final String FILE_NAME_PPG = "ppg_data.txt";
    public static final String FILE_NAME_ACTIVITY = "activity_data.txt";
    public static final String FILE_NAME_FEEDBACK = "feedback_data.txt";
    public static final String FILE_NAME_GPS = "gps_data.txt";
    public static final String FILE_NAME_PREDICTIONS = "predictions_data.txt";

    //User feedback
    public static final String[] AVAILABLE_LABELS_TO_PREDICT = DEFAULT_ACTIVITIES_LIST_TEXT; //Change this if needed. Remember that we are not only predicting activities. Also postures for example

    /* Take the last 8 digits of the device's serial number as a unique identifier  */
    //TODO: use Build.getSerial() to get serial, as this method is deprecated
        //BUT, that function call requires permission to access phone state, so this would have to be initialized after the permissions check in HomeActivity.onCreate
    public static final String DEVICE_ID = (Build.SERIAL).substring(Build.SERIAL.length() - 8);

    //Broadcasts
    public static final String BROADCAST_DATA_SAVE_ALARM_RECEIVED = "BROADCAST_DATA_SAVE_ALARM_RECEIVED";
    public static final String BROADCAST_DATA_SAVE_DATA_AND_STOP = "BROADCAST_DATA_SAVE_DATA_AND_STOP";
    public static final String SET_LOADING_HOME_ACTIVITY = "SET_LOADING_HOME_ACTIVITY"; //Enables the loading view on the home activity
    public static final String SET_LOADING_USER_FEEDBACK_QUESTION = "SET_LOADING_USER_FEEDBACK_QUESTION"; //Enables the loading view on the feedback question activity
    public static final String SET_LOADING = "SET_LOADING"; //Enables the loading view
    public static final String DISMISS_FEEDBACK_QUESTION_ACTIVITY = "DISMISS_FEEDBACK_QUESTION_ACTIVITY";
    public static final String START_SERVICES = "START_SERVICES";
    public static final String STOP_SERVICES = "STOP_SERVICES";

    /*Server data, need to use "ifconfig" in ubuntu environment to find this
    , or "ipconfig" in windows on the server machine to find local network ip address */
    //TODO set up an online server so this can remain static, or even use url
    public static final String SERVER_ADDRESS  = "192.168.1.100";//"192.168.1.104";
    public static final String SERVER_PORT = "9000";
    //This address may need to change based on where the server is setup (use command "ifconfig" in terminal to find current IP/inet address)
    public static final String BASE_SERVER_URL = "http://" + SERVER_ADDRESS + ":" + SERVER_PORT + "/file/";
    //below is old static URL, instead use combination of other constants
    //public static final String BASE_SERVER_URL = "http://192.168.1.104:9000/file/";//"http://23663c85.ngrok.io/file/";//http://10.222.208.252:9000/file/";//"http://192.168.0.103:9000/file/"; //"http://10.222.208.252/file/";


    /** Interval between data save trigger to file in memory. The Interval is used by AlarmManager to schedule repeat braodcast after each interval defined in the Const. */
    public static final int ALARM_SENSOR_DATA_SAVE_INTERVAL = 5 * 60 * 1000;


    //Sensor data collection constants

    /** Min, Interval between two data collection. Esp. Gyro
     *  and Accelerometer and Heart Rate sensors throw a lot of data within a sec when sensor detects corr. action.
     *  This limit would ensure at the same time limited data is being stored.
     *  Value in milliseconds*/
    //If we use 40 for 20Hz we might get values as low as 17Hz
    public static final long SENSOR_DATA_MIN_INTERVAL = 35;//28Hz this actually gives about 25Hz?
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

    /* Diet annotation constants */

    //Questions

    public static final String FIRST_TIER_ACTIVITIES[] = {"Eating", "Walking", "Exercising", "Sitting", "Nothing", "Other"};

    public static final String EATING_ACTIVITY_FIRST_TIER_QUESTIONS[] = {
            "Did you use your right hand to eat?",
            "Did you finish your plate?",
            "Did you get full?",
            "Did you order any extra?",
            "Did you like it?",
            "Were you in a rush?",
            "Would you have eaten something else if you had opportunity?",
            "Did you like your meal?",
            "What you ate was a snack?"
    };

    public static final String EATING_ACTIVITY_SECOND_TIER_QUESTIONS[] = {
            "What did you eat?",
            "Where did you eat?",
            "What did you drink?",
            "What dessert did you have?",
            "How often do you eat this",
            "What were you doing while eating?"
    };

    public static final Map<String, String[]> EATING_SECOND_TIER_ANSWERS = new HashMap<String, String[]>() {{
        put("a0", new String[]{"Fast food", "Healthy food (veggies, etc.)", "Home made"});
        put("a1", new String[]{"Home", "Restaurant", "Work"});
        put("a2", new String[]{"Water", "Soda", "Juice", "Nothing"});
        put("a3", new String[]{"Icecream", "Cookies", "Cake", "Fruit", "Protein bar", "Nothing"});
        put("a4", new String[]{"Rarely", "Often", "Regularly"});
        put("a5", new String[]{"Just eating", "Working", "Studying", "Watching TV", "Chatting"});
    }};

    public static final Map<String, String[]> REMAINING_ACTIVITIES_ANSWERS = new HashMap<String, String[]>() {{
        put("a1", new String[]{"To work", "To eat", "Just walking"});
        put("a2", new String[]{"Cardio", "Lifting", "Sports"});
        put("a3", new String[]{"Working", "Watching TV", "Studying", "In-class", "Other"});
    }};

    public static final int DIET_ANNOTATION_REMINDER_INTERVAL = 15*60*1000; //In millis, 15 minutes

    public static final String DIET_MODEL_LABELS [] = {"Eating", "Walking", "Exercising", "Sitting", "Other"};

    //Prediction constants
    public static final boolean REQUEST_FEEDBACK = true; //This should be set to false if no feedback will be asked
    public static int PREDICTION_INTERVAL = 5000; //Milliseconds between each prediction
    public static int FEATURES = 3; //The number of features to be computed
    public static int LABELS = 10; //Labels to be predicted
    public static int SAMPLES_PER_SENSOR = 120; //The number of raw data points used in the NN
    public static double MODEL_PROB_THRESHOLD_FOR_FEEDBACK = 0.65; //The minimum probability that the model has to get in a prediction
    public static int MIN_TIME_INTERVAL_BETWEEN_REQUESTS = 30; //In minutes
    public static int MAX_TIME_INTERVAL_WITH_NO_REQUESTS = 120; //In minutes
    public static int FEEDBACK_NOTIFICATION_EXPIRATION_TIME = 2*60*1000; //In millis
    public static int RANDOM_REQUEST_PROBABILITY = 10; //When the prediction is above the threshold, with what probability will we request f
}
