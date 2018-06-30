package data.com.datacollector.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import data.com.datacollector.model.BTDevice;
import data.com.datacollector.network.BluetoothFileTransfer;
import data.com.datacollector.receiver.DataCollectReceiver;
import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Notifications;
import data.com.datacollector.view.HomeActivity;

import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_DATA_AND_STOP;
import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_ALARM_RECEIVED;
import static data.com.datacollector.model.Const.ALARM_SENSOR_DATA_SAVE_INTERVAL;
import static data.com.datacollector.model.Const.PENDING_INTENT_CODE_FILE_TRANSFER_JOB;
import static data.com.datacollector.model.Const.TRANSFER_DATA;
import static data.com.datacollector.model.Const.TRANSFER_DATA_RETRIES;

import android.os.HandlerThread;
import android.os.Process;

/**
 * Service to carry out BLE Scan in the background
 */
public class FileTransferService extends Service {

    /**
     * Android Handler
     */
    private Handler mHandler;

    //Service worker thread variables based on android guidelines
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    public String TAG = "FileTransferService";
    public static boolean isJobAlreadyRunning = false; //This flag will remain true as soon as this JOB is called and as long as retries are still available
    public static final int JOB_ID = 1000;
    public static int MAX_NUM_OF_RETRIES = 6;//How many times are we going to retry to send the data
    private int MINUTES_TO_WAIT = 3; //The minutes we wait between each attempt


    //We force the app to stay up so we can get sensor data
    //private PowerManager.WakeLock mWakeLock = null; //Uncomment if the BT data has missing data. The system might be putting this to sleep

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: called from ServiceHandler, worker thread with ID: " + msg.arg1);

            int retriesRemaining = msg.arg2; //Get the number of retries we have. Default to 1 (this one)
            Log.d(TAG, "handleMessage: About to attempt transfer with remaining retries " + String.valueOf(retriesRemaining));


            try {
                BluetoothFileTransfer btio = new BluetoothFileTransfer();
                Log.d(TAG, "handleMessage: About to send data over Bluetooth");
                btio.sendData(FileTransferService.this.getApplicationContext());
                FileTransferService.isJobAlreadyRunning = false; //Success, then this is no longer running
                Log.d(TAG, "handleMessage: The data has been sent over Bluetooth");
            }catch (Exception e){
                Log.d(TAG, "handleMessage: There was a problem with the BT transfer: " + e.getMessage());

                retriesRemaining--; //We reduce the number of retries we have

                //If no more retries available, simply do nothing
                if (retriesRemaining > 0) {
                    Log.d(TAG, "handleMessage: Setting up alarm. Retries ramaining: " + String.valueOf(retriesRemaining));
                    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    Intent alarmIntent = new Intent(FileTransferService.this.getApplicationContext(), DataCollectReceiver.class);
                    alarmIntent.setAction(TRANSFER_DATA);
                    alarmIntent.putExtra(TRANSFER_DATA_RETRIES, retriesRemaining);

                    PendingIntent alarmPendingIntent = PendingIntent.getBroadcast( FileTransferService.this.getApplicationContext(), PENDING_INTENT_CODE_FILE_TRANSFER_JOB, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    int totalTime = MINUTES_TO_WAIT*60*1000;
                    if(alarmManager != null){
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                                System.currentTimeMillis() + totalTime,
                                alarmPendingIntent);
                        Log.d(TAG, "handleMessage: Alarm is set, waiting " + String.valueOf(totalTime) + " minutes for next attempt...");
                    }else{
                        Log.d(TAG, "handleMessage: Alarm could not be set. Alarm manager is NULL");
                    }

                }else{
                    Log.d(TAG, "handleMessage: There are no more retries");
                    FileTransferService.isJobAlreadyRunning = false;
                }
            }finally {
                Log.d(TAG, "handleMessage: Stopping self");
                FileTransferService.this.stopSelf();
            }

            Log.d(TAG, "handleMessage: called from ServiceHandler, worker thread finished setup");
        }
    }

    public FileTransferService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        // Start up the thread running the service and in background priority to not disrupt the UI
        Log.d(TAG, "onCreate: Creating thread");
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Log.d(TAG, "onCreate: Finished creating thread");

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: The File transfer service has started");
        Log.d(TAG, "onStartCommand: Intent " + intent);

        /*If the system kills our service, because START_STICKY property, the system restarts our service
         with a null intent unless there are pending intents to start the service which is NOT our case
         since we do not pending intents to start our service. but this is important to consider if there is
         any bug */
        if (intent != null) {
            Log.d(TAG, "onStartCommand: called by our activity");
            int retriesRemaining = intent.getIntExtra(TRANSFER_DATA_RETRIES,1);
            startService(startId, retriesRemaining);
        } else {
            Log.d(TAG, "onStartCommand: intent null because of sticky property");
            //startService(startId);
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    /**
     * Starts our service and creates the notification. This method can be called when the user
     * starts the service on when it is started by the system
     *
     * @param startId
     */
    public void startService(int startId, int retriesRemaining) {
        startForeground(Notifications.NOTIFICATION_ID_FILE_TRANSFER, Notifications.getServiceFileTransferNotification(getApplicationContext()));
        sendMessageToWorkerThread(startId, retriesRemaining);
    }

    /**
     * This is how we actually start our service in our worker thread
     *
     * @param startId The start id of the service
     */
    public void sendMessageToWorkerThread(int startId, int retriesRemaining) {
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Log.d(TAG, "onStartCommand: Sending start request to working thread");
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.arg2 = retriesRemaining;
        mServiceHandler.sendMessage(msg);
        Log.d(TAG, "onStartCommand: Sent start request to working thread");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }


}

