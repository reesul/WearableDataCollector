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
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import data.com.datacollector.model.BTDevice;
import data.com.datacollector.receiver.DataCollectReceiver;
import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.ServiceNotification;

import static data.com.datacollector.model.Const.BLE_SCAN_START_TIME;
import static data.com.datacollector.model.Const.BLE_SCAN_STOP_TIME;
import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_DATA_AND_STOP;
import static data.com.datacollector.model.Const.NUM_BLE_CYCLES;
import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_ALARM_RECEIVED;
import static data.com.datacollector.model.Const.ALARM_SENSOR_DATA_SAVE_INTERVAL;
import android.os.HandlerThread;
import android.os.Process;

/**
 * Service to carry out BLE Scan in the background
 */
public class LeBLEService extends Service {
    private final String TAG = "DC_LeBLEService";

    /**
     * BLE scan specific object starts
     */
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    /**
     * BLE scan object ends
     */

    //used for the alarm, so that data from the scanner is written to files every few seconds
    private AlarmManager alarmManagerData;
    private PendingIntent pendingIntentData;
    private DataCollectReceiver mReceiver;

    public static boolean mScanning;
    public static boolean isServiceRunning = false;

    private List<BTDevice> btDeviceList = new ArrayList<BTDevice>();    //holds devices scanned within current interval
    public List<String> macList = new ArrayList<>();    //map of MAC addresses from the current scan

    /**
     * time when BLE was last updated. This helps to  keep data collection frequency in check
     */
    private long lastUpdateBLE = 0;


    /**
     * Android Handler
     */
    private Handler mHandler;

    //Service worker thread variables based on android guidelines
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: called from ServiceHandler, worker thread with ID: " + msg.arg1);

            //When the service is killed by the OS, the alarm may not be killed, we verify that there is no alarm running already
            //setup the broadcast receiver to receive ACTION_POWER_CONNECTED for transferring files
            //TODO: This along with the alarm logic can be moved to its own service to have a cleaner implementation
            registerFileTransferAction();

            //remove the alarm if it exists
            cancelAlarm();
            setRepeatingAlarm();
            initBlParams();
            scanLeDevice(true);
            isServiceRunning = true;
            mHandler = new Handler();

            if (!mScanning) {
                setUpScanParams();
                scanLeDevice(true);
            }

            Log.d(TAG, "handleMessage: called from ServiceHandler, worker thread finished setup");
        }
    }

    public LeBLEService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
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

    /**
     * initialize BLE scan related objects. Basically setup the ground for BLE scan
     */
    private void initBlParams() {
        Log.d(TAG, "initBlParams");
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        setUpScanParams();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: The BLEService has started");
        Log.d(TAG, "onStartCommand: Intent " + intent);

        /*If the system kills our service, because START_STICKY property, the system restarts our service
         with a null intent unless there are pending intents to start the service which is NOT our case
         since we do not pending intents to start our service. but this is important to consider if there is
         any bug */
        if (intent != null) {

            //When the alarm fires, it broadcast a message which is received by the DataCollectReceiver
            //which then starts the services and this method is called. Here, we validate if we were
            //called to start the service, or to save the data.
            if (intent.getBooleanExtra("save_data", false)) {
                //The service is still running, we only save data
                Log.d(TAG, "onStartCommand: intent with save_data");
                if (!FileUtil.lastUploadResult)
                    Log.d(TAG, "last attempt to upload data failed");
                saveDataToFile(intent.getBooleanExtra(BROADCAST_DATA_SAVE_DATA_AND_STOP, false));
                scanLeDevice(false); //cycle scans off and back on
                scanLeDevice(true);
            } else {
                Log.d(TAG, "onStartCommand: called by our activity");
                startService(startId);
            }
        } else {
            Log.d(TAG, "onStartCommand: intent null because of sticky property");
            startService(startId);
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
    public void startService(int startId) {
        //Start due STICKY property

        //TODO: Make sure the notification is visible and that when touched it opens up our app
        startForeground(ServiceNotification.getNotificationId(), ServiceNotification.getNotification(getApplicationContext()));
        sendMessageToWorkerThread(startId);
        /* Might not be necessary since we are doing this inside the message handling
        if (mReceiver == null) {
            //Check to ensure the Broadcast Receiver is set up properly
            Log.d(TAG, "onStartCommand: Resetting broadcast receiver");
            registerFileTransferAction();
        }*/
    }

    /**
     * This is how we actually start our service in our worker thread
     *
     * @param startId The start id of the service
     */
    public void sendMessageToWorkerThread(int startId) {
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Log.d(TAG, "onStartCommand: Sending start request to working thread");
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);
        Log.d(TAG, "onStartCommand: Sent start request to working thread");
    }


    /**
     * Set up scan params
     */
    private void setUpScanParams() {
        if (Build.VERSION.SDK_INT >= 21) {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setNumOfMatches(1)
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .build();
            filters = new ArrayList<ScanFilter>();
        }
    }

    /**
     * Start/Stop BLE scan
     *
     * @param enable : true to start BLE scan, false to stop BLE scan
     */
    private void scanLeDevice(final boolean enable) {
        Log.d(TAG, "scanLeDevice: start?: " + enable);
        mScanning = enable;

        //following if-block created for moto 360; may not need for Polar M600
        if (mLEScanner == null) {
            initBlParams();
            Log.d(TAG, "scanLeDevice:: for some reason mLeScan is null");
            return;
        }
        // It looks like sometimes, the bluetooth is turned off and the state is not updated.
        // Maybe it has to do to some instability in the timers
        // Before starting a scan or stopping it, make sure the Bluetooth is ON
        // See reference: https://stackoverflow.com/questions/28085104/app-crash-when-bluetooth-is-turned-off-on-android-lollipop
        if (isBTAvailable()) {//TODO: try removing this if scans still unstable
            if (enable) {
                mLEScanner.startScan(filters, settings, mScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    /**
     * Checks if the bluetooth is turned on so we can use it safe
     */
    public boolean isBTAvailable() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        return (btAdapter != null &&
                btAdapter.isEnabled() &&
                btAdapter.getState() == BluetoothAdapter.STATE_ON);
    }

    /**
     * Callback received from BLE frameowrk with detected BLE details
     */
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //interval between two data entry should be min SENSOR_DATA_MIN_INTERVAL
            /*  No plans to use this; may have lots of devices coming in at a time, don't want to miss them
                    since there would be no way to differentiate low frequency from high frequency advertisements
            if ((System.currentTimeMillis() - lastUpdateBLE) < SENSOR_DATA_MIN_INTERVAL) {
                return;
            }       */

            long scanTime = System.currentTimeMillis(); //time of scan, not exact, but close enough
            String curMac = result.getDevice().getAddress();

            //If the device scanned has a MAC hasn't already been scanned in this data save period, add to list
            if (!macList.contains(curMac)) {
                BTDevice btDevice = new BTDevice(result, scanTime);
                btDeviceList.add(btDevice);
                macList.add(curMac);

                //Log.d(TAG, "OnScanResult:: dev scanned: " + btDevice.getMac());
            }
            //otherwise, the device has been scanned prior, and we need to update the period and accessCount
            else {

                /*This code is only needed if some changes need to be made to the device already in the array list
                    //sometimes causes IndexOutOfBounds exception, though index is based on macList, which should match the device list
                int index = macList.indexOf(curMac);   //macs and btDevices are added to list at same time, so index corresponds between the two
                BTDevice devInList = btDeviceList.get(index);
                devInList.setAccessCount((devInList.getAccessCount()+1));

                //check the amount of time between subsequent scans of the same device
                int newPeriod = (int)(scanTime - devInList.getTime());
                if(newPeriod < devInList.getMinPeriod())
                    devInList.setMinPeriod(newPeriod);

                devInList.setTime(scanTime);
                */

                int index = macList.indexOf(curMac);   //macs and btDevices are added to list at same time, so index should correspond between the two
                BTDevice devInList = btDeviceList.get(index);
                devInList.addRepeatDevice(result, scanTime);

                //Log.d(TAG, "OnScanResult:: dev scanned: " + curMac);

            }

            /*      previous implementation; saved less information
            BluetoothDevice device = result.getDevice();
            Log.d(TAG, "onScanResult:: device name: " + device.getName() + " mac: " + device.getAddress() + " rssi: " + result.getRssi());
            btDeviceList.add(makeBtDeviceObj(device, result.getRssi()));
            */
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.d(TAG, "onBatchScanResults:: ScanResult - Results" + sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan Failed" + "Error Code: " + errorCode);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        isServiceRunning = false;
        scanLeDevice(false);
        mScanning = false;

        unregisterReceiver(mReceiver);
    }

    /**
     * call to save stored data in local list to device memory.
     */
    private void saveDataToFile(boolean stop) {
        Log.d(TAG, "saveDataToFile");

        if (btDeviceList.size() == 0) {
            Log.d(TAG, "saveDataToFile:: nothing to save, and service is scanning (t/f): " + mScanning);
            return;
        }

        FileUtil.saveBLEDataToFile(getApplicationContext(), btDeviceList);

        //clear local copy of data, since data has been stored in memory.
        btDeviceList.clear();
        macList.clear();

        //Once, we have finished saving the files, we send the broadcast message to stop the services
        if(stop){
            Log.d(TAG, "saveDataToFile: Broadcast stop message");
            Intent intent = new Intent(BROADCAST_DATA_SAVE_DATA_AND_STOP);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    /**
     * Cancels any alarm that its still going before we can create another one
     */
    private void cancelAlarm(){
        Log.d(TAG, "OnCreate:: Existing repeating alarm - Removing.");
        Intent intent = new Intent(LeBLEService.this, DataCollectReceiver.class);
        intent.putExtra(BROADCAST_DATA_SAVE_ALARM_RECEIVED, true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                LeBLEService.this.getApplicationContext(), 234324243, intent, 0);
        if (pendingIntent != null && alarmManagerData != null) {
            alarmManagerData.cancel(pendingIntent);
        }
    }

    /**
     * Sets an alarm that goes off every few seconds that triggers data saving from memory into files
     * A broadcast message is sent, and it is captured by a BroadcastReceiver, in our case,
     * our DataCollectReceiver. When the receiver receives the message, it issues a startService
     * command with an extra parameter "save_data" The extra parameter is handled in the onStart
     * which either saves data or starts the service
     */
    private void setRepeatingAlarm() {
        Log.d(TAG, "setRepeatingAlarm: ");
        Intent intent = new Intent(this, DataCollectReceiver.class);
        intent.putExtra(BROADCAST_DATA_SAVE_ALARM_RECEIVED, true);
        pendingIntentData = PendingIntent.getBroadcast(
                this.getApplicationContext(), 234324243, intent, 0);

        alarmManagerData = (AlarmManager) getSystemService(ALARM_SERVICE);

        //start timer, start time is earlier than current
        //trigger on interval ALARM_SENSOR_DATA_SAVE INTERVAL
        alarmManagerData.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                + ALARM_SENSOR_DATA_SAVE_INTERVAL, ALARM_SENSOR_DATA_SAVE_INTERVAL, pendingIntentData);
    }

    /***
     * Android > 8.0 requirement
     * Registers the action (ACTION_POWER_CONNECTED) to this activity, so that the Broadcast Receiver
     * can recognize when the watch is plugged in
     */
    private void registerFileTransferAction() {
        Log.d(TAG, "registerFileTransferAction: setup Broadcast Receiver");
        mReceiver = new DataCollectReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
        registerReceiver(mReceiver, filter);

    }


}

