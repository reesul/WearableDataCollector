package data.com.datacollector.view.feedback_ui;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;

import data.com.datacollector.R;
import data.com.datacollector.service.SensorService;
import data.com.datacollector.utility.ActivitiesAdapter;
import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Notifications;
import data.com.datacollector.view.HomeActivity;

import static data.com.datacollector.model.Const.AVAILABLE_LABELS_TO_PREDICT;
import static data.com.datacollector.model.Const.DISMISS_FEEDBACK_QUESTION_ACTIVITY;
import static data.com.datacollector.model.Const.EXTRA_FEEDBACK_FEATURES;
import static data.com.datacollector.model.Const.EXTRA_FEEDBACK_LBLS_ORDER;
import static data.com.datacollector.model.Const.EXTRA_FEEDBACK_PREDICTED_LABEL;
import static data.com.datacollector.model.Const.EXTRA_FEEDBACK_TIMESTAMP;
import static data.com.datacollector.model.Const.FEEDBACK_NOTIFICATION_EXPIRATION_TIME;
import static data.com.datacollector.model.Const.SET_LOADING;
import static data.com.datacollector.model.Const.SET_LOADING_USER_FEEDBACK_QUESTION;

public class UserFeedbackGroundTruth extends WearableActivity {

    private String TAG = "UserFeedbackGroundTruth";
    private TextView mTextView;
    private String predictedLabel = "";
    private String timestamp = "";
    private double features[];
    private int orderedIndexes[];
    private ActivitiesAdapter adapterList;
    private WearableRecyclerView recLabelsList;
    private FrameLayout progressBar;
    private int previousEvent = MotionEvent.ACTION_UP;
    private NotificationManager notificationManager;
    public static boolean isInProgress = false;
    private Handler timelimitHandler = new Handler();

    /**
     * This class shows the UI feedback to request from the user and its managed by the Notifications utility on the requestFeedback method.
     * This will store the ground truth label from the user. Currently shows all the list of activities.
     *  TODO: A potential improvement is to suggest some activities that might be the ones to be chosen as GT. For example show only 3 instead of 10
     *  TODO: these labels might be the next 3 closers in probability to the one that its wrong
     *  TODO: Store the required information in the format expected by the actual model (feedback, features, etc)
     *  TODO: Make notes and documentation about this. If the model changes, this format of saving should also change (features, saving format, etc)
     */
    private BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mStopServicesReceiver onReceive: called");
            String action = intent.getAction();
            //Action that tells us to stop our services
            if(action.equals(SET_LOADING_USER_FEEDBACK_QUESTION)) {
                Log.d(TAG, "onReceive: Received SET_LOADING_USER_FEEDBACK_QUESTION confirmation");
                if(intent.getBooleanExtra(DISMISS_FEEDBACK_QUESTION_ACTIVITY,false)){ //TODO: Verify this default works well
                    Log.d(TAG, "onReceive: DISMISS_FEEDBACK_QUESTION_ACTIVITY, dismissing this activity");
                    SensorService.previousFeedbackRequestTimestamp = System.currentTimeMillis();
                    isInProgress = false;
                    timelimitHandler.removeCallbacks(timeLimitRunnable);
                    clearNotification(Notifications.NOTIFICATION_ID_FEEDBACK);
                    UserFeedbackGroundTruth.this.finish();
                }else {
                    setLoading(intent.getBooleanExtra(SET_LOADING,false));
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        if(intent != null) {
            predictedLabel = intent.getStringExtra(EXTRA_FEEDBACK_PREDICTED_LABEL);
            features = intent.getDoubleArrayExtra(EXTRA_FEEDBACK_FEATURES);
            timestamp = intent.getStringExtra(EXTRA_FEEDBACK_TIMESTAMP);
            orderedIndexes = intent.getIntArrayExtra(EXTRA_FEEDBACK_LBLS_ORDER);
        }

        //Registering a local broadcast receiver to listen for data save confirmation
        IntentFilter confirmationIntent = new IntentFilter();
        confirmationIntent.addAction(SET_LOADING_USER_FEEDBACK_QUESTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalReceiver, confirmationIntent);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_feedback_ground_truth);

        mTextView = (TextView) findViewById(R.id.text);

        setRecyclerView();

        //TODO: Add adapter using the second constructor
        //TODO: Finish the loading method
        //TODO: Add the same behavior on service notification as for currentlabel and set time activities

        //This simply modifies the running notification to open this activity instead the main one
        notificationManager = (NotificationManager) UserFeedbackGroundTruth.this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Notifications.NOTIFICATION_ID_RUNNING_SERVICES, Notifications.getServiceRunningNotification(UserFeedbackGroundTruth.this,UserFeedbackGroundTruth.class));

        // Enables Always-on
        setAmbientEnabled();
        timelimitHandler.postDelayed(timeLimitRunnable, FEEDBACK_NOTIFICATION_EXPIRATION_TIME);
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "onResume: ");
        isInProgress = true;
        setLoading(false);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        isInProgress = false;
    }

    public void setRecyclerView(){
        //initialize the list that holds all of the labels
        recLabelsList =  findViewById((R.id.recUserFeedbackList));
        //recActivitiesList.setCircularScrollingGestureEnabled(true); Consider if this might be easier for the user
        recLabelsList.setLayoutManager(new WearableLinearLayoutManager((this)));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recLabelsList.getContext(), DividerItemDecoration.VERTICAL);
        recLabelsList.addItemDecoration(dividerItemDecoration);

        //Set up recycler view adapter with the obtained list
        adapterList = new ActivitiesAdapter(AVAILABLE_LABELS_TO_PREDICT, predictedLabel, features, timestamp, orderedIndexes);
        recLabelsList.setAdapter(adapterList);

        //We intercept multiple touches and prevent any other after the first one arrives
        recLabelsList.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                // return
                // true: consume touch event
                // false: dispatch touch event

                View childView = rv.findChildViewUnder(e.getX(), e.getY());

                //It could be the case on which the user touches in a portion of the UI where there is NO children. That's why our ui was being froze
                //therefore we first verify we are touching on a child
                if(childView != null){

                    //We first determine if we touched the label and not only moved
                    if(previousEvent == MotionEvent.ACTION_DOWN && e.getAction() == MotionEvent.ACTION_UP) {
                        setLoading(true);// This prevents user from submitting multiple labels when touching quickly
                    }
                    //else ignore
                    previousEvent = e.getAction();
                }

                return false;
            }
        });

        progressBar = findViewById(R.id.feedbackGroundTruthProgress);
    }

    public void setLoading(boolean b){
        if(b){
            Log.d(TAG, "setLoading: true");
            recLabelsList.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }else{
            Log.d(TAG, "setLoading: false");
            recLabelsList.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    //Removes the reminder notification and changes back the services notification intent to open the main activity instead of this.
    public void clearNotification(int id) {
        notificationManager.cancel(id);
        notificationManager.notify(Notifications.NOTIFICATION_ID_RUNNING_SERVICES, Notifications.getServiceRunningNotification(this, HomeActivity.class));
    }

    private void cancelFeedbackEvent(){
        SaveFeedbackDataInBackground saveData = new SaveFeedbackDataInBackground(UserFeedbackGroundTruth.this, features); //This cancels the notif and closes this activity
        saveData.execute(timestamp, predictedLabel, ""); //NO feedback was provided, that's why is left blank
    }

    private final Runnable timeLimitRunnable = new Runnable(){//Thread that will run the prediction
        public void run(){
            Log.d(TAG, "run: Expiration time is over");
            cancelFeedbackEvent();
        }
    };

    public static class SaveFeedbackDataInBackground extends AsyncTask<String, Integer, Boolean> {

        private WeakReference<UserFeedbackGroundTruth> currentActivity;
        private double features[];
        SaveFeedbackDataInBackground(UserFeedbackGroundTruth context, double features[]){
            currentActivity = new WeakReference<>(context);
            this.features = features;
        }

        protected Boolean doInBackground(String... lists) {
            UserFeedbackGroundTruth activityRef = currentActivity.get();
            if (activityRef == null || activityRef.isFinishing()) return false;
            try {
                FileUtil.saveFeedbackDataToFile(activityRef, lists[0], lists[1], lists[2], features);
                Log.d(activityRef.TAG, "doInBackground: Feedback has been saved");
                return true;
            }catch (IOException e){
                Log.e(activityRef.TAG,"Error while saving feedback: " + e.getMessage());
                return false;
            }
        }

        protected void onPostExecute(Boolean success) {

            UserFeedbackGroundTruth activityRef = currentActivity.get();
            if (activityRef != null && !activityRef.isFinishing()) {
                Log.d(activityRef.TAG, "onPostExecute: Saved the files asynchronously");
                if (success) {
                    UserFeedbackQuestion.isInProgress = false;
                    activityRef.clearNotification(Notifications.NOTIFICATION_ID_FEEDBACK);
                    activityRef.finish();
                } else {
                    Log.d(activityRef.TAG, "onPostExecute: Error saving on force. Trying again");
                    //TODO: Verify that this works
                    activityRef.timelimitHandler.postDelayed(activityRef.timeLimitRunnable, FEEDBACK_NOTIFICATION_EXPIRATION_TIME);
                }
            }
        }
    }

}
