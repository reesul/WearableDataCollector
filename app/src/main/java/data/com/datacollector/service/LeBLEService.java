package data.com.datacollector.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import data.com.datacollector.model.BTDevice;
import data.com.datacollector.network.NetworkIO;
import data.com.datacollector.receiver.DataCollectReceiver;
import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Util;
import data.com.datacollector.model.Const;

import static data.com.datacollector.model.Const.BLE_SCAN_START_TIME;
import static data.com.datacollector.model.Const.BLE_SCAN_STOP_TIME;
import static data.com.datacollector.model.Const.NUM_BLE_CYCLES;
import static data.com.datacollector.model.Const.SENSOR_DATA_MIN_INTERVAL;
import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_ALARM_RECEIVED;
import static data.com.datacollector.model.Const.ALARM_SENSOR_DATA_SAVE_INTERVAL;

/**
 * Service to carry out BLE Scan in the background
 */
public class LeBLEService extends Service {
    private final String TAG = "DC_LeBLEService";

    /** BLE scan specific object starts */
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    /** BLE scan object ends */

    //used for the alarm, so that data from the scanner is written to files every few seconds
    private AlarmManager alarmManagerData;
    private PendingIntent pendingIntentData;

    public static boolean mScanning;
    public static boolean isServiceRunning = false;

    private List<BTDevice> btDeviceList = new ArrayList<BTDevice>();    //holds devices scanned within current interval
    public List<String> macList = new ArrayList<>();    //map of MAC addresses from the current scan

    /** time when BLE was last updated. This helps to  keep data collection frequency in check*/
    private long lastUpdateBLE = 0;

    private int bleCycles = 0;

    /** Android Handler*/
    private Handler mHandler;

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

        //set alarm to trigger data saving
        setRepeatingAlarm();

        initBlParams();
        scanLeDevice(true);
        isServiceRunning = true;

        mHandler = new Handler();
    }

    /**
     * initialize BLE scan realted objects. Basically setup the ground for BLE scan
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
        Log.d(TAG, "onStartCommand");

        if(intent != null) {
            if (intent.getBooleanExtra("save_data", false)) {
                if (!NetworkIO.lastUploadResult)
                    Log.d(TAG, "last attempt to upload data failed");
                saveDataToFile();
            }
        }
        if(!mScanning) {
            setUpScanParams();
            scanLeDevice(true);

        }

        //reset scan counter
        bleCycles = 0;
        //shut BLE scan after specified time; this runnable will also start the back to achieve 25% duty cycle
//        setRepeatingAlarm();
        mHandler.postDelayed(mStopScanRunnable, BLE_SCAN_STOP_TIME);

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Runnable to shut BLE scan after specified time.
     */
    private Runnable mStopScanRunnable = new Runnable() {
        @Override
        public void run() {

            Log.d(TAG, "mStopServiceRunnable");
            scanLeDevice(false);

            //only execute runnable to start the service if we have not reached max cycles for the data saving interval used by the repeating alarm
            bleCycles++;
            if(bleCycles <= NUM_BLE_CYCLES) {
                //once this runnable has executed, set a similar runnable to restart the scans
                mHandler.postDelayed(mStartScanRunnable, BLE_SCAN_START_TIME);
            }
        }
    };

    /**
     * Runnable to start BLE scan after specified time.
     */
    private Runnable mStartScanRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mStartServiceRunnable");

            //if the service running, restart the scan in a moment
            if(isServiceRunning) {
                scanLeDevice(true);

                //check to see if we should run this cycle again

                mHandler.postDelayed(mStopScanRunnable, BLE_SCAN_STOP_TIME);
            }
        }
    };

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
     * @param enable : true to start BLE scan, false to stop BLE scan
     */
    private void scanLeDevice(final boolean enable) {
        Log.d(TAG, "scanLeDevice: " + enable);
        mScanning = enable;

        //following if-block created for moto 360; may not need for Polar M600
        if (mLEScanner == null) {
            initBlParams();
            Log.d(TAG, "scanLeDevice:: for some reason mLeScan is null");
            return;
        }
        if (enable) {
            mLEScanner.startScan(filters, settings, mScanCallback);
        } else {
            mLEScanner.stopScan(mScanCallback);
        }
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
            if(!macList.contains(curMac)) {
                BTDevice btDevice = new BTDevice(result, scanTime);
                btDeviceList.add(btDevice);
                macList.add(curMac);

                Log.d(TAG, "OnScanResult:: dev scanned: " + btDevice.getMac());
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

                Log.d(TAG, "OnScanResult:: dev scanned: " + curMac);

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


/*  @deprecated
    /**
     * make a BLE device object with given parameters
     * @param result: BLE device
     * @param rssi: RSSI of the BLE
     * @return: BLE device object
     *//*
    private BTDevice makeBtDeviceObj(BluetoothDevice result, int rssi) {
        BluetoothDevice bluetoothDevice = result;
        BTDevice btDevice = new BTDevice();
        btDevice.setName(bluetoothDevice.getName());
        btDevice.setMac(bluetoothDevice.getAddress());
        btDevice.setTimeStamp(Util.getTime(System.currentTimeMillis()));
        btDevice.setRssi(rssi);
        return btDevice;
    }
*/
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        isServiceRunning = false;
        scanLeDevice(false);
        mScanning = false;
    }

    /**
     * call to save stored data in local list to device memory.
     */
    private void saveDataToFile() {
        Log.d(TAG, "saveDataToFile");

        if(btDeviceList.size() == 0){
            Log.d(TAG, "saveDataToFile:: nothing to save ");
            return;
        }

        FileUtil.saveBLEDataToFile(getApplicationContext(), btDeviceList);

        //clear local copy of data, since data has been stored in memory.
        btDeviceList.clear();
        macList.clear();
    }


    //sets an alarm that goes off every few seconds
    private void setRepeatingAlarm(){
        Log.d(TAG, "setRepeatingAlarm");
        Intent intent = new Intent(this, DataCollectReceiver.class);
        intent.putExtra(BROADCAST_DATA_SAVE_ALARM_RECEIVED, true);

        pendingIntentData = PendingIntent.getBroadcast(
                this.getApplicationContext(), 234324243, intent, 0);

//        if(alarmManagerData !=null) {
//            alarmManagerData.cancel(pendingIntentData);
//        }
        alarmManagerData = (AlarmManager) getSystemService(ALARM_SERVICE);



        //start timer, start time is earlier than current
        //trigger on interval ALARM_SENSOR_DATA_SAVE INTERVAL
        alarmManagerData.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                + ALARM_SENSOR_DATA_SAVE_INTERVAL, ALARM_SENSOR_DATA_SAVE_INTERVAL, pendingIntentData);
    }
}

