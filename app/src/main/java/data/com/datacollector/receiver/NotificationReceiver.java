package data.com.datacollector.receiver;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import data.com.datacollector.utility.Notifications;

import static android.content.Context.ALARM_SERVICE;
import static data.com.datacollector.model.Const.ACTION_REMINDER_NOTIFICATION;
import static data.com.datacollector.model.Const.ACTION_REMINDER_NOTIFICATION_INTERVAL;
import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_ALARM_RECEIVED;
import static data.com.datacollector.model.Const.PENDING_INTENT_CODE_NOTIFICATION;

public class NotificationReceiver extends BroadcastReceiver {

    private String TAG = "NotificationReceiver";
    private int defaultInterval = 60*1000; //One minute by default

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "onReceive: " + action);
        if (action != null){
            if(action.equals(ACTION_REMINDER_NOTIFICATION)){
                Log.d(TAG, "onReceive: ACTION_REMINDER_NOTIFICATION sending notification");
                int interval = intent.getIntExtra(ACTION_REMINDER_NOTIFICATION_INTERVAL, defaultInterval);
                //We create the next alarm for notification
                setAlarm(context, interval);

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                vibrate(context);
                notificationManager.notify(Notifications.NOTIFICATION_ID_REMINDER, Notifications.getReminderNotification(context));
            }
        }
    }

    public void setAlarm(Context context, int interval){
        Intent intent = new Intent(context.getApplicationContext(), NotificationReceiver.class);
        intent.setAction(ACTION_REMINDER_NOTIFICATION);
        intent.putExtra(ACTION_REMINDER_NOTIFICATION_INTERVAL, interval);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast( context.getApplicationContext(), PENDING_INTENT_CODE_NOTIFICATION, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + interval,
                alarmPendingIntent);
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
