package data.com.datacollector.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import data.com.datacollector.interfaces.ServiceStatusInterface;
import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Notifications;
import data.com.datacollector.view.HomeActivity;

import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_DATA_AND_STOP;

/**
 * This class provides a set up of basic functions that the integration of any sensor must follow
 * in order to properly save their data. When creating a new service that will run forever collecting
 * their data, they extend this class and add their connection protocol with the external sensor and
 * use the provided methods to save the data into files
 *
 * The user has to define a model for the type of data that is being saved
 *
 * IMPORTANT NOTE FOR USAGE:
 * 1.- Any service that extends this class must be registered in the Manifest
 * 2.- Any service that extends this class must be added to the REGISTERED_SENSOR_SERVICES constant
 * 3.- Any service that extends this class must have an attribute
 *                  public static boolean isServiceRunning = false;
 *     It must be properly handled to indicate the status of the service. It should be set to true
 *     inside the mainCode method and should be set to false in the onDestroy method.
 *     Also, this attribute should be returned by the method isServiceRunning from
 *     ServiceStatusInterface
 * 4.- Any service that extends this class must implement the interface ServiceStatusInterface and
 *     return the attribute isServiceRunning in the method implementation
 *
 */
public abstract class BaseExternalSensorService extends Service implements ServiceStatusInterface {

    public String TAG = "BaseExternalSensorServ";

    //Holds the sensor information (Timestamp, value, etc) Anything defined by the model that the user
    //defines
    public List<Object> sensorData = new ArrayList<>();

    //Service worker thread variables based on android guidelines
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    // We do not implement this in the parent class since causes conflicts when multiple classes
    // extend this class since the value is changed globaly for the parend. Therefore, if one of the
    // children changes the state, the others' attribute is also change, which we don't want!
    //public static boolean isServiceRunning = false

    //NOTE: Uncomment all the comments that have "wakelock" on the right
    //      if you app presents gaps on data. Android turns off the device for energy saving and it might cause some issues. If
    //      you think that some code in here might not be being executed sometime during the whole app run, this might solve it.
    //We force the app to stay up so we can get sensor data
    //private PowerManager.WakeLock mWakeLock = null; //Wakelock

    /**
     * Constructor
     */
    public BaseExternalSensorService(){ }

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
     * You should implement the necessary code in order to successfully disconnect from the sensor whenever the
     * service is stopped. Adding the conde in the onDestroy might be one way to go, however, if the process
     * is very long it might not be finished before android finishes this activity.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        //mWakeLock.release();//Wakelock
        //isServiceRunning = false;
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

            /* //Wakelock
            if(mWakeLock == null){
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SensorWakeLock");
            }
            mWakeLock.acquire();*/

            mainCode();
            //isServiceRunning = true;
            Log.d(TAG, "handleMessage: called from ServiceHandler, worker thread finished setup");
        }
    }

    /**
     * The actual implementation of the sensor connection. Here goes all the code that the developer
     * needs to connect with the external sensor and handle the incoming information. This mainCode
     * is the method that is called in the worker thread
     */
    public abstract void mainCode();

    /**
     * Starts our service and creates the notification. This method can be called when the user
     * starts the service on when it is started by the system
     *
     * @param startId
     */
    public void startService(int startId) {
        startForeground(Notifications.NOTIFICATION_ID_RUNNING_SERVICES, Notifications.getServiceRunningNotification(getApplicationContext(), HomeActivity.class));
        sendMessageToWorkerThread(startId);
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

    //TODO: Finish and test what would happen if overwritten
    //TODO: Does overwritting one overwrites others?
    public static synchronized void saveInFile(List<Object> sensorData){
        //Saving format should contain the variable names that you want to save separated by comma.
        //TODO: Implement
    }


    /**
     * called every minute to save data initially stored in local object to File in memory.
     */
    private void saveDataToFile(boolean stop){
        Log.d(TAG, "saveDataToFile");

        List<Object> tempSensorData = new ArrayList<>(sensorData);

        //Should create right away since we already have a copy to allow for new samples to come
        sensorData.clear();

        SaveDataInBackground backgroundSave = new SaveDataInBackground(BaseExternalSensorService.this, stop);
        backgroundSave.execute(tempSensorData);
        Log.d(TAG, "saveDataToFile: Saving files asynchronously");
    }

    public static class SaveDataInBackground extends AsyncTask<List, Integer, Void> {

        boolean stopServiceAfterFinish;
        private WeakReference<BaseExternalSensorService> serviceReference;

        SaveDataInBackground(BaseExternalSensorService context, boolean stop){
            serviceReference = new WeakReference<>(context);
            stopServiceAfterFinish = stop;
        }

        protected Void doInBackground(List... lists) {
            // get a reference to the activity if it is still there
            BaseExternalSensorService service = serviceReference.get();
            if (service != null){
                Log.d(service.TAG, "doInBackground: About to save files in background");
                saveInFile((List<Object>)lists[0]);
                ((List<Object>)lists[0]).clear();
            }


            return null;
        }

        protected void onPostExecute(Void v) {
            BaseExternalSensorService service = serviceReference.get();
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