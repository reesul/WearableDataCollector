package data.com.datacollector.utility;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import data.com.datacollector.R;
import data.com.datacollector.service.LeBLEService;
import data.com.datacollector.service.SensorService;
import data.com.datacollector.view.CurrentLabelActivity;
import data.com.datacollector.view.HomeActivity;
import data.com.datacollector.view.ReminderTimeConfigActivity;
import data.com.datacollector.view.feedback_ui.UserFeedbackGroundTruth;
import data.com.datacollector.view.feedback_ui.UserFeedbackQuestion;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static data.com.datacollector.model.Const.EXTRA_FEEDBACK_PREDICTED_LABEL;
import static data.com.datacollector.model.Const.EXTRA_FEEDBACK_PREDICTION_END_LBL;
import static data.com.datacollector.model.Const.EXTRA_FEEDBACK_PREDICTION_START_LBL;
import static data.com.datacollector.model.Const.EXTRA_FEEDBACK_QUESTION;
import static data.com.datacollector.model.Const.EXTRA_FEEDBACK_VIBRATE;

public class Notifications {
    public static final String TAG = "Notifications";

    //Notification IDs
    public static final int NOTIFICATION_ID_RUNNING_SERVICES = 101;
    public static final int NOTIFICATION_ID_REMINDER = 102;
    public static final int NOTIFICATION_ID_FEEDBACK = 103;
    public static final int NOTIFICATION_ID_FILE_TRANSFER = 104;

    public static final String NOTIFICATION_CHANNEL_ID_REMINDERS = "NOTIFICATION_CHANNEL_ID_REMINDERS";
    public static final String NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING = "NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING";
    public static final String NOTIFICATION_CHANNEL_ID_FEEDBACK = "NOTIFICATION_CHANNEL_ID_FEEDBACK";
    public static final String NOTIFICATION_CHANNEL_ID_FILE_TRANSFER = "NOTIFICATION_CHANNEL_ID_FILE_TRANSFER";

    public static Notification getServiceRunningNotification(Context context, Class<?> activity) {

        // Build intent for notification content
        Intent viewIntent = new Intent(context, activity);
        viewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); //Bring activity to top

        PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);

        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING, "Datacollector service", NotificationManager.IMPORTANCE_MIN);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Configure the notification channel.
        notificationChannel.setDescription("Notifications issued by the datacollector app to remind users to finish an activity");
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        //notificationChannel.setVibrationPattern(new long[]{0, 300, 150, 300, 150, 300, 700, 300, 150, 300, 150, 300});
        //notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING)
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

        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_REMINDERS, "Datacollector reminders", NotificationManager.IMPORTANCE_MIN);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        // Configure the notification channel.
        notificationChannel.setDescription("Notifications issued by the datacollector app to remind users to finish an activity");
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        //notificationChannel.setVibrationPattern(new long[]{0, 300, 150, 300, 150, 300, 700, 300, 150, 300, 150, 300});
        //notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_REMINDERS)
                .setContentIntent(viewPendingIntent)
                .setSmallIcon(R.drawable.ic_cc_checkmark)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setContentTitle("Reminder")
                .setContentText("Verify your activity status")
                .setAutoCancel(true);

        return builder.build();
    }

    //TODO: Verify that the activity is opened even if the notification was not touched (in other words, if the user directly opens the app)
    //TODO: Also, dismiss the notification on that case

    /**
     * This should be used with the method that sends the feedbacknotification
     * @param context
     * @param question
     * @param predictedLabel
     * @return
     */
    public static Notification getFeedbackNotification(Context context, String question, String predictedLabel, String predictionStartTs, String predictionEndTs) {

        // Build intent for notification content
        Intent viewIntent = new Intent(context, UserFeedbackQuestion.class);
        viewIntent.putExtra(EXTRA_FEEDBACK_QUESTION, question);
        viewIntent.putExtra(EXTRA_FEEDBACK_PREDICTED_LABEL, predictedLabel);
        viewIntent.putExtra(EXTRA_FEEDBACK_PREDICTION_START_LBL, predictionStartTs);
        viewIntent.putExtra(EXTRA_FEEDBACK_PREDICTION_END_LBL, predictionEndTs);

        viewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); //Bring activity to top

        PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);

        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_FEEDBACK, "Datacollector feedback", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        // Configure the notification channel.
        notificationChannel.setDescription("Notifications issued by the datacollector app to ask for feedback");
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.setVibrationPattern(new long[]{0, 300, 150, 300, 150, 300, 700, 300, 150, 300, 150, 300});
        notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_FEEDBACK)
                .setContentIntent(viewPendingIntent)
                .setSmallIcon(R.drawable.ic_cc_checkmark)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setVibrate(new long[]{0, 300, 150, 300, 150, 300, 700, 300, 150, 300, 150, 300})
                .setContentTitle("Feedback")
                .setContentText("Please, provide us some feedback")
                .setAutoCancel(true);

        return builder.build();
    }

    /**
     * This will automatically manage the best way (scenario) to ask for the feedback. If the app is dismissed will be through a notification and if the user opens the app
     * directly (without touching the notification) will show the feedback activity. If the app is open we will automatically open the screen.
     *
     * TODO: A future feature might be to dismiss this option if the user does not answer after X minutes and do not save any answer.
     * By default, we have restricted the states of the app on which this action can be accomplished (Asking for feedback)
     * In order to ask for feedback (by any of the previously mentioned means)
     *  1. the sensors have to be running
     *  2. the user cannot be in the middle of a labeling process
     *  3. the user cannot be in the middle of another feedback event
     * @param context
     * @param question
     * @param predictedLabel
     */
    public static void requestFeedback(Context context, String question, String predictedLabel, String predictionStartTs, String predictionEndTs) {
        //TODO: Verify what happens on long questions

        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);

        //TODO: This should be changed if more services are added
        if(SensorService.isServiceRunning && LeBLEService.isServiceRunning &&
                !ReminderTimeConfigActivity.isInProgress && !CurrentLabelActivity.isInProgress &&
                !UserFeedbackQuestion.isInProgress && !UserFeedbackGroundTruth.isInProgress){
            Log.d(TAG, "requestFeedback: All the conditions are met, we can prompt for feedback");
            if(appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE){
                Log.d(TAG, "requestFeedback: The app is in the foreground, simply show the new activity");
                //The app is on the foreground
                Intent intent = new Intent(context, UserFeedbackQuestion.class);
                intent.putExtra(EXTRA_FEEDBACK_QUESTION, question);
                intent.putExtra(EXTRA_FEEDBACK_PREDICTED_LABEL, predictedLabel);
                intent.putExtra(EXTRA_FEEDBACK_PREDICTION_START_LBL, predictionStartTs);
                intent.putExtra(EXTRA_FEEDBACK_PREDICTION_END_LBL, predictionEndTs);
                intent.putExtra(EXTRA_FEEDBACK_VIBRATE, true);
                context.startActivity(intent);
                //vibrate(context);//TODO: Review this

            }else{
                Log.d(TAG, "requestFeedback: The app is not in the foreground, send notification");
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(Notifications.NOTIFICATION_ID_FEEDBACK, Notifications.getFeedbackNotification(context.getApplicationContext(),question, predictedLabel, predictionStartTs, predictionEndTs));
            }
        }else{
            //Do nothing, the services are not running OR the labeling is in progress
            Log.d(TAG, "requestFeedback: We cannot prompt for feedback because the user is doing another thing");
        }

    }

    /**
     * Forces a vibrate event
     * @param context
     */
    public static void vibrate(Context context){
        long[] timings = {0, 350, 150, 350, 150, 350, 700, 350, 150, 350, 150, 600};
        int[] amplitudes = {0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255};
        int repeat = -1;
        Vibrator v = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));

        if(v != null){
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, repeat));
        }

    }

    /**
     * This is used in the case when the user opens the app directly and the notification was previously issued. By this we make sure that the user
     * will provide the feedback before doing anything else. This will open the feedback activity if the notification is detected as delivered
     * TODO: Make sure that this is added at the onResume method of the homeactivity when using the feedback feature
     * @param context
     */
    public static void openFeedbackIfNotificationActive(Context context){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
        if(activeNotifications != null){
            Log.d(TAG, "isFeedbackNotificationActive: Notifications found");
            for (StatusBarNotification notif : activeNotifications) {
                Log.d(TAG, "isFeedbackNotificationActive: ID: " + String.valueOf(notif.getId()));
                if(notif.getId() == Notifications.NOTIFICATION_ID_FEEDBACK){
                    Log.d(TAG, "isFeedbackNotificationActive: Found");
                    try {
                        notif.getNotification().contentIntent.send();
                        notificationManager.cancel(Notifications.NOTIFICATION_ID_FEEDBACK);
                        Log.d(TAG, "openFeedbackIfNotificationActive: Send intent");
                    } catch (PendingIntent.CanceledException e) {
                        Log.d(TAG, "isFeedbackNotificationActive: There was an error sending the pending intent from the notification");
                        e.printStackTrace();
                    }
                }
            }
        }else{
            Log.d(TAG, "isFeedbackNotificationActive: No notifications active");
        }
    }

    public static Notification getServiceFileTransferNotification(Context context) {

        // Build intent for notification content
        Intent viewIntent = new Intent(context, HomeActivity.class);
        viewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); //Bring activity to top

        PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);

        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_FILE_TRANSFER, "Datacollector service", NotificationManager.IMPORTANCE_MIN);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Configure the notification channel.
        notificationChannel.setDescription("Notifications issued by the datacollector app to remind users to finish an activity");
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        //notificationChannel.setVibrationPattern(new long[]{0, 300, 150, 300, 150, 300, 700, 300, 150, 300, 150, 300});
        //notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_FILE_TRANSFER)
                .setContentIntent(viewPendingIntent)
                .setSmallIcon(R.drawable.ic_cc_checkmark)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setOnlyAlertOnce(true)
                .setContentTitle("Datacollector app")
                .setContentText("Files are being transferred");
        return builder.build();
    }


}