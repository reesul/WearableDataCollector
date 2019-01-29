package data.com.datacollector.utility.diet_annotation_adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import data.com.datacollector.view.diet_annotation_ui.RadioButtonsActivity;

import static data.com.datacollector.model.Const.EATING_SECOND_TIER_ANSWERS;

public class ListToRadioButtonsAdapter extends BaseAnnotationAdapter {
    private final String TAG = "DC_MainActivityDetails2Adapter";

    public ListToRadioButtonsAdapter(String[] itemsList) {
        super(itemsList);
    }

    public void onItemClick(View v, int listItemPosition, TextView txtView){
        //super.onItemClick(v,int ,  txtView);
        Log.d(TAG, "onItemClick: The item " + super.itemsList[listItemPosition] + " has been selected");
        //TODO: Check that the other adapter is not interfering with the parent itemsList

        Context ctx = txtView.getContext();
        Intent intent = new Intent(ctx, RadioButtonsActivity.class);
        intent.putExtra("ANSWERS", EATING_SECOND_TIER_ANSWERS.get("a"+listItemPosition));
        intent.putExtra("QUESTION_ID", listItemPosition);
        v.setBackgroundColor(Color.argb(150,250,250,250));
        ctx.startActivity(intent);
    }
}
