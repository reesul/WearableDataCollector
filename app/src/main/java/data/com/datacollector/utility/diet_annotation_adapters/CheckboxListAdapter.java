package data.com.datacollector.utility.diet_annotation_adapters;

import android.graphics.Color;
import android.support.wear.widget.WearableRecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

public class CheckboxListAdapter extends WearableRecyclerView.Adapter<CheckboxListAdapter.ViewHolder>{
    public String[] itemsList;
    public boolean[] itemsListChecks;
    private final String TAG = "DC_MainActivityDetailsAdapter";

    // Provide a reference to the views for each data item
    public static class ViewHolder extends WearableRecyclerView.ViewHolder {
        // each data item is just a string in this case
        public CheckBox mCheckBox;
        public ViewHolder(CheckBox c) {
            super(c);
            mCheckBox = c;
        }
    }

    //Constructor with only a list of strings
    public CheckboxListAdapter(String[] itemsList) {
        this.itemsList = itemsList;

        //Initialize boolean reference for checked box
        itemsListChecks = new boolean[itemsList.length];
        for (int i=0;i<itemsList.length;i++) {
            itemsListChecks[i]=false;
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public CheckboxListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // create a new view
        CheckBox c = new CheckBox(parent.getContext());

        //Used for dinamically get dimensions in DP rather than plain pixels
        DisplayMetrics dm = parent.getContext().getResources().getDisplayMetrics();
        float myTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 17F, dm);
        int paddLeftRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 2F, dm);
        int paddTopBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 4F, dm);
        c.setTextSize(myTextSize);
        c.setPadding(paddLeftRight, paddTopBottom, paddLeftRight, paddTopBottom);

        ViewHolder vh = new ViewHolder(c);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.mCheckBox.setText(itemsList[position]);
        holder.mCheckBox.setTextColor(Color.WHITE);
        holder.mCheckBox.setOnCheckedChangeListener(null);
        holder.mCheckBox.setChecked(itemsListChecks[position]);


        final int listItemPosition = position;

        holder.mCheckBox.setOnClickListener(new View.OnClickListener() {
            //Context ctx = holder.mTextView.getContext();
            CheckBox checkBox = holder.mCheckBox;

            /**
             * Called when the user clicks on an activity label
             * @param v
             */
            @Override
            public void onClick(View v) {
                itemsListChecks[listItemPosition] = !itemsListChecks[listItemPosition];
            }
        });

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return itemsList.length;
    }
}
