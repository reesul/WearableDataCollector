package data.com.datacollector.view.diet_annotation_ui;

import android.content.Intent;
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

import data.com.datacollector.R;
import data.com.datacollector.utility.diet_annotation_adapters.CheckboxListAdapter;

import static data.com.datacollector.model.Const.EATING_ACTIVITY_FIRST_TIER_QUESTIONS;

public class CheckboxListActivity extends WearableActivity {

    private String TAG = "DC_CheckboxListActivity";
    private CheckboxListAdapter adapterList;
    private WearableRecyclerView recMainDetailActivityList;
    private FrameLayout progressBar;
    private int previousEvent = MotionEvent.ACTION_UP;
    public static boolean isInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkbox_list);

        // Enables Always-on
        setAmbientEnabled();

        setRecyclerView();
    }

    public void setRecyclerView(){
        //initialize the list that holds all of the labels
        recMainDetailActivityList =  findViewById((R.id.recMainDetailActivityList));
        //recActivitiesList.setCircularScrollingGestureEnabled(true); Consider if this might be easier for the user
        recMainDetailActivityList.setLayoutManager(new WearableLinearLayoutManager((this)));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recMainDetailActivityList.getContext(), DividerItemDecoration.VERTICAL);
        recMainDetailActivityList.addItemDecoration(dividerItemDecoration);

        //Set up recycler view adapter with the obtained list

        adapterList = new CheckboxListAdapter(EATING_ACTIVITY_FIRST_TIER_QUESTIONS);
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

    public void onClickDetailsNext(View v){
        Log.d(TAG, "onClickDetailsNext: ");
        String t = "";
        for (int i=0;i<adapterList.itemsListChecks.length;i++){
            t+= adapterList.itemsListChecks[i]? "Checked\n":"Not Checked\n";
        }
        Log.d(TAG, "onClickDetailsDone: " + t);

        Intent intent = new Intent(CheckboxListActivity.this, ListToRadioButtonsActivity.class);
        startActivityForResult(intent,1);
//        CheckboxListActivity.this.startActivity(intent);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                int[] returnedResult = data.getExtras().getIntArray("RADIO_ANSWER_IDS");
                Log.d(TAG, "onActivityResult: ");
                Intent intent = new Intent("SAVE_DETAILED_DATA");
                intent.putExtra("CHECKBOX_DATA", adapterList.itemsListChecks);
                intent.putExtra("RADIO_DATA", returnedResult);
                LocalBroadcastManager.getInstance(CheckboxListActivity.this).sendBroadcast(intent);
                finish();
            }
        }
    }

}
