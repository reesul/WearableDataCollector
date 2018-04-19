package data.com.datacollector.view;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.support.wear.widget.WearableRecyclerView;

import data.com.datacollector.R;
import data.com.datacollector.model.ActivitiesList;
import data.com.datacollector.model.Const;
import data.com.datacollector.service.LeBLEService;
import data.com.datacollector.service.SensorService;
import data.com.datacollector.utility.ActivitiesAdapter;
import data.com.datacollector.utility.CustomizedExceptionHandler;
import data.com.datacollector.utility.Util;

/**
 * Application's Home activity. This is also the launcher activity for the application
 */
public class HomeActivity extends Activity   {
    private final String TAG = "DC_HomeActivity";
    private Button btnStartStop;
    private WearableRecyclerView recActivitiesList;

    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BODY_SENSOR = 2;

    private ActivitiesList activities;
    private ActivitiesAdapter adapterList;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: called");

        //Custom uncaugh exception handling
        Thread.setDefaultUncaughtExceptionHandler(new CustomizedExceptionHandler(this.getFilesDir().toString()));
        //Finish custom

        setContentView(R.layout.activity_main);
        initView();
        requestPermission();

        //startBgService();
        Util.initTimeStamps(this);

        Log.d(TAG, "ID is " + Const.DEVICE_ID);
    }

    /**
     * initialize all views realted to the application.
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

        //Get activites list
        activities = new ActivitiesList();
        //Set up recycler view adapter with the obtained list
        adapterList = new ActivitiesAdapter(activities.getList());
        recActivitiesList.setAdapter(adapterList);

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
        }else{
            btnStartStop.setText("START");
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
            btnStartStop.setText("STOP");
        }else{
            stopBgService();
            btnStartStop.setText("START");
        }
    }

    /**
     * Start Sensor Service to collect data related to Acceleromter, Gyroscope and Heart Rate
     */
    private void startSensorService(){
        startService(new Intent(this, SensorService.class));
    }

    /**
     * start BLe data collection service in the background.
     */
    private void startBLEService(){
        startService(new Intent(this, LeBLEService.class));
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
     * Stop all background service
     */
    private void stopBgService(){
        stopService(new Intent(this, SensorService.class));
        stopService(new Intent(this, LeBLEService.class));
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
