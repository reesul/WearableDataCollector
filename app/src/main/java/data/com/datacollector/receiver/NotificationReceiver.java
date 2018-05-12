package data.com.datacollector.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import data.com.datacollector.R;
import data.com.datacollector.view.CurrentLabelActivity;

import static android.content.Context.NOTIFICATION_SERVICE;
import static data.com.datacollector.model.Const.ACTION_REMINDER_NOTIFICATION;
import static data.com.datacollector.model.Const.NOTIFICATION_CHANNEL_ID;
import static data.com.datacollector.model.Const.NOTIFICATION_ID_REMINDER;

public class NotificationReceiver extends BroadcastReceiver {

    private String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "onReceive: " + action);
        if (action != null){
            if(action.equals(ACTION_REMINDER_NOTIFICATION)){
                Log.d(TAG, "onReceive: ACTION_REMINDER_NOTIFICATION sending notification");

                // Build intent for notification content
                Intent viewIntent = new Intent(context, CurrentLabelActivity.class);
                viewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); //Bring activity to top

                PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);

                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Datacollector reminders", NotificationManager.IMPORTANCE_DEFAULT);
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

                // Configure the notification channel.
                notificationChannel.setDescription("Notifications issued by the datacollector app to remind users to finish an activity");
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Color.RED);
                notificationChannel.setVibrationPattern(new long[]{0, 300, 150, 300, 150, 300, 700, 300, 150, 300, 150, 300});
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setContentIntent(viewPendingIntent)
                        .setSmallIcon(R.drawable.custom_circle)
                        .setContentTitle("Reminder")
                        .setContentText("Verify your activity status")
                        .setAutoCancel(true);
                vibrate(context);
                notificationManager.notify(NOTIFICATION_ID_REMINDER, builder.build());
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
