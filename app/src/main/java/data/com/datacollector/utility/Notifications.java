package data.com.datacollector.utility;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;

import data.com.datacollector.R;
import data.com.datacollector.view.CurrentLabelActivity;
import data.com.datacollector.view.HomeActivity;

import static android.content.Context.NOTIFICATION_SERVICE;

public class Notifications {

    //Notification IDs
    public static final int NOTIFICATION_ID_RUNNING_SERVICES = 101;
    public static final int NOTIFICATION_ID_REMINDER = 102;

    public static final String NOTIFICATION_CHANNEL_ID_REMINDERS = "REMINDERS_CHANNEL";
    public static final String NOTIFICATION_CHANNEL_ID_SERVICE_RUNNIGN = "REMINDERS_CHANNEL";

    public static Notification getServiceRunningNotification(Context context, Class<?> activity) {

        // Build intent for notification content
        Intent viewIntent = new Intent(context, activity);
        viewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); //Bring activity to top

        PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);

        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_SERVICE_RUNNIGN, "Datacollector service", NotificationManager.IMPORTANCE_MIN);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Configure the notification channel.
        notificationChannel.setDescription("Notifications issued by the datacollector app to remind users to finish an activity");
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        //notificationChannel.setVibrationPattern(new long[]{0, 300, 150, 300, 150, 300, 700, 300, 150, 300, 150, 300});
        //notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_SERVICE_RUNNIGN)
                .setContentIntent(viewPendingIntent)
                .setSmallIcon(R.drawable.ic_cc_checkmark)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setOnlyAlertOnce(true)
                .setContentTitle("Datacollector service")
                .setContentText("Datacollector services are running");
        return builder.build();
    }

    public static Notification getReminderNotification(Context context) {
        // Build intent for notification content
        Intent viewIntent = new Intent(context, CurrentLabelActivity.class);
        viewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); //Bring activity to top

        PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);

        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_REMINDERS, "Datacollector reminders", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        // Configure the notification channel.
        notificationChannel.setDescription("Notifications issued by the datacollector app to remind users to finish an activity");
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.setVibrationPattern(new long[]{0, 300, 150, 300, 150, 300, 700, 300, 150, 300, 150, 300});
        notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_REMINDERS)
                .setContentIntent(viewPendingIntent)
                .setSmallIcon(R.drawable.ic_cc_checkmark)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setContentTitle("Reminder")
                .setContentText("Verify your activity status")
                .setAutoCancel(true);

        return builder.build();
    }

}