package data.com.datacollector.view.diet_annotation_ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import data.com.datacollector.R;

public class RadioButtonsActivity extends WearableActivity {

    private RadioGroup radioGroup;
    public int questionId =0;
    private final String TAG = "DC_DetailsMainActivity2Radio";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio_buttons);

        radioGroup = (RadioGroup) findViewById(R.id.detailsRadioGroup);

        // Enables Always-on
        setAmbientEnabled();

        getExtrasAndSetButtons();
    }

    public void getExtrasAndSetButtons(){
        //TODO: Get extras
        String answers[] = getIntent().getStringArrayExtra("ANSWERS");
        questionId = getIntent().getIntExtra("QUESTION_ID",0);

        for (int i=0; i<answers.length;i++){
            RadioButton rdbtn = new RadioButton(this);
            rdbtn.setId(i);
            rdbtn.setText(answers[i]);
            RadioGroup.LayoutParams lp = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT,RadioGroup.LayoutParams.WRAP_CONTENT);
            rdbtn.setLayoutParams(lp);
            rdbtn.setTextColor(Color.WHITE);
            rdbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRadioClick(v, questionId);
                }
            });
            radioGroup.addView(rdbtn);
        }
    }

    public void onRadioClick(View v, int questionId){
        //TODO: Close, go back, save, and mark
        Log.d(TAG, "onRadioClick: ");
        Intent intent = new Intent("RADIO_ANSWER");
        intent.putExtra("ANSWER_ID",v.getId());
        intent.putExtra("QUESTION_ID",questionId);
        LocalBroadcastManager.getInstance(v.getContext()).sendBroadcast(intent);
        finish();
    }
}
