package data.com.datacollector.utility.diet_annotation_adapters;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivityAnnotationAdapter extends BaseAnnotationAdapter {
    private final String TAG = "DC_MainActivityAnnotationAdapter";

    public MainActivityAnnotationAdapter(String[] itemsList) {
        super(itemsList);
    }

    public void onItemClick(View v, int listItemPosition, TextView txtView){
        //super.onItemClick(v,int ,  txtView);
        Log.d(TAG, "onItemClick: The item " + super.itemsList[listItemPosition] + " has been selected");
        //TODO: Check that the other adapter is not interfering with the parent itemsList

        if(listItemPosition == 0){
            //Eating activity has been selected
            
        } else {
            //Another activity was selected
            Context ctx = txtView.getContext();
            Toast.makeText(ctx,"TBD",Toast.LENGTH_SHORT).show();
        }

        //        Log.d(TAG, "onClick label feedback");
//        Context context = txtView.getContext();
//        Log.d(TAG, "Saving feedback on background: " + activitiesList[listItemPosition]);
//        //Save information to file
//        String timestamp = Util.getTimeMillis(System.currentTimeMillis());
//        SaveFeedbackDataInBackground saveData = new SaveFeedbackDataInBackground(context, TAG);
//        saveData.execute(timestamp, predictedLabel, activitiesList[listItemPosition], predictionStartLbl, predictionEndLbl); //The predicted was correct so its the actual label


//        Context context = txtView.getContext();
//        //TODO: If multiple services are added, this must change
//        if (LeBLEService.isServiceRunning && SensorService.isServiceRunning) {
//            Log.d(TAG, "Saving activity on background: " + activitiesList[listItemPosition]);
//            //Save information to file
//            String timestamp = Util.getTimeMillis(System.currentTimeMillis());
//            SaveDataInBackground backgroundSave = new SaveDataInBackground(context, activitiesList[listItemPosition], TAG);
//            backgroundSave.execute(timestamp);
//
//        } else {
//            Log.d(TAG, "onClick: The app is not collecting any data");
//            //Re enable window
//            Intent intent = new Intent(SET_LOADING_HOME_ACTIVITY);
//            intent.putExtra(SET_LOADING,false);
//            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
//            Toast.makeText(context, "The app is not collecting data", Toast.LENGTH_LONG).show();
//        }
    }

}
