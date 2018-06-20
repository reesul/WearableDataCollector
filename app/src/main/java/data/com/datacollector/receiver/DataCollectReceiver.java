package data.com.datacollector.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import data.com.datacollector.network.BluetoothFileTransfer;
import data.com.datacollector.network.NetworkIO;
import data.com.datacollector.service.LeBLEService;
import data.com.datacollector.service.SensorService;

import static android.content.Context.ALARM_SERVICE;
import static data.com.datacollector.model.Const.ALARM_SENSOR_DATA_SAVE_INTERVAL;
import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_ALARM_RECEIVED;
import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_DATA_AND_STOP;
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
    private static boolean isAsyncTaskRunning = false;//TODO: This static variable might need to be changed to another location or using shared preferences
    private int retries = 6;//How many times are we going to retry to send the data
    private int MINUTES_TO_WAIT = 10;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "OnReceive");

        // intent received from alarm to save data
        if(intent.getBooleanExtra(BROADCAST_DATA_SAVE_ALARM_RECEIVED, false)){

            //Since there is no repeating allow while idle we have to manually create the alarm again
            //once the previous one goes off. Therefore, we first need to validate that this received broadcast
            //was not a BROADCAST_DATA_SAVE_DATA_AND_STOP broadcast message
            if(!intent.getBooleanExtra(BROADCAST_DATA_SAVE_DATA_AND_STOP, false)) {
                //If we were triggered by a stop command there is no need to create another alarm
                Intent alarmIntent = new Intent(context.getApplicationContext(), DataCollectReceiver.class);
                alarmIntent.putExtra(BROADCAST_DATA_SAVE_ALARM_RECEIVED, true);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context.getApplicationContext(), 234324243, alarmIntent, 0);
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                        + ALARM_SENSOR_DATA_SAVE_INTERVAL, pendingIntent);
            }

            Log.d(TAG, "onReceive:: BROADCAST_DATA_SAVE_ALARM_RECEIVED");

            if(!LeBLEService.isServiceRunning || !LeBLEService.mScanning)
                Log.d(TAG, "OnReceive: BLE service is not running or is not scanning");
            if(!SensorService.isServiceRunning)
                Log.d(TAG, "OnReceive: Sensor service is not running, but BLE service is");

            if(!LeBLEService.isServiceRunning && !SensorService.isServiceRunning){
                Log.d(TAG, "Service not running so not saving the data");
                return;
            }


            //TODO reenable sensor service once BLE works

            //This intents are not restarting the service itself but its a way to communicate to the
            //service something. In our case, we ask the service to save the data. We handle in the
            //onStart method what the service should do depending on the extra values from the intent
            //if not save_data, then we start our processes using the working tree
            Intent serviceIntent = new Intent(context, SensorService.class);
            serviceIntent.putExtra("save_data", true);
            //If we were asked to stop after saving, pass this parameter to the service intent
            serviceIntent.putExtra(BROADCAST_DATA_SAVE_DATA_AND_STOP, intent.getBooleanExtra(BROADCAST_DATA_SAVE_DATA_AND_STOP, false));
            context.startService(serviceIntent);

            Intent leBleServiceIntent = new Intent(context, LeBLEService.class);
            leBleServiceIntent.putExtra("save_data", true);
            leBleServiceIntent.putExtra(BROADCAST_DATA_SAVE_DATA_AND_STOP, intent.getBooleanExtra(BROADCAST_DATA_SAVE_DATA_AND_STOP, false));
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
                    if(DataCollectReceiver.isAsyncTaskRunning){
                        Log.d(TAG, "onReceive: BT Asynctask transfer is already running");
                        return;
                    }else{
                        Log.d(TAG, "onReceive: Attempt to send data through BT");
                        uploadBTData(context, retries);
                    }

                    /*if(DataCollectReceiver.uploadBTThread == null) {
                        Log.d(TAG, "onReceive: Creating the thread");
                        DataCollectReceiver.uploadBTThread = new Thread(new uploadBTRunnable(context));
                    }
                    if(!DataCollectReceiver.uploadBTThread.isAlive()){
                        Log.d(TAG, "onReceive: Is not alive, starting the thread");
                        //Uploads the data to the Bluetooth Server
                        DataCollectReceiver.uploadBTThread.start();
                    }else{
                        Log.d(TAG, "onReceive: It was alive, do nothing");
                    }*/
                }

                //uploadData(context);
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
    private void uploadBTData(Context context, int retries){
        //BluetoothFileTransfer btio = new BluetoothFileTransfer();
        //btio.sendData(context.getApplicationContext());
        Log.d(TAG, "uploadBTData: Preparing asynctask");
        DataCollectReceiver.isAsyncTaskRunning = true;
        TransferBTData backgroundTransfer = new TransferBTData(context, retries);
        backgroundTransfer.execute();
        
    }

    private class uploadRunnable implements Runnable {
        Context currentContext;
        uploadRunnable(Context context) {currentContext = context;}
        public void run() {
            uploadData(currentContext);
        }
    }

    /*private class uploadBTRunnable implements Runnable {
        Context currentContext;
        uploadBTRunnable(Context context) {currentContext = context;}
        public void run() {
            uploadBTData(currentContext);
        }
    }*/

    /**
     * Transfers the collected data asynchronously
     */
    private class TransferBTData extends AsyncTask<Void, Integer, Boolean> {

        private Context context;
        private int retriesRemaining;

        public TransferBTData(Context context, int retriesRemaining){
            this.context = context;
            this.retriesRemaining = retriesRemaining;
        }

        protected Boolean doInBackground(Void... lists) {
            boolean success = false;
            try {
                BluetoothFileTransfer btio = new BluetoothFileTransfer();
                Log.d(TAG, "doInBackground: About to send data");
                btio.sendData(context.getApplicationContext());
                Log.d(TAG, "doInBackground: Data is sent");
                success = true;
            }catch (Exception e){
                Log.d(TAG, "doInBackground: Error: " + e.getMessage());
                success = false;
            }
            return success;
        }

        protected void onPostExecute(Boolean success) {
            Log.d(TAG, "onPostExecute:");
            if(!success){
                Log.d(TAG, "onPostExecute: There was a problem with the BT transfer");
                if((retriesRemaining-1) > 0){
                    Log.d(TAG, "onPostExecute: Retrying");
                    try {
                        Thread.sleep(MINUTES_TO_WAIT*1000); //Wait a 10 minutes before retrying
                        uploadBTData(context, retriesRemaining-1);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "onPostExecute: Error while waiting to retry" );
                        e.printStackTrace();
                    }
                }else{
                    Log.d(TAG, "onPostExecute: No more retries remain");
                    DataCollectReceiver.isAsyncTaskRunning = false;
                }
            }else{
                Log.d(TAG, "onPostExecute: BT Data transfer successful");
                DataCollectReceiver.isAsyncTaskRunning = false;
            }
        }
    }

}
