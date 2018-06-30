package data.com.datacollector.utility;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import data.com.datacollector.network.BluetoothFileTransfer;
import data.com.datacollector.receiver.DataCollectReceiver;

import static data.com.datacollector.model.Const.PENDING_INTENT_CODE_FILE_TRANSFER_JOB;
import static data.com.datacollector.model.Const.TRANSFER_DATA;
import static data.com.datacollector.model.Const.TRANSFER_DATA_RETRIES;

/**
 * JobIntentService for our BT file transfer process. We use this instead of asynctask or Threads
 * since they may cause leaks.
 */
public class FileTransferJob extends JobIntentService {
    /**
     * Unique job ID for this service.
     */

    //TODO: IMPortant, what happens with this flag if the BT transfer gets interrupted? Is it restarted? (set to false)
    public static boolean isJobAlreadyRunning = false; //This flag will remain true as soon as this JOB is called and as long as retries are still available
    public static final int JOB_ID = 1000;
    public static int MAX_NUM_OF_RETRIES = 6;//How many times are we going to retry to send the data
    private int MINUTES_TO_WAIT = 3; //The minutes we wait between each attempt
    public String TAG = "FileTransferJob";

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, FileTransferJob.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(Intent intent) {

        int retriesRemaining = intent.getIntExtra(TRANSFER_DATA_RETRIES,1); //Get the number of retries we have. Default to 1 (this one)
        Log.d(TAG, "onHandleWork: About to attempt transfer with remaining retries " + String.valueOf(retriesRemaining));


        try {
            BluetoothFileTransfer btio = new BluetoothFileTransfer();
            Log.d(TAG, "onHandleWork: About to send data over Bluetooth");
            btio.sendData(FileTransferJob.this.getApplicationContext());
            FileTransferJob.isJobAlreadyRunning = false; //Success, then this is no longer running
            Log.d(TAG, "onHandleWork: The data has been sent over Bluetooth");
        }catch (Exception e){
            Log.d(TAG, "onHandleWork: There was a problem with the BT transfer: " + e.getMessage());

            retriesRemaining--; //We reduce the number of retries we have

            //If no more retries available, simply do nothing
            if (retriesRemaining > 0) {
                Log.d(TAG, "onHandleWork: Setting up alarm. Retries ramaining: " + String.valueOf(retriesRemaining));
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                Intent alarmIntent = new Intent(this.getApplicationContext(), DataCollectReceiver.class);
                alarmIntent.setAction(TRANSFER_DATA);
                alarmIntent.putExtra(TRANSFER_DATA_RETRIES, retriesRemaining);

                PendingIntent alarmPendingIntent = PendingIntent.getBroadcast( this.getApplicationContext(), PENDING_INTENT_CODE_FILE_TRANSFER_JOB, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                int totalTime = MINUTES_TO_WAIT*60*1000;
                if(alarmManager != null){
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + totalTime,
                            alarmPendingIntent);
                    Log.d(TAG, "onHandleWork: Alarm is set, waiting " + String.valueOf(totalTime) + " minutes for next attempt...");
                }else{
                    Log.d(TAG, "onHandleWork: Alarm could not be set. Alarm manager is NULL");
                }

            }else{
                Log.d(TAG, "onHandleWork: There are no more retries");
                FileTransferJob.isJobAlreadyRunning = false;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: The file transfer JOB has finished");
    }

}