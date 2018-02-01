package data.com.datacollector.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import data.com.datacollector.network.NetworkIO;
import data.com.datacollector.service.LeBLEService;
import data.com.datacollector.service.SensorService;

import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_ALARM_RECEIVED;

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

        // intent received from alarm to save data
        if(intent.getBooleanExtra(BROADCAST_DATA_SAVE_ALARM_RECEIVED, false)){
            Log.d(TAG, "onReceive:: BROADCAST_DATA_SAVE_ALARM_RECEIVED");

            if(!LeBLEService.isServiceRunning && !SensorService.isServiceRunning){
                Log.d(TAG, "Service not running so not saving the data");
                return;
            }


            //TODO reenable sensor service once BLE works
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
                uploadDataBLE(context);
                uploadData(context);
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

    private void uploadDataBLE(Context context) {
        NetworkIO.uploadDataBLE(context.getApplicationContext());
    }
}
