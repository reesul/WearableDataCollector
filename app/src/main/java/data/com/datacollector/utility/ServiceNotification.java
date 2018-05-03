package data.com.datacollector.utility;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import data.com.datacollector.R;

import static android.app.NotificationChannel.DEFAULT_CHANNEL_ID;
import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;

public class ServiceNotification {

    private static final int NOTIFICATION_ID = 101;

    private static Notification notification;

    public static Notification getNotification(Context context) {

        if(notification == null) {

            notification = new NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
                    .setContentTitle("Sensor Service")
                    .setContentText("Running service")
                    .setOngoing(true)
                    .build();


        }

        return notification;
    }

    public static int getNotificationId() {
        return NOTIFICATION_ID;
    }

}