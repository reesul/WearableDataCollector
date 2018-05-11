package data.com.datacollector.utility;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
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

import static data.com.datacollector.model.Const.EXTRA_ACTIVITY_LABEL;


/**
 * Adapter for the ActivitiesList recycler view
 * Created by ROGER on 2/5/2018.
 */

public class ActivitiesAdapter extends WearableRecyclerView.Adapter<ActivitiesAdapter.ViewHolder>{
    private String[] activitiesList;
    private final String TAG = "DC_ActivitiesAdapter";

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
        this.activitiesList = activitiesList;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ActivitiesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // create a new view
        TextView v = new TextView(parent.getContext());

        //Used for dinamically get dimensions in DP rather than plain pixels
        DisplayMetrics dm = parent.getContext().getResources().getDisplayMetrics();
        float myTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18F, dm);
        int paddLeftRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 2F, dm);
        int paddTopBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 4F, dm);

        v.setTextSize(myTextSize);
        v.setPadding(paddLeftRight, paddTopBottom, paddLeftRight, paddTopBottom);

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

        holder.mTextView.setOnClickListener(new View.OnClickListener() {
            //Context ctx = holder.mTextView.getContext();
            TextView txtView = holder.mTextView;

            /**
             * Called when the user clicks on an activity label
             * @param v
             */
            @Override
            public void onClick(View v) {
                Log.d(TAG,"Saving activity on background: " + activitiesList[listItemPosition]);
                Context context = txtView.getContext();
                if(LeBLEService.isServiceRunning && SensorService.isServiceRunning){
                    //Save information to file
                    String timestamp = Util.getTime(System.currentTimeMillis());
                    SaveDataInBackground backgroundSave = new SaveDataInBackground(context);
                    backgroundSave.execute(timestamp, activitiesList[listItemPosition]);

                    //Launch time select activity
                    Intent intent = new Intent(context, ReminderTimeConfigActivity.class);
                    intent.putExtra(EXTRA_ACTIVITY_LABEL, activitiesList[listItemPosition]);
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "The app is not collecting data", Toast.LENGTH_LONG).show();
                }

            }
        });

    }

    private class SaveDataInBackground extends AsyncTask<String, Integer, Void> {

        Context context;
        String activity;

        public SaveDataInBackground(Context context){
            this.context = context;
        }

        protected Void doInBackground(String... lists) {
            try {
                activity = lists[1];
                FileUtil.saveActivityDataToFile(context, lists[0], activity);
            }catch (IOException e){
                Log.e(TAG,"Error while saving activity: " + e.getMessage());
                Toast.makeText(context, "Error, try again later", Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        protected void onPostExecute(Void v) {
            Log.d(TAG, "onPostExecute: Saved the files asynchronously");

            Toast.makeText(context, activity + " saved", Toast.LENGTH_SHORT).show();
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return activitiesList.length;
    }
}