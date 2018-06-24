package data.com.datacollector.utility;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wear.widget.WearableRecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import data.com.datacollector.service.LeBLEService;
import data.com.datacollector.service.SensorService;
import data.com.datacollector.view.ReminderTimeConfigActivity;

import static data.com.datacollector.model.Const.DISMISS_FEEDBACK_QUESTION_ACTIVITY;
import static data.com.datacollector.model.Const.SET_LOADING;
import static data.com.datacollector.model.Const.EXTRA_ACTIVITY_LABEL;
import static data.com.datacollector.model.Const.SET_LOADING_HOME_ACTIVITY;
import static data.com.datacollector.model.Const.SET_LOADING_USER_FEEDBACK_QUESTION;


/**
 * Adapter for the ActivitiesList recycler view
 * Created by ROGER on 2/5/2018.
 */

public class ActivitiesAdapter extends WearableRecyclerView.Adapter<ActivitiesAdapter.ViewHolder>{
    private String[] activitiesList;
    private final String TAG = "DC_ActivitiesAdapter";
    private String type;
    private String predictedLabel = "";
    private String predictionStartLbl = "";
    private String predictionEndLbl = "";

    // Provide a reference to the views for each data item
    public static class ViewHolder extends WearableRecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public ViewHolder(TextView v) {
            super(v);
            mTextView = v;
        }
    }

    //Constructor with only a list of strings
    public ActivitiesAdapter(String[] activitiesList) {
        this.type = "LABELING";
        this.activitiesList = activitiesList;
    }

    public ActivitiesAdapter(String[] activitiesList, String predictedLabel, String predictionStartLbl, String predictionEndLbl) {
        this.type = "FEEDBACK";
        this.activitiesList = activitiesList;
        this.predictedLabel = predictedLabel;
        this.predictionStartLbl = predictionStartLbl;
        this.predictionEndLbl = predictionEndLbl;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ActivitiesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // create a new view
        TextView v = new TextView(parent.getContext());

        if(type.equals("LABELING")){
            //Used for dinamically get dimensions in DP rather than plain pixels
            DisplayMetrics dm = parent.getContext().getResources().getDisplayMetrics();
            float myTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18F, dm);
            int paddLeftRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 2F, dm);
            int paddTopBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 4F, dm);

            v.setTextSize(myTextSize);
            v.setPadding(paddLeftRight, paddTopBottom, paddLeftRight, paddTopBottom);
        }else if (type.equals("FEEDBACK")){
            //Used for dinamically get dimensions in DP rather than plain pixels
            DisplayMetrics dm = parent.getContext().getResources().getDisplayMetrics();
            float myTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14F, dm);
            int paddLeftRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 2F, dm);
            int paddTopBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 4F, dm);

            v.setTextSize(myTextSize);
            v.setPadding(paddLeftRight, paddTopBottom, paddLeftRight, paddTopBottom);
        }


        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.mTextView.setText(activitiesList[position]);
        holder.mTextView.setTextColor(Color.WHITE);
        final int listItemPosition = position;

        if(type.equals("LABELING")){

            holder.mTextView.setOnClickListener(new View.OnClickListener() {
                //Context ctx = holder.mTextView.getContext();
                TextView txtView = holder.mTextView;

                /**
                 * Called when the user clicks on an activity label
                 * @param v
                 */
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick label labeling");
                    Context context = txtView.getContext();
                    //TODO: If multiple services are added, this must change
                    if (LeBLEService.isServiceRunning && SensorService.isServiceRunning) {
                        Log.d(TAG, "Saving activity on background: " + activitiesList[listItemPosition]);
                        //Save information to file
                        String timestamp = Util.getTimeMillis(System.currentTimeMillis());
                        SaveDataInBackground backgroundSave = new SaveDataInBackground(context, listItemPosition);
                        backgroundSave.execute(timestamp, activitiesList[listItemPosition]);

                    } else {
                        Log.d(TAG, "onClick: The app is not collecting any data");
                        //Re enable window
                        Intent intent = new Intent(SET_LOADING_HOME_ACTIVITY);
                        intent.putExtra(SET_LOADING,false);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        Toast.makeText(context, "The app is not collecting data", Toast.LENGTH_LONG).show();
                    }
                }
            });

        }else if (type.equals("FEEDBACK")){

            holder.mTextView.setOnClickListener(new View.OnClickListener() {
                //Context ctx = holder.mTextView.getContext();
                TextView txtView = holder.mTextView;

                /**
                 * Called when the user clicks on an activity label
                 * @param v
                 */
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick label feedback");
                    Context context = txtView.getContext();
                    Log.d(TAG, "Saving feedback on background: " + activitiesList[listItemPosition]);
                    //Save information to file
                    String timestamp = Util.getTimeMillis(System.currentTimeMillis());
                    SaveFeedbackDataInBackground saveData = new SaveFeedbackDataInBackground(context);
                    saveData.execute(timestamp, predictedLabel, activitiesList[listItemPosition], predictionStartLbl, predictionEndLbl); //The predicted was correct so its the actual label
                }
            });

        }

    }

    private class SaveDataInBackground extends AsyncTask<String, Integer, Boolean> {

        Context context;
        String activity;
        int listItemPosition;

        public SaveDataInBackground(Context context, int listItemPosition){
            this.context = context;
            this.listItemPosition = listItemPosition;
        }

        protected Boolean doInBackground(String... lists) {
            try {
                activity = lists[1];
                FileUtil.saveActivityDataToFile(context, lists[0], activity, "start");
                return true;
            }catch (IOException e){
                Log.e(TAG,"Error while saving activity: " + e.getMessage());
                return false;
            }
        }

        protected void onPostExecute(Boolean success) {
            Log.d(TAG, "onPostExecute: Saved the files asynchronously");

            if(success){
                //Launch time select activity
                Intent intent = new Intent(context, ReminderTimeConfigActivity.class);
                intent.putExtra(EXTRA_ACTIVITY_LABEL, activitiesList[listItemPosition]);
                context.startActivity(intent);
            }else{
                //Re enable window
                Intent intent = new Intent(SET_LOADING_HOME_ACTIVITY);
                intent.putExtra(SET_LOADING,false);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                Toast.makeText(context, "Error saving, try again", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class SaveFeedbackDataInBackground extends AsyncTask<String, Integer, Boolean> {

        Context context;

        public SaveFeedbackDataInBackground(Context context){
            this.context = context;
        }

        protected Boolean doInBackground(String... lists) {
            try {
                FileUtil.saveFeedbackDataToFile(context, lists[0], lists[1], lists[2], lists[3], lists[4]);
                Log.d(TAG, "doInBackground: Feedback has been saved");
                return true;
            }catch (IOException e){
                Log.e(TAG,"Error while saving feedback: " + e.getMessage());
                return false;
            }
        }

        protected void onPostExecute(Boolean success) {
            Log.d(TAG, "onPostExecute: Saved the files asynchronously");

            if(success){
                //TODO: Dismiss this activity and any possible notification
                Intent intent = new Intent(SET_LOADING_USER_FEEDBACK_QUESTION);
                intent.putExtra(DISMISS_FEEDBACK_QUESTION_ACTIVITY,true);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }else{
                //Re enable window
                Intent intent = new Intent(SET_LOADING_USER_FEEDBACK_QUESTION);
                intent.putExtra(SET_LOADING,false);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                Toast.makeText(context, "Error saving, try again", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return activitiesList.length;
    }
}