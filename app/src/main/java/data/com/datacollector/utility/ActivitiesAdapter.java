package data.com.datacollector.utility;

import android.content.Context;
import android.support.wear.widget.WearableRecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import data.com.datacollector.R;

/**
 * Created by ROGER on 2/5/2018.
 */

public class ActivitiesAdapter extends WearableRecyclerView.Adapter<ActivitiesAdapter.ViewHolder>{
    private String[] mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends WearableRecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public ViewHolder(TextView v) {
            super(v);
            mTextView = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ActivitiesAdapter(String[] myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ActivitiesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        /*TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_element, parent, false);*/

        TextView v = new TextView(parent.getContext());
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
        holder.mTextView.setText(mDataset[position]);

        holder.mTextView.setOnClickListener(new View.OnClickListener() {
            //Context ctx = holder.mTextView.getContext();
            TextView txtView = holder.mTextView;
            @Override
            public void onClick(View v) {
                Toast.makeText(txtView.getContext(), txtView.getText(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}
