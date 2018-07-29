package data.com.datacollector.view;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import data.com.datacollector.R;
import data.com.datacollector.utility.ActivitiesAdapter;
import data.com.datacollector.utility.Notifications;

import static data.com.datacollector.model.Const.EXTRA_ACTIVITY_LABEL;
import static data.com.datacollector.model.Const.EXTRA_ACTIVITY_LABEL_REMINDING_TIME;

public class ReminderTimeConfigActivity extends WearableActivity {

    private TextView mTextView;
    private String label = "";
    private int minutes = 5;
    private String TAG = "ReminderTimeConfigActivity";

    private Button btn1 = null;
    private Button btn2 = null;
    private Button btn3 = null;

    //Whenever the user is seeing this screen this will set to true since a labeling process is in progress
    public static boolean isInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_reminder_time_config);

        mTextView = (TextView) findViewById(R.id.text);

        btn1 = findViewById(R.id.btn5min);
        btn2 = findViewById(R.id.btn10min);
        btn3 = findViewById(R.id.btn20min);

        // Enables Always-on TODO: Verify what's this
        setAmbientEnabled();

        Intent intent = getIntent();
        if(intent != null) {
            label = intent.getStringExtra(EXTRA_ACTIVITY_LABEL);
        }

        //This simply modifies the running notification to open this activity instead the main one
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Notifications.NOTIFICATION_ID_RUNNING_SERVICES, Notifications.getServiceRunningNotification(this,ReminderTimeConfigActivity.class));
    }

    public void onClickMinutesButton(View view){
        enableButtons(false);
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
        isInProgress = false;
        this.finish();
    }
    
    protected void onDestroy(){
        super.onDestroy();
        isInProgress = false;
        Log.d(TAG, "onDestroy:");
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableButtons(true);
        isInProgress = true;
    }

    private void enableButtons(boolean b){
        if(btn1 != null && btn2 != null && btn3 != null) {
            btn1.setEnabled(b);
            btn2.setEnabled(b);
            btn3.setEnabled(b);
        }
    }
}
