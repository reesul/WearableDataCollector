package data.com.datacollector.utility.diet_annotation_adapters;

import android.graphics.Color;
import android.support.wear.widget.WearableRecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class BaseAnnotationAdapter extends WearableRecyclerView.Adapter<BaseAnnotationAdapter.ViewHolder>{
    public String[] itemsList;
    private final String TAG = "DC_BaseAnnotationAdapter";

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
    public BaseAnnotationAdapter(String[] itemsList) {
        this.itemsList = itemsList;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public BaseAnnotationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // create a new view
        TextView v = new TextView(parent.getContext());

        //Used for dinamically get dimensions in DP rather than plain pixels
        DisplayMetrics dm = parent.getContext().getResources().getDisplayMetrics();
        float myTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14F, dm);
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
        holder.mTextView.setText(itemsList[position]);
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
                onItemClick(v, listItemPosition, txtView);
            }
        });

    }

    /**
     * This method is to be override when this parent class is inherited as this will be the action
     * to take when a list element is touched
     * @param v
     */
    public void onItemClick(View v, int listItemPosition, TextView txtView){
        Log.d(TAG, "onItemClick: TOUCHED on parent");
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return itemsList.length;
    }
}