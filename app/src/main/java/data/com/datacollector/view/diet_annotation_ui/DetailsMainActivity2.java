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

import data.com.datacollector.R;
import data.com.datacollector.utility.diet_annotation_adapters.MainActivityDetails2Adapter;

public class DetailsMainActivity2 extends WearableActivity {

    private String TAG = "AnnotationMainActivity";
    private MainActivityDetails2Adapter adapterList;
    private WearableRecyclerView recMainDetailActivityList;
    private FrameLayout progressBar;
    private int previousEvent = MotionEvent.ACTION_UP;
    public static boolean isInProgress = false;


    String activitiesList[] = {
            "What did you eat?",
            "Where did you eat?",
            "What did you drink?",
            "What dessert did you have?",
            "How often do you eat this",
            "What were you doing while eating?"
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_main2);

        // Enables Always-on
        setAmbientEnabled();
        setRecyclerView();
    }

    public void setRecyclerView(){
        //initialize the list that holds all of the labels
        recMainDetailActivityList =  findViewById((R.id.recMainDetail2ActivityList));
        //recActivitiesList.setCircularScrollingGestureEnabled(true); Consider if this might be easier for the user
        recMainDetailActivityList.setLayoutManager(new WearableLinearLayoutManager((this)));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recMainDetailActivityList.getContext(), DividerItemDecoration.VERTICAL);
        recMainDetailActivityList.addItemDecoration(dividerItemDecoration);

        //Set up recycler view adapter with the obtained list

        adapterList = new MainActivityDetails2Adapter(activitiesList);
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

    public void onClickDetails2Next(View v){
        Log.d(TAG, "onClickDetails2Next: ");
        //TODO: This button should be disabled until all the radios are selected

    }

}
