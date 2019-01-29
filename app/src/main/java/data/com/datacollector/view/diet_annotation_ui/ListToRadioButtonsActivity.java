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
import android.widget.Toast;


import data.com.datacollector.R;
import data.com.datacollector.utility.diet_annotation_adapters.ListToRadioButtonsAdapter;

import static data.com.datacollector.model.Const.EATING_ACTIVITY_SECOND_TIER_QUESTIONS;

/**
 * Lists the questions on which multiple answers could be given. This will hold and manage the answers
 * for the radio button view
 */
public class ListToRadioButtonsActivity extends WearableActivity {

    private String TAG = "DC_AnnotationMainActivity";
    private ListToRadioButtonsAdapter adapterList;
    private WearableRecyclerView recMainDetailActivityList;
    private FrameLayout progressBar;
    private int previousEvent = MotionEvent.ACTION_UP;
    public static boolean isInProgress = false;
    public int incompleteId = -1;

    public int[] answersIds = new int[EATING_ACTIVITY_SECOND_TIER_QUESTIONS.length];

    private BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mStopServicesReceiver onReceive: called");
            String action = intent.getAction();

            //Action that tells us to stop our services
            if (action.equals("RADIO_ANSWER")) {

                int answerId = intent.getExtras().getInt("ANSWER_ID");
                int questionId = intent.getExtras().getInt("QUESTION_ID");

                Log.d(TAG, "onReceive: AnswerID: " + String.valueOf(answerId));
                Log.d(TAG, "onReceive: QuestionID: " + String.valueOf(questionId));

                //For questionId the answer selected is answerId (from the set of corresponding answer to this question)
                answersIds[questionId] = answerId;

            } else {
                Log.d(TAG, "onReceive: Not recognized intent");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_to_radio_buttons);

        IntentFilter confirmationIntent = new IntentFilter();
        confirmationIntent.addAction("RADIO_ANSWER");
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalReceiver, confirmationIntent);

        // Enables Always-on
        setAmbientEnabled();
        setIds();
        setRecyclerView();
    }

    public void setIds(){
        for (int i=0;i<answersIds.length;i++){
            answersIds[i]=-1;
        }
    }

    public void setRecyclerView(){
        //initialize the list that holds all of the labels
        recMainDetailActivityList =  findViewById((R.id.recMainDetail2ActivityList));
        //recActivitiesList.setCircularScrollingGestureEnabled(true); Consider if this might be easier for the user
        recMainDetailActivityList.setLayoutManager(new WearableLinearLayoutManager((this)));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recMainDetailActivityList.getContext(), DividerItemDecoration.VERTICAL);
        recMainDetailActivityList.addItemDecoration(dividerItemDecoration);

        //Set up recycler view adapter with the obtained list

        adapterList = new ListToRadioButtonsAdapter(EATING_ACTIVITY_SECOND_TIER_QUESTIONS);
        recMainDetailActivityList.setAdapter(adapterList);

        //We intercept multiple touches and prevent any other after the first one arrives
        recMainDetailActivityList.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
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

    public boolean isComplete(){
        boolean isComplete = true;
        for (int i=0;i<answersIds.length;i++){
            if(answersIds[i] == -1){
                incompleteId = i;
                isComplete = false;
                break;
            }
        }

        return isComplete;
    }

    public void onClickDone(View v){
        Log.d(TAG, "onClickDone:");
        if (isComplete()){
            Log.d(TAG, "onClickDone: Is complete");
        }else{
            Toast.makeText(ListToRadioButtonsActivity.this,
                    "Must answer: " + EATING_ACTIVITY_SECOND_TIER_QUESTIONS[incompleteId],
                    Toast.LENGTH_SHORT)
                    .show();
        }

        
        //TODO: This button should be disabled until all the radios are selected
        //TODO: Make sure the information is saved into files
        //TODO: Make sure I finish the images for the paper
    }

}
