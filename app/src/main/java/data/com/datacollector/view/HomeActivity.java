package data.com.datacollector.view;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.support.wear.widget.WearableRecyclerView;
import android.widget.ProgressBar;

import data.com.datacollector.R;
import data.com.datacollector.model.ActivitiesList;
import data.com.datacollector.model.Const;
import data.com.datacollector.receiver.DataCollectReceiver;
import data.com.datacollector.service.LeBLEService;
import data.com.datacollector.service.SensorService;
import data.com.datacollector.utility.ActivitiesAdapter;
import data.com.datacollector.utility.CustomizedExceptionHandler;

import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_ALARM_RECEIVED;
import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_DATA_AND_STOP;

/**
 * Application's Home activity. This is also the launcher activity for the application
 */
public class HomeActivity extends WearableActivity {
    private final String TAG = "DC_HomeActivity";
    private Button btnStartStop;
    private WearableRecyclerView recActivitiesList;
    private ProgressBar progressBar;

    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BODY_SENSOR = 2;
    private static final int PERMISSION_READ_PHONE_STATE = 3;
    private final int CONFIRMATIONS_EXPECTED = 2; //The numbers of services we are waiting for
    private int confirmationsReceived = 0; //The number of confirmations received so far

    private ActivitiesList activities;
    private ActivitiesAdapter adapterList;

    private BroadcastReceiver mStopServicesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mStopServicesReceiver onReceive: called");
            String action = intent.getAction();

            //Action that tells us to stop our services
            if(action.equals(BROADCAST_DATA_SAVE_DATA_AND_STOP)){

                Log.d(TAG, "onReceive: Received confirmation");
                confirmationsReceived++;

                if(confirmationsReceived >= CONFIRMATIONS_EXPECTED){
                    Log.d(TAG, "onReceive: Safe to restart, everything has been saved");
                    //It is safe to stop the activity
                    confirmationsReceived = 0;

                    //A previous alarm issued a restart request. Once we have received confirmation
                    //that all the data has been saved, we now force the activity to restart
                    stopBgService();
                }

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: called");

        //Registering a local broadcast receiver to listen for data save confirmation
        IntentFilter confirmationIntent = new IntentFilter(BROADCAST_DATA_SAVE_DATA_AND_STOP);
        LocalBroadcastManager.getInstance(this).registerReceiver(mStopServicesReceiver, confirmationIntent);

        //Custom uncaught exception handling
        Thread.setDefaultUncaughtExceptionHandler(new CustomizedExceptionHandler(this.getFilesDir().toString()));
        //Finish custom

        setContentView(R.layout.activity_main);
        requestPermission();
        initView();

        Log.d(TAG, "ID is " + Const.DEVICE_ID);
        setAmbientEnabled();
    }

    /**
     * initialize all views related to the application.
     */
    private void initView(){


        //initialize the button that starts and stop the service
        btnStartStop = (Button) findViewById(R.id.btn_start);
        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleStartStopBtnClick();
            }
        });
        setBtnStartStopText();


        //initialize the list that holds all of the labels
        recActivitiesList =  findViewById((R.id.recycler_activities));
        //recActivitiesList.setCircularScrollingGestureEnabled(true); Consider if this might be easier for the user
        recActivitiesList.setLayoutManager(new WearableLinearLayoutManager((this)));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recActivitiesList.getContext(), DividerItemDecoration.VERTICAL);
        recActivitiesList.addItemDecoration(dividerItemDecoration);

        //Get activities list
        activities = new ActivitiesList();
        //Set up recycler view adapter with the obtained list
        adapterList = new ActivitiesAdapter(activities.getList());
        recActivitiesList.setAdapter(adapterList);

        progressBar = findViewById(R.id.progressBar);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

    }

    /**
     * Depending on current status of running service, update text on the button
     * to help the user understand what action can be performed currently.
     */
    private void setBtnStartStopText(){
        if(btnStartStop == null)
            return;

        //TODO: reenable check for sensor service (IMU) once BLE working
        if(LeBLEService.isServiceRunning ||  SensorService.isServiceRunning){
            btnStartStop.setText("STOP");
            //btnStartStop.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_red_circle) );
        }else{
            btnStartStop.setText("START");
            //btnStartStop.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_circle) );
        }

        //while making changes an trying to add a list
        //btnStartStop.setText("null, test");

    }

    /**
     * Called when Start/Stop button is clicked to trigger start/stop of service
     * for data collection
     */
    private void handleStartStopBtnClick(){
        //TODO: reenable check for sensor service (IMU) once BLE working

        if(!LeBLEService.isServiceRunning || !SensorService.isServiceRunning){
            startBgService();
            confirmationsReceived = 0;
            btnStartStop.setText("STOP");
            //btnStartStop.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_red_circle) );
        }else{
            requestSaveBeforeStop();
        }
    }

    /**
     * Start Sensor Service to collect data related to Acceleromter, Gyroscope and Heart Rate
     */
    private void startSensorService(){
        startForegroundService(new Intent(this, SensorService.class));
    }

    /**
     * start BLe data collection service in the background.
     */
    private void startBLEService(){
        startForegroundService(new Intent(this, LeBLEService.class));
    }

    /**
     * Request permission for COARSE_LOCATION
     */
    private void requestPermission(){
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        if(this.checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs sensor access ");
            builder.setMessage("Please grant sensor access so this app can detect Heart rate using PPG");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, PERMISSION_REQUEST_BODY_SENSOR);
                }
            });
            builder.show();
        }

        //TODO if DEVICE_ID stops working due to updates, add permission for READ_PHONE_STATE here so we can get hardware serial number

    }


    /**
     * Start all background services to collect data
     */
    private void startBgService(){
        Log.d(TAG, "startBgService::");
        startSensorService();
        startBLEService();
    }

    /**
     * Stop all background service once the data has been saved
     */
    public void stopBgService(){

        stopService(new Intent(this, SensorService.class));
        stopService(new Intent(this, LeBLEService.class));
        btnStartStop.setText("START");
        //btnStartStop.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_circle) );
        btnStartStop.setEnabled(true);
        progressBar.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    /**
     * Requests the broadcast to issue a saving event
     */
    private void requestSaveBeforeStop(){

        //Before stopping we need to make sure that the data on memory buffer is saved to files
        Log.d(TAG, "forceDataSave: Forcing the app to save data before shutting down");
        //Send a special request to the receiver
        Intent intent = new Intent(this, DataCollectReceiver.class);
        intent.putExtra(BROADCAST_DATA_SAVE_ALARM_RECEIVED, true);
        intent.putExtra(BROADCAST_DATA_SAVE_DATA_AND_STOP, true);

        //We send the broadcast to the receiver that coordinates the services to save their data
        HomeActivity.this.sendBroadcast(intent);
        btnStartStop.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called");
    }

}
