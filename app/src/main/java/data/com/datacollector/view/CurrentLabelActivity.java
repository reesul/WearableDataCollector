package data.com.datacollector.view;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;

import data.com.datacollector.R;
import data.com.datacollector.receiver.NotificationReceiver;
import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Notifications;
import data.com.datacollector.utility.Util;

import static data.com.datacollector.model.Const.ACTION_REMINDER_NOTIFICATION;
import static data.com.datacollector.model.Const.ACTION_REMINDER_NOTIFICATION_INTERVAL;
import static data.com.datacollector.model.Const.EXTRA_ACTIVITY_LABEL;
import static data.com.datacollector.model.Const.EXTRA_ACTIVITY_LABEL_REMINDING_TIME;
import static data.com.datacollector.model.Const.PENDING_INTENT_CODE_NOTIFICATION;

public class CurrentLabelActivity extends WearableActivity {

    private TextView txtActivityLabel;
    private String label = "";
    private int minutes = 0;
    private String TAG = "CurrentLabelActivity";
    private PendingIntent alarmPendingIntent;
    private NotificationReceiver notificationReceiver;
    private AlarmManager alarmManager;
    private NotificationManager notificationManager;
    private Button finishActivity = null;
    private int interval = 60*1000; //One minute by default
    //Whenever the user is seeing this screen this will set to true since a labeling process is in progress
    public static boolean isInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_current_label);

        txtActivityLabel = (TextView) findViewById(R.id.txtActivityLabel);
        finishActivity = findViewById(R.id.btnFinish);

        // Enables Always-on TODO: Verify what's this
        setAmbientEnabled();

        Intent intent = getIntent();
        if(intent != null) {
            label = intent.getStringExtra(EXTRA_ACTIVITY_LABEL);
            minutes = intent.getIntExtra(EXTRA_ACTIVITY_LABEL_REMINDING_TIME, 0);
            Log.d(TAG, "onCreate: Label: " + label);
            Log.d(TAG, "onCreate: Minutes: " + minutes);
        }
        interval = minutes*60*1000;
        txtActivityLabel.setText(label);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        notificationReceiver = new NotificationReceiver();

        IntentFilter filter = new IntentFilter(ACTION_REMINDER_NOTIFICATION);
        registerReceiver(notificationReceiver, filter);

        //Set up the alarm for notification
        setRepeatingAlarm();

        //This simply modifies the running notification to open this activity instead the main one
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Notifications.NOTIFICATION_ID_RUNNING_SERVICES, Notifications.getServiceRunningNotification(this,CurrentLabelActivity.class));

    }

    /**
     * Called when the activity is finished by the user. Saves the timestamp
     * @param v
     */
    public void onClickFinishActivity(View v){
        Log.d(TAG, "onClickFinishActivity:");

        finishActivity.setEnabled(false);
        String timestamp = Util.getTimeMillis(System.currentTimeMillis());

        cancelRepeatingAlarm();
        clearNotification(Notifications.NOTIFICATION_ID_REMINDER);
        //Save information to file
        SaveDataInBackground backgroundSave = new SaveDataInBackground(CurrentLabelActivity.this);
        backgroundSave.execute(timestamp, label);
    }

    //Removes the reminder notification and changes back the services notification intent to open the main activity instead of this.
    public void clearNotification(int id) {
        notificationManager.cancel(id);
        notificationManager.notify(Notifications.NOTIFICATION_ID_RUNNING_SERVICES, Notifications.getServiceRunningNotification(this,HomeActivity.class));
    }

    private void setRepeatingAlarm() {
        Log.d(TAG, "setRepeatingAlarm: ");

        Intent intent = new Intent(this.getApplicationContext(), NotificationReceiver.class);
        intent.setAction(ACTION_REMINDER_NOTIFICATION);
        intent.putExtra(ACTION_REMINDER_NOTIFICATION_INTERVAL, interval);
        alarmPendingIntent = PendingIntent.getBroadcast( this.getApplicationContext(), PENDING_INTENT_CODE_NOTIFICATION, intent, 0);


        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + interval,
                alarmPendingIntent);
    }

    private void cancelRepeatingAlarm(){
        Log.d(TAG, "cancelRepeatingAlarm: ");
        //Just for security in case something goes wrong we first verify we have the information before using it
        if(alarmManager == null){
            alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        }
        if(alarmPendingIntent == null){
            Intent intent = new Intent(this.getApplicationContext(), NotificationReceiver.class);
            intent.setAction(ACTION_REMINDER_NOTIFICATION);
            intent.putExtra(ACTION_REMINDER_NOTIFICATION_INTERVAL, interval);
            alarmPendingIntent = PendingIntent.getBroadcast( this.getApplicationContext(), PENDING_INTENT_CODE_NOTIFICATION, intent, 0);
        }
        if(alarmPendingIntent != null) {
            alarmManager.cancel(alarmPendingIntent);
        }
    }

    public static class SaveDataInBackground extends AsyncTask<String, Integer, Boolean> {

        private WeakReference<CurrentLabelActivity> currentActivity;
        String activity;

        SaveDataInBackground(CurrentLabelActivity context){
            currentActivity = new WeakReference<>(context);
        }

        protected Boolean doInBackground(String... lists) {
            CurrentLabelActivity activityRef = currentActivity.get();
            if (activityRef == null || activityRef.isFinishing()) return false;

            try {
                activity = lists[1];
                FileUtil.saveActivityDataToFile(activityRef, lists[0], activity, "end");
                return true;
            }catch (IOException e){
                Log.e(activityRef.TAG,"Error while saving activity: " + e.getMessage());
                return false;
            }
        }

        protected void onPostExecute(Boolean success) {
            CurrentLabelActivity activityRef = currentActivity.get();
            if (activityRef != null && !activityRef.isFinishing()){
                Log.d(activityRef.TAG, "onPostExecute: Saved the files asynchronously");
                if(success) {
                    CurrentLabelActivity.isInProgress = false;
                    activityRef.finish();
                }else{
                    Toast.makeText(activityRef, "Error saving, try again", Toast.LENGTH_SHORT).show();
                    activityRef.finishActivity.setEnabled(true);
                }
            }

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(finishActivity != null) {
            finishActivity.setEnabled(true);
        }
        Log.d(TAG, "onResume: ");
        isInProgress = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        unregisterReceiver(notificationReceiver);
        cancelRepeatingAlarm();
        isInProgress = false;
    }

}
