package data.com.datacollector.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import data.com.datacollector.utility.Notifications;
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

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                vibrate(context);
                notificationManager.notify(Notifications.NOTIFICATION_ID_REMINDER, Notifications.getReminderNotification(context));
            }
        }
    }

    public void vibrate(Context context){
        long[] timings = {0, 350, 150, 350, 150, 350, 700, 350, 150, 350, 150, 600};
        int[] amplitudes = {0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255};
        int repeat = -1;
        Vibrator v = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));

        if(v != null){
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, repeat));
        }

    }
}
