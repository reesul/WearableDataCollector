package data.com.datacollector.utility.diet_annotation_adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivityDetails2Adapter extends BaseAnnotationAdapter {
    private final String TAG = "DC_MainActivityDetails2Adapter";

    public MainActivityDetails2Adapter(String[] itemsList) {
        super(itemsList);
    }

    public void onItemClick(View v, int listItemPosition, TextView txtView){
        //super.onItemClick(v,int ,  txtView);
        Log.d(TAG, "onItemClick: The item " + super.itemsList[listItemPosition] + " has been selected");
        //TODO: Check that the other adapter is not interfering with the parent itemsList

        Context ctx = txtView.getContext();
        //Intent intent = new Intent(ctx, DetailsMainActivity.class);
        //ctx.startActivity(intent);
    }
}
