package data.com.datacollector.view.diet_annotation_ui;

import android.os.Bundle;
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

public class AnnotationMainActivity extends WearableActivity {

    private TextView mTextView;
    private String TAG = "AnnotationMainActivity";
    private MainActivityAnnotationAdapter adapterList;
    private WearableRecyclerView recLabelsList;
    private FrameLayout progressBar;
    private int previousEvent = MotionEvent.ACTION_UP;
    public static boolean isInProgress = false;


    String activitiesList[] = {"Eating", "Walking", "Exercising", "Sitting", "Biking", "Nothing", "Other"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_annotation_main_activity);

        mTextView = (TextView) findViewById(R.id.text);

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

        adapterList = new MainActivityAnnotationAdapter(activitiesList);
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

//    public void setLoading(boolean b){
//        if(b){
//            Log.d(TAG, "setLoading: true");
//            recLabelsList.setVisibility(View.GONE);
//            progressBar.setVisibility(View.VISIBLE);
//        }else{
//            Log.d(TAG, "setLoading: false");
//            recLabelsList.setVisibility(View.VISIBLE);
//            progressBar.setVisibility(View.GONE);
//        }
//    }
}
