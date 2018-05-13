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
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import data.com.datacollector.R;
import data.com.datacollector.receiver.NotificationReceiver;
import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Notifications;
import data.com.datacollector.utility.Util;

import static data.com.datacollector.model.Const.ACTION_REMINDER_NOTIFICATION;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_current_label);

        txtActivityLabel = (TextView) findViewById(R.id.txtActivityLabel);

        // Enables Always-on TODO: Verify what's this
        setAmbientEnabled();

        Intent intent = getIntent();
        if(intent != null) {
            label = intent.getStringExtra(EXTRA_ACTIVITY_LABEL);
            minutes = intent.getIntExtra(EXTRA_ACTIVITY_LABEL_REMINDING_TIME, 0);
            Log.d(TAG, "onCreate: Label: " + label);
            Log.d(TAG, "onCreate: Minutes: " + minutes);
        }

        txtActivityLabel.setText(label);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        notificationReceiver = new NotificationReceiver();

        IntentFilter filter = new IntentFilter(ACTION_REMINDER_NOTIFICATION);
        registerReceiver(notificationReceiver, filter);
        setRepeatingAlarm(minutes);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Notifications.NOTIFICATION_ID_RUNNING_SERVICES, Notifications.getServiceRunningNotification(this,CurrentLabelActivity.class));

    }

    /**
     * Called when the activity is finished by the user. Saves the timestamp
     * @param v
     */
    public void onClickFinishActivity(View v){

        String timestamp = Util.getTime(System.currentTimeMillis());

        cancelRepeatingAlarm();
        clearNotification(Notifications.NOTIFICATION_ID_REMINDER);
        //Save information to file
        SaveDataInBackground backgroundSave = new SaveDataInBackground(this);
        backgroundSave.execute(timestamp, label);
    }

    public void clearNotification(int id) {
        notificationManager.cancel(id);
        notificationManager.notify(Notifications.NOTIFICATION_ID_RUNNING_SERVICES, Notifications.getServiceRunningNotification(this,HomeActivity.class));
    }

    private void setRepeatingAlarm(int minutes) {
        Log.d(TAG, "setRepeatingAlarm: ");

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.setAction(ACTION_REMINDER_NOTIFICATION);
        alarmPendingIntent = PendingIntent.getBroadcast( this.getApplicationContext(), PENDING_INTENT_CODE_NOTIFICATION, intent, 0);

        //start timer, start time is earlier than current
        //trigger on interval ALARM_SENSOR_DATA_SAVE INTERVAL
        int interval = minutes*60*1000;
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, interval, alarmPendingIntent);
    }

    private void cancelRepeatingAlarm(){
        Log.d(TAG, "cancelRepeatingAlarm: ");
        alarmManager.cancel(alarmPendingIntent);
    }

    private class SaveDataInBackground extends AsyncTask<String, Integer, Void> {

        Context context;
        String activity;

        public SaveDataInBackground(Context context){
            this.context = context;
        }

        protected Void doInBackground(String... lists) {
            try {
                activity = lists[1];
                FileUtil.saveActivityDataToFile(context, lists[0], activity);
            }catch (IOException e){
                Log.e(TAG,"Error while saving activity: " + e.getMessage());
                Toast.makeText(context, "Error, try again later", Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        protected void onPostExecute(Void v) {
            Log.d(TAG, "onPostExecute: Saved the files asynchronously");
            CurrentLabelActivity.this.finish();
            Toast.makeText(context, activity + " saved", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        unregisterReceiver(notificationReceiver);
        cancelRepeatingAlarm();
    }

}
