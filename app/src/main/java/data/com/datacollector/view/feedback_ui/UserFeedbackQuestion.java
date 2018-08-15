package data.com.datacollector.view.feedback_ui;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;

import data.com.datacollector.R;
import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Notifications;
import data.com.datacollector.utility.Util;
import data.com.datacollector.view.HomeActivity;

import static data.com.datacollector.model.Const.EXTRA_FEEDBACK_FEATURES;
import static data.com.datacollector.model.Const.EXTRA_FEEDBACK_PREDICTED_LABEL;
import static data.com.datacollector.model.Const.EXTRA_FEEDBACK_QUESTION;
import static data.com.datacollector.model.Const.EXTRA_FEEDBACK_VIBRATE;

/**
 * This class shows the UI feedback to request from the user and its managed by the Notifications utility on the requestFeedback method.
 * This is a yes no answer, if subject answers yes, no further action is required. If subject answers no, then the ground truth its to be collected (real label)
 */
public class UserFeedbackQuestion extends WearableActivity {

    private String TAG = "UserFeedbackQuestion";
    private TextView txtQuestion;
    private String feedbackQuestion = "";
    private String predictedLabel = "";
    private double features[];
    private Button btnYes;
    private Button btnNo;
    private NotificationManager notificationManager;
    public static boolean isInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //NOTE: If another type of feedback other than yes/no question is required, we can create
        // another layout fitting our requirements (for example extended questions and input)
        // and then set up the layout on this activity so no bigger changes are nedded other than naming conventions
        // and methods compatibility.
        //
        // This might not work, it could actually be required to create another activity with its own layot
        setContentView(R.layout.activity_user_feedback);

        txtQuestion = findViewById(R.id.txtQuestion);
        btnYes = findViewById(R.id.btnYes);
        btnNo = findViewById(R.id.btnNo);

        Intent intent = getIntent();
        if(intent != null) {
            feedbackQuestion = intent.getStringExtra(EXTRA_FEEDBACK_QUESTION);
            predictedLabel = intent.getStringExtra(EXTRA_FEEDBACK_PREDICTED_LABEL);
            features = intent.getDoubleArrayExtra(EXTRA_FEEDBACK_FEATURES);
            if(intent.getBooleanExtra(EXTRA_FEEDBACK_VIBRATE,false)){
                Notifications.vibrate(UserFeedbackQuestion.this.getApplicationContext());
            }
            Log.d(TAG, "onCreate: feedbackQuestion: " + feedbackQuestion);
            Log.d(TAG, "onCreate: predictedLabel: " + predictedLabel);
        }

        txtQuestion.setText(feedbackQuestion);


        //This simply modifies the running notification to open this activity instead the main one
        notificationManager = (NotificationManager) UserFeedbackQuestion.this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Notifications.NOTIFICATION_ID_RUNNING_SERVICES, Notifications.getServiceRunningNotification(UserFeedbackQuestion.this,UserFeedbackQuestion.class));

        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "onResume: ");
        enableButtons(true);
        isInProgress = true;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        isInProgress = false;
    }

    public void onClickYes(View v){
        saveAnswer(true);
    }

    public void onClickNo(View v){
        saveAnswer(false);
    }

    public void saveAnswer(boolean answer){

        enableButtons(false);
        if(answer){
            Log.d(TAG, "saveAnswer: The predicted label was correct");
            String timestamp = Util.getTimeMillis(System.currentTimeMillis());
            SaveFeedbackDataInBackground saveData = new SaveFeedbackDataInBackground(UserFeedbackQuestion.this, features);
            saveData.execute(timestamp, predictedLabel, predictedLabel); //The predicted was correct so its the actual label

        }else{
            Log.d(TAG, "saveAnswer: The predicted label was incorrect, prompting for the correct one");
            Intent feedbackGt = new Intent(UserFeedbackQuestion.this.getApplicationContext(), UserFeedbackGroundTruth.class);
            feedbackGt.putExtra(EXTRA_FEEDBACK_PREDICTED_LABEL, predictedLabel);
            feedbackGt.putExtra(EXTRA_FEEDBACK_FEATURES, features);
            startActivity(feedbackGt);
            isInProgress = false;
            UserFeedbackQuestion.this.finish();
        }
    }

    private void enableButtons(boolean b){
        btnYes.setEnabled(b);
        btnNo.setEnabled(b);
    }

    public static class SaveFeedbackDataInBackground extends AsyncTask<String, Integer, Boolean> {

        private WeakReference<UserFeedbackQuestion> currentActivity;
        private double features[];
        SaveFeedbackDataInBackground(UserFeedbackQuestion context, double features[]){
            currentActivity = new WeakReference<>(context);
            this.features = features;
        }

        protected Boolean doInBackground(String... lists) {
            UserFeedbackQuestion activityRef = currentActivity.get();
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

            UserFeedbackQuestion activityRef = currentActivity.get();
            if (activityRef != null && !activityRef.isFinishing()) {
                Log.d(activityRef.TAG, "onPostExecute: Saved the files asynchronously");

                if (success) {
                    UserFeedbackQuestion.isInProgress = false;
                    activityRef.clearNotification(Notifications.NOTIFICATION_ID_FEEDBACK);
                    activityRef.finish();
                } else {
                    Toast.makeText(activityRef, "Error saving, try again", Toast.LENGTH_LONG);
                    activityRef.enableButtons(true);
                }
            }
        }
    }

    //Removes the reminder notification and changes back the services notification intent to open the main activity instead of this.
    public void clearNotification(int id) {
        notificationManager.cancel(id);
        notificationManager.notify(Notifications.NOTIFICATION_ID_RUNNING_SERVICES, Notifications.getServiceRunningNotification(this,HomeActivity.class));
    }
}
