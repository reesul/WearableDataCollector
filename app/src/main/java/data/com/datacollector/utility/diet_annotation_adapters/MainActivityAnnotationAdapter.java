package data.com.datacollector.utility.diet_annotation_adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import data.com.datacollector.view.diet_annotation_ui.CheckboxListActivity;

public class MainActivityAnnotationAdapter extends BaseAnnotationAdapter {
    private final String TAG = "DC_MainActivityAnnotationAdapter";

    public MainActivityAnnotationAdapter(String[] itemsList) {
        super(itemsList);
    }

    public void onItemClick(View v, int listItemPosition, TextView txtView){
        //super.onItemClick(v,int ,  txtView);
        Log.d(TAG, "onItemClick: The item " + super.itemsList[listItemPosition] + " has been selected");
        //TODO: Check that the other adapter is not interfering with the parent itemsList

        Context ctx = txtView.getContext();
        if(listItemPosition == 0){
            //Eating activity has been selected
            Intent intent = new Intent(ctx, CheckboxListActivity.class);
            ctx.startActivity(intent);

        } else {
            //Another activity was selected
            Toast.makeText(ctx,"TBD",Toast.LENGTH_SHORT).show();
        }
    }

}
