package data.com.datacollector.view.diet_annotation_ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;

import data.com.datacollector.R;
import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Util;
import data.com.datacollector.utility.diet_annotation_adapters.MainActivityAnnotationAdapter;
import static data.com.datacollector.model.Const.FIRST_TIER_ACTIVITIES;

/**
 * Main activity where the list of all the possible activities performed by the user are listed
 */
public class AnnotationMainActivity extends WearableActivity {

    private String TAG = "DC_AnnotationMainActivity";
    private MainActivityAnnotationAdapter adapterList;
    private WearableRecyclerView recLabelsList;
    private FrameLayout progressBar;
    private int previousEvent = MotionEvent.ACTION_UP;
    public static boolean isInProgress = false;
    public String startTimestamp = "";

    //TODO: This must change if new questions are added. 3 because only 3 activities besides eating require more info
    //public int[] answersIds = new int[3];//Just 3 questions

    private BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mLocalReceiver onReceive: called");
            String action = intent.getAction();

            if (action.equals("RADIO_ANSWER_MAIN")) {
                //Saving the answers of the radio buttons found in the main view. In other words, for the
                //questions which requiered details
                Log.d(TAG, "RADIO_ANSWER_MAIN");
                int answerId = intent.getExtras().getInt("ANSWER_ID");
                int questionId = intent.getExtras().getInt("QUESTION_ID");

                Log.d(TAG, "onReceive: AnswerID: " + String.valueOf(answerId));
                Log.d(TAG, "onReceive: QuestionID: " + String.valueOf(questionId));

                //For questionId the answer selected is answerId (from the set of corresponding answer to this question)
                //questionId-1 since the activities start after eating, therefore 1
                //answersIds[questionId-1] = answerId; //THIS IS NOT USED ON THIS ACTIVITY

                String endTimestamp = Util.getTimeMillis(System.currentTimeMillis());
                String text = startTimestamp + "," + endTimestamp + "," + String.valueOf(questionId) + "," + String.valueOf(answerId);
                String header = "Timestamp start, Timestamp end, Question ID, Answer ID";
                SaveAnnotationDataInBackground backgroundSave = new SaveAnnotationDataInBackground(context, text, "diet_other",header, TAG);
                backgroundSave.execute();


            } else if (action.equals("FINISH_MAIN")){
                Log.d(TAG, "FINISH_MAIN");

                //Make sure we unregister this receiver so the next time
                //this activity is created we do not stack up receivers and have
                //multiple calls
                Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show();
                LocalBroadcastManager.getInstance(AnnotationMainActivity.this).unregisterReceiver(mLocalReceiver);
                AnnotationMainActivity.this.finish();
            } else if (action.equals("SAVE_DETAILED_DATA")){
                Log.d(TAG, "onReceive: SAVE_DETAILED_DATA");
                boolean checkboxData[] = intent.getExtras().getBooleanArray("CHECKBOX_DATA");
                int radioData[] = intent.getExtras().getIntArray("RADIO_DATA");

                String checkData = "";
                String radData = "";

                for (int i =0; i<checkboxData.length;i++){
                    checkData += checkboxData[i]? "1":"0";
                    if((i+1)<checkboxData.length){
                        checkData +=",";
                    }
                }

                for (int i =0; i<radioData.length;i++){
                    radData += String.valueOf(radioData[i]);
                    if((i+1)<radioData.length){
                        radData +=",";
                    }
                }

                String endTimestamp = Util.getTimeMillis(System.currentTimeMillis());
                String text = startTimestamp + "," + endTimestamp + "," +
                        checkData + "," + radData;
                String header = "Timestamp start, Timestamp end, First 9 yes(1) no(0) questions, Next 6 answers (id=order) for the detailed question";
                SaveAnnotationDataInBackground backgroundSave = new SaveAnnotationDataInBackground(context, text, "diet_eating", header, TAG);
                backgroundSave.execute();
            } else {
                Log.d(TAG, "onReceive: Not recognized intent");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_annotation_main_activity);

        startTimestamp = getIntent().getExtras().getString("START_TS");

        IntentFilter confirmationIntent = new IntentFilter();
        confirmationIntent.addAction("RADIO_ANSWER_MAIN");
        confirmationIntent.addAction("FINISH_MAIN");
        confirmationIntent.addAction("SAVE_DETAILED_DATA");
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalReceiver, confirmationIntent);

        // Enables Always-on
        setAmbientEnabled();
        setRecyclerView();
    }

    public void setRecyclerView(){
        //initialize the list that holds all of the labels
        recLabelsList =  findViewById((R.id.recMainActivityList));
        //recActivitiesList.setCircularScrollingGestureEnabled(true); Consider if this might be easier for the user
        recLabelsList.setLayoutManager(new WearableLinearLayoutManager((this)));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recLabelsList.getContext(), DividerItemDecoration.VERTICAL);
        recLabelsList.addItemDecoration(dividerItemDecoration);

        //Set up recycler view adapter with the obtained list

        adapterList = new MainActivityAnnotationAdapter(FIRST_TIER_ACTIVITIES);
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
//                        setLoading(true);// This prevents user from submitting multiple labels when touching quickly
                    }
                    //else ignore
                    previousEvent = e.getAction();
                }

                return false;
            }
        });

        progressBar = findViewById(R.id.mainAnnotationProgress);
    }

    public static class SaveAnnotationDataInBackground extends AsyncTask<String, Integer, Boolean> {

        private WeakReference<Context> ctx;
        private String TAG;
        String text;
        String fileName;
        String header;

        SaveAnnotationDataInBackground(Context context, String text, String fileName, String header, String TAG){
            ctx = new WeakReference<>(context);
            this.text = text;
            this.TAG = TAG;
            this.fileName = fileName;
            this.header = header;
        }

        protected Boolean doInBackground(String... lists) {
            Context context = ctx.get();
            if (context == null) return false;
            try {
                FileUtil.saveDietAnnotation(context, text, fileName, header);
                return true;
            }catch (IOException e){
                Log.e(TAG,"Error while saving activity: " + e.getMessage());
                return false;
            }
        }

        protected void onPostExecute(Boolean success) {
            Context context = ctx.get();
            if (context != null ) {
                Log.d(TAG, "onPostExecute: Saved the files asynchronously");

                if (success) {
                    //Launch time select activity
                    Log.d(TAG, "onPostExecute: Finishing activity");
                    Intent intent = new Intent("FINISH_MAIN");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                } else {
                    Toast.makeText(context, "Error saving, try again", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}
