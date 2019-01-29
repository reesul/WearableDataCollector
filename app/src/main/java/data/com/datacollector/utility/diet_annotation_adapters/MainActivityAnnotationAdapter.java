package data.com.datacollector.utility.diet_annotation_adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import data.com.datacollector.view.diet_annotation_ui.CheckboxListActivity;
import data.com.datacollector.view.diet_annotation_ui.RadioButtonsActivity;

import static data.com.datacollector.model.Const.FIRST_TIER_ACTIVITIES;
import static data.com.datacollector.model.Const.REMAINING_ACTIVITIES_ANSWERS;

public class MainActivityAnnotationAdapter extends BaseAnnotationAdapter {
    private final String TAG = "DC_MainActivityAnnotationAdapter";

    public MainActivityAnnotationAdapter(String[] itemsList) {
        super(itemsList);
    }

    public void onItemClick(View v, int listItemPosition, TextView txtView){
        //super.onItemClick(v,int ,  txtView);
        Log.d(TAG, "onItemClick: The item " + super.itemsList[listItemPosition] + " has been selected");

        Context ctx = txtView.getContext();
        if(listItemPosition == 0){
            //Eating activity has been selected
            Intent intent = new Intent(ctx, CheckboxListActivity.class);
            ctx.startActivity(intent);

        } else if(listItemPosition<(FIRST_TIER_ACTIVITIES.length-2)) {
            //TODO: -2 must change if the questions change, since the last two do not require extra, thats why -2
            //Another activity was selected
            //TODO: Next questions
            Intent intent = new Intent(ctx, RadioButtonsActivity.class);
            intent.putExtra("ANSWERS", REMAINING_ACTIVITIES_ANSWERS.get("a"+listItemPosition));
            intent.putExtra("QUESTION_ID", listItemPosition);
            intent.putExtra("MAIN_ACTIVITY_ANNOTATION", true);
            ctx.startActivity(intent);
        } else {
            Log.d(TAG, "onItemClick: Selected: " + super.itemsList[listItemPosition]);
            //TODO: Save this
        }
    }

}
