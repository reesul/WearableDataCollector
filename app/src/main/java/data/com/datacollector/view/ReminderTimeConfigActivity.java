package data.com.datacollector.view;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import data.com.datacollector.R;
import data.com.datacollector.utility.Notifications;

import static data.com.datacollector.model.Const.EXTRA_ACTIVITY_LABEL;
import static data.com.datacollector.model.Const.EXTRA_ACTIVITY_LABEL_REMINDING_TIME;

public class ReminderTimeConfigActivity extends WearableActivity {

    private TextView mTextView;
    private String label = "";
    private int minutes = 5;
    private String TAG = "ReminderTimeConfigActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_reminder_time_config);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on TODO: Verify what's this
        setAmbientEnabled();

        Intent intent = getIntent();
        if(intent != null) {
            label = intent.getStringExtra(EXTRA_ACTIVITY_LABEL);
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Notifications.NOTIFICATION_ID_RUNNING_SERVICES, Notifications.getServiceRunningNotification(this,ReminderTimeConfigActivity.class));
    }

    public void onClickMinutesButton(View view){
        switch(view.getId())
        {
            case R.id.btn5min:
                Log.d(TAG, "onClickMinutesButton: 5 minutes");
                minutes = 5;
                break;

            case R.id.btn10min:
                Log.d(TAG, "onClickMinutesButton: 10 minutes");
                minutes = 10;
                break;

            case R.id.btn20min:
                Log.d(TAG, "onClickMinutesButton: 20 minutes");
                minutes = 20;
                break;
        }

        launchCurrentActivityLabel(label, minutes);
    }


    private void launchCurrentActivityLabel(String label, int minutes){
        //Launch time select activity

        Log.d(TAG, "launchCurrentActivityLabel: starting activity with label " + label + " and reminder of " + minutes);
        Intent intent = new Intent(this, CurrentLabelActivity.class);
        intent.putExtra(EXTRA_ACTIVITY_LABEL, label);
        intent.putExtra(EXTRA_ACTIVITY_LABEL_REMINDING_TIME, minutes);
        this.startActivity(intent);
        this.finish();
    }
    
    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy:");
    }
}
