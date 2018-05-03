package data.com.datacollector.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import data.com.datacollector.network.BluetoothFileTransfer;
import data.com.datacollector.network.NetworkIO;
import data.com.datacollector.service.LeBLEService;
import data.com.datacollector.service.SensorService;

import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_ALARM_RECEIVED;
import static data.com.datacollector.model.Const.TM_HTTP;
import static data.com.datacollector.model.Const.TM_BT;
import static data.com.datacollector.model.Const.SELECTED_TRANSFER_METHOD;

/**
 * BroadcastReceiver of the applciation.
 * Other than lcoal broadcasts, all broadcasts should be received in this class.
 * Created by ritu on 10/29/17.
 */

public class DataCollectReceiver extends BroadcastReceiver {
    private final String TAG = "DC_Receiver";
    public static Thread uploadBTThread = null;
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "OnReceive");

        // intent received from alarm to save data
        if(intent.getBooleanExtra(BROADCAST_DATA_SAVE_ALARM_RECEIVED, false)){
            Log.d(TAG, "onReceive:: BROADCAST_DATA_SAVE_ALARM_RECEIVED");

            if(!LeBLEService.isServiceRunning && !SensorService.isServiceRunning){
                Log.d(TAG, "Service not running so not saving the data");
                return;
            }
            if(!LeBLEService.isServiceRunning || !LeBLEService.mScanning)
                Log.d(TAG, "OnReceive: BLE service is not running or is not scanning");
            if(!SensorService.isServiceRunning)
                Log.d(TAG, "OnReceive: Sensor service is not running, but BLE service is");

            //TODO reenable sensor service once BLE works

            //This intents are not restarting the service iself but its a way to communicate to the
            //service something. In our case, we ask the service to save the data. We handle in the
            //onStart method what the service should do depending on the extra values from the intent
            //if not save_data, then we start our processes using the working tree
            Intent serviceIntent = new Intent(context, SensorService.class);
            serviceIntent.putExtra("save_data", true);
            context.startService(serviceIntent);

            Intent leBleServiceIntent = new Intent(context, LeBLEService.class);
            leBleServiceIntent.putExtra("save_data", true);
            context.startService(leBleServiceIntent);

        }

        // device connected to power source, lets upload saved data to server.
        if(action!=null) {
            if(action.equals(Intent.ACTION_POWER_CONNECTED)) {
                Log.d(TAG, "onReceive:: ACTION_POWER_CONNECTED");

                if(SELECTED_TRANSFER_METHOD == TM_HTTP){
                    Log.d(TAG, "onReceive:: sending files through HTTP");
                    //Uploads the data to the HTTP server
                    Thread uploadThread = new Thread(new uploadRunnable(context));
                    uploadThread.start();
                }

                if(SELECTED_TRANSFER_METHOD == TM_BT){
                    Log.d(TAG, "onReceive:: sending files through Bluetooth");
                    if(DataCollectReceiver.uploadBTThread == null) {
                        Log.d(TAG, "onReceive: Creating the thread");
                        DataCollectReceiver.uploadBTThread = new Thread(new uploadBTRunnable(context));
                    }
                    if(!DataCollectReceiver.uploadBTThread.isAlive()){
                        Log.d(TAG, "onReceive: Is not alive, starting the thread");
                        //Uploads the data to the Bluetooth Server
                        DataCollectReceiver.uploadBTThread.start();
                    }else{
                        Log.d(TAG, "onReceive: It was alive, do nothing");
                    }
                }

                //uploadData(context);
            }else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)){
                Log.d(TAG, "onReceive:: ACTION_POWER_DISCONNECTED");
                //TODO: Properly close any possible data sending thread
                //Sometimes, the watch can be connected multiple times in a few seconds creating multiple threads and possible
                //causing conflicts
            }
        }
    }

    /**
     * call Network class to upload data
     * @param context : context of the caller.
     */
    private void uploadData(Context context){
        NetworkIO.uploadData(context.getApplicationContext());
    }

    /**
     * call Bluetooth class to upload data
     * @param context : context of the caller.
     */
    private void uploadBTData(Context context){
        BluetoothFileTransfer btio = new BluetoothFileTransfer();
        btio.sendData(context.getApplicationContext());
    }

    private class uploadRunnable implements Runnable {
        Context currentContext;
        uploadRunnable(Context context) {currentContext = context;}
        public void run() {
            uploadData(currentContext);
        }
    }

    private class uploadBTRunnable implements Runnable {
        Context currentContext;
        uploadBTRunnable(Context context) {currentContext = context;}
        public void run() {
            uploadBTData(currentContext);
        }
    }

}
