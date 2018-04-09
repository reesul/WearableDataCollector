package data.com.datacollector.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.Log;

import data.com.datacollector.utility.FileUtil;

/**
 * This class provides a set up of basic functions that the integration of any sensor must follow
 * in order to properly save their data. When creating a new service that will run forever collecting
 * their data, they extend this class and add their connection protocol with the external sensor and
 * use the provided methods to save the data into files
 *
 * IMPORTANT NOTE FOR USAGE:
 * 1.- Any service that extends this class must be registered in the Manifest
 * 2.- Any service that extends this class must be added to the REGISTERED_SENSOR_SERVICES constant
 * 3.- Any service that extends this class must have an attribute
 *                  public static boolean isServiceRunning = false;
 *     It must be properly handled to indicate the status of the service. It should be set to true
 *     inside the mainCode method and should be set to false in the onDestroy method
 * 4.- Any service that extends this class must implement the interface ServiceStatusInterface and
 *     return the attribute isServiceRunning in the method implementation
 *
 */
public abstract class BaseExternalSensorService extends Service {

    //Holds the sensor information (Timestamp, value, etc) Anything defined by the model that the user
    //defines
    public Object sensorModel = null;
    public String TAG = "BaseExternalSensorServ";

    //Service worker thread variables based on android guidelines
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    // We do not implement this in the parent class since causes conflicts when multiple classes
    // extend this class since the value is changed globaly for the parend. Therefore, if one of the
    // children changes the state, the others' attribute is also change, which we don't want!
    //public static boolean isServiceRunning = false


    /**
     * Constructor
     */
    public BaseExternalSensorService(){ }

    /**
     * Handler that receives the messages for the worker thread
     */
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: called from ServiceHandler, worker thread");
            mainCode();
            Log.d(TAG, "handleMessage: called from ServiceHandler, worker thread finished setup");
        }
    }

    /**
     * The actuall implementation of the sensor connection. Here goes all the code that the developer
     * needs to connect with the external sensor and handle the oncomming information. This mainCode
     * is the method that is called in the worker thread
     */
    public abstract void mainCode();

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
     * called every x minutes to save data initially stored in local object to File in memory.
     */
    public void saveDataToFile(){
        Log.d(TAG, "saveDataToFile");

        //TODO: Implement the model storage in memory
        //TODO: Implement a generic method in FileUtil to store a generic model
        /*
        List<SensorData> tempGyroList = new ArrayList<>(listGyroData);
        List<SensorData> tempAccelerList = new ArrayList<>(listAccelData);
        List<PPGData> tempPPGList = new ArrayList<>(listPPGData);

        FileUtil.saveGyroNAcceleroDataToFile(this, tempAccelerList, tempGyroList);
        FileUtil.savePPGDataToFile(this, tempPPGList);

        //clear local copy of data, since data has been stored in memory.
        listGyroData.clear(); listAccelData.clear(); listPPGData.clear();
        */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
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

    }

    /**
     * This is executed every time our service is started by an activity, or by the system (Sticky)
     * or from the collector receiver to save data
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
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
                saveDataToFile();
            }else{
                //Start from activity
                Log.d(TAG, "onStartCommand: intent without save_data");
                sendMessageToWorkerThread(startId);
            }
        }else{
            //Start due STICKY property
            Log.d(TAG, "onStartCommand: intent null");
            sendMessageToWorkerThread(startId);
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    /**
     * Implement this if you need to establish a binding connection between this service and some
     * other
     * @param intent
     * @return
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
