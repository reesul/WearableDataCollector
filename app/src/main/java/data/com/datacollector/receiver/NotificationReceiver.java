package data.com.datacollector.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static data.com.datacollector.model.Const.ACTION_REMINDER_NOTIFICATION;

public class NotificationReceiver extends BroadcastReceiver {

    private String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "onReceive: " + action);
        if (action != null){
            if(action.equals(ACTION_REMINDER_NOTIFICATION)){
                Log.d(TAG, "onReceive: ACTION_REMINDER_NOTIFICATION sending notification");
            }
        }
    }
}
