package data.com.datacollector.view.diet_annotation_ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.TextView;

import data.com.datacollector.R;
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

    //TODO: This must change if new questions are added. 3 because only 3 activities besides eating require more info
    public int[] answersIds = new int[3];//Just 3 questions

    private BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mStopServicesReceiver onReceive: called");
            String action = intent.getAction();

            //Action that tells us to stop our services
            if (action.equals("RADIO_ANSWER_MAIN")) {

                int answerId = intent.getExtras().getInt("ANSWER_ID");
                int questionId = intent.getExtras().getInt("QUESTION_ID");

                Log.d(TAG, "onReceive: AnswerID: " + String.valueOf(answerId));
                Log.d(TAG, "onReceive: QuestionID: " + String.valueOf(questionId));

                //For questionId the answer selected is answerId (from the set of corresponding answer to this question)
                //questionId-1 since the activities start after eating, therefore 1
                answersIds[questionId-1] = answerId;

                //TODO: Save
                AnnotationMainActivity.this.finish();

            } else {
                Log.d(TAG, "onReceive: Not recognized intent");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_annotation_main_activity);

        IntentFilter confirmationIntent = new IntentFilter();
        confirmationIntent.addAction("RADIO_ANSWER_MAIN");
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

}
