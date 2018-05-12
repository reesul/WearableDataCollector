package data.com.datacollector.utility;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import static android.app.NotificationChannel.DEFAULT_CHANNEL_ID;
import static data.com.datacollector.model.Const.NOTIFICATION_ID_RUNNING_SERVICES;

public class ServiceNotification {

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
        return NOTIFICATION_ID_RUNNING_SERVICES;
    }

}