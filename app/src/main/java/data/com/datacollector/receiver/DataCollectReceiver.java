package data.com.datacollector.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import data.com.datacollector.network.NetworkIO;
import data.com.datacollector.service.FileTransferService;
import data.com.datacollector.service.LeBLEService;
import data.com.datacollector.service.SensorService;
import data.com.datacollector.utility.FileTransferJob;

import static android.content.Context.ALARM_SERVICE;
import static data.com.datacollector.model.Const.ALARM_SENSOR_DATA_SAVE_INTERVAL;
import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_ALARM_RECEIVED;
import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_DATA_AND_STOP;
import static data.com.datacollector.model.Const.TM_HTTP;
import static data.com.datacollector.model.Const.TM_BT;
import static data.com.datacollector.model.Const.SELECTED_TRANSFER_METHOD;
import static data.com.datacollector.model.Const.TRANSFER_DATA;
import static data.com.datacollector.model.Const.TRANSFER_DATA_RETRIES;

/**
 * BroadcastReceiver of the applciation.
 * Other than lcoal broadcasts, all broadcasts should be received in this class.
 * Created by ritu on 10/29/17.
 */

public class DataCollectReceiver extends BroadcastReceiver {
    private final String TAG = "DC_Receiver";

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
            boolean isPowerConnected = action.equals(Intent.ACTION_POWER_CONNECTED);
            boolean isTransferData = action.equals(TRANSFER_DATA); //The alarm that triggers this

            if( isPowerConnected || isTransferData) {
                Log.d(TAG, "onReceive:: ACTION_POWER_CONNECTED " + isPowerConnected);
                Log.d(TAG, "onReceive:: TRANSFER_DATA " + isTransferData);

                if(SELECTED_TRANSFER_METHOD == TM_HTTP){
                    Log.d(TAG, "onReceive:: sending files through HTTP");
                    //Uploads the data to the HTTP server
                    Thread uploadThread = new Thread(new uploadRunnable(context));
                    uploadThread.start();
                }

                if(SELECTED_TRANSFER_METHOD == TM_BT){
                    Log.d(TAG, "onReceive:: Broadcast receive about to request Bluetooth data transfer");

                    if(isTransferData){
                        //The retries alarm triggered this action
                        int remainingRetries = intent.getIntExtra(TRANSFER_DATA_RETRIES,1); //How many retries are available
                        Log.d(TAG, "onReceive: About to request upload with remaining retries " + String.valueOf(remainingRetries));
                        uploadBTData(context, remainingRetries);
                    } else{
                        //Then its power connected who called this action. We verify that the job is not yet running (even if its waiting for a retry)
                        //TODO: If using the service instead, then change this for FileTransferService.isJobAlreadyRunning
                        if(FileTransferJob.isJobAlreadyRunning){
                            Log.d(TAG, "onReceive: The BT data transfer job is already running");
                            return;
                        }else{
                            Log.d(TAG, "onReceive: About to transfer data over Bluetooth for the first time");
                            uploadBTData(context, FileTransferJob.MAX_NUM_OF_RETRIES);
                        }
                    }
                }
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
        Log.d(TAG, "uploadBTData: Preparing intent for Jobintent");
        FileTransferJob.isJobAlreadyRunning = true;
        Intent intent = new Intent(context, FileTransferJob.class);
        //note, putExtra remembers type and I need this to be an integer.  so get an integer first.
        intent.putExtra(TRANSFER_DATA_RETRIES, retries);  //should do error checking here!
        FileTransferJob.enqueueWork(context,intent);
        Log.d(TAG, "uploadBTData: Job has been enqueued");

        //TODO: Comment out above and uncomment below if using the service
        //Intent intent = new Intent(context, FileTransferService.class);
        //intent.putExtra(TRANSFER_DATA_RETRIES, retries);
        //context.startForegroundService(intent);
    }

    private class uploadRunnable implements Runnable {
        Context currentContext;
        uploadRunnable(Context context) {currentContext = context;}
        public void run() {
            uploadData(currentContext);
        }
    }

}
