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
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.support.wear.widget.WearableRecyclerView;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

import data.com.datacollector.R;
import data.com.datacollector.model.ActivitiesList;
import data.com.datacollector.model.Const;
import data.com.datacollector.receiver.DataCollectReceiver;
import data.com.datacollector.service.LeBLEService;
import data.com.datacollector.service.SensorService;
import data.com.datacollector.utility.ActivitiesAdapter;
import data.com.datacollector.utility.CustomizedExceptionHandler;
import data.com.datacollector.utility.Notifications;
import data.com.datacollector.utility.Util;

import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_ALARM_RECEIVED;
import static data.com.datacollector.model.Const.BROADCAST_DATA_SAVE_DATA_AND_STOP;
import static data.com.datacollector.model.Const.SET_LOADING;
import static data.com.datacollector.model.Const.SET_LOADING_HOME_ACTIVITY;
import static data.com.datacollector.model.Const.START_SERVICES;
import static data.com.datacollector.model.Const.STOP_SERVICES;

/**
 * Application's Home activity. This is also the launcher activity for the application
 */
public class HomeActivity extends WearableActivity {
    private final String TAG = "DC_HomeActivity";
    private Button btnStartStop;
    private Button btnRecord;
//    private WearableRecyclerView recActivitiesList;
    private FrameLayout progressBar;

    //Audio recording
    private int bufferSize = 640;
    private byte[] audioBuffer = new byte[bufferSize];
    private AudioRecord audioRecord = null;
    private boolean isAudioRecording = false;
    private Thread audioRecordThread = null;
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;
    private static final int SAMPLE_RATE = 8000; // Hz
    private static final int ENCODING = AudioFormat.ENCODING_PCM_8BIT;
    private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;


    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BODY_SENSOR = 2;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL = 3;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO= 4;
    private final int CONFIRMATIONS_EXPECTED = 2; //The numbers of services we are waiting for
    private int confirmationsReceived = 0; //The number of confirmations received so far
    private int previousEvent = MotionEvent.ACTION_UP;

    private ActivitiesList activities;
    private ActivitiesAdapter adapterList;

    private BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mStopServicesReceiver onReceive: called");
            String action = intent.getAction();

            //Action that tells us to stop our services
            if(action.equals(BROADCAST_DATA_SAVE_DATA_AND_STOP)){

                Log.d(TAG, "onReceive: Received BROADCAST_DATA_SAVE_DATA_AND_STOP confirmation");
                confirmationsReceived++;

                if(confirmationsReceived >= CONFIRMATIONS_EXPECTED){
                    Log.d(TAG, "onReceive: Safe to restart, everything has been saved");
                    //It is safe to stop the activity
                    confirmationsReceived = 0;

                    //A previous alarm issued a restart request. Once we have received confirmation
                    //that all the data has been saved, we now force the activity to restart
                    stopBgService();
                }

            } else if(action.equals(SET_LOADING_HOME_ACTIVITY)) {
                Log.d(TAG, "onReceive: Received SET_LOADING_HOME_ACTIVITY confirmation");
                setLoading(intent.getBooleanExtra(SET_LOADING,false));
            } else if(action.equals(START_SERVICES)){
                Log.d(TAG, "onReceive: Requested to start services");
                //NOTE: It is safer to not use handleStartStopBtnClick(); because this might turn off the device if is on, we only want to start it.
                if(!LeBLEService.isServiceRunning || !SensorService.isServiceRunning){
                    startBgService();
                    confirmationsReceived = 0;
                    btnStartStop.setText("STOP");
                }
            } else if(action.equals(STOP_SERVICES)){
                Log.d(TAG, "onReceive: Requested to stop services");
                if(LeBLEService.isServiceRunning && SensorService.isServiceRunning) {
                    requestSaveBeforeStop();
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: called");

        //Registering a local broadcast receiver to listen for data save confirmation
        IntentFilter confirmationIntent = new IntentFilter();
        confirmationIntent.addAction(BROADCAST_DATA_SAVE_DATA_AND_STOP);
        confirmationIntent.addAction(SET_LOADING_HOME_ACTIVITY);
        confirmationIntent.addAction(START_SERVICES);
        confirmationIntent.addAction(STOP_SERVICES);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalReceiver, confirmationIntent);

        //Custom uncaught exception handling
        Thread.setDefaultUncaughtExceptionHandler(new CustomizedExceptionHandler(Environment.getExternalStorageDirectory().getAbsolutePath()));
        //Finish custom

        setContentView(R.layout.activity_main);
        requestPermission();

        //This might be accomplished while the permission is being granted
        if(this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            initAudioRecord();
        }

        initView();

        Log.d(TAG, "ID is " + Const.DEVICE_ID);
        setAmbientEnabled();
    }

    public void initAudioRecord(){
        audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, bufferSize);
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
        btnRecord = (Button) findViewById(R.id.btnRecord);
        setBtnStartStopText();
        setBtnStartStopRecordText();


        //initialize the list that holds all of the labels
//        recActivitiesList =  findViewById((R.id.recycler_activities));
//        //recActivitiesList.setCircularScrollingGestureEnabled(true); Consider if this might be easier for the user
//        recActivitiesList.setLayoutManager(new WearableLinearLayoutManager((this)));
//        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recActivitiesList.getContext(), DividerItemDecoration.VERTICAL);
//        recActivitiesList.addItemDecoration(dividerItemDecoration);
//
//        //Get activities list
//        activities = new ActivitiesList();
//        //Set up recycler view adapter with the obtained list
//        adapterList = new ActivitiesAdapter(activities.getList());
//        recActivitiesList.setAdapter(adapterList);
//
//        //We intercept multiple touches and prevent any other after the first one arrives
//        recActivitiesList.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
//            @Override
//            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
//                // return
//                // true: consume touch event
//                // false: dispatch touch event
//
//                View childView = rv.findChildViewUnder(e.getX(), e.getY());
//
//                //It could be the case on which the user touches in a portion of the UI where there is NO children. That's why our ui was being froze
//                //therefore we first verify we are touching on a child
//                if(childView != null){
//
//                    //We first determine if we touched the label and not only moved
//                    if(previousEvent == MotionEvent.ACTION_DOWN && e.getAction() == MotionEvent.ACTION_UP) {
//                        setLoading(true);// This prevents user from submitting multiple labels when touching quickly
//                    }
//                    //else ignore
//                    previousEvent = e.getAction();
//                }
//
//                return false;
//            }
//        });

        progressBar = findViewById(R.id.progressBar);

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

    private void setBtnStartStopRecordText(){
        if(btnRecord == null)
            return;

        if(isAudioRecording){
            btnRecord.setText("STOP RECORDING");
        }else{
            btnRecord.setText("START RECORDING");
        }
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

        if(this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs permission to write data");
            builder.setMessage("Please grant write permission to external storage");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL);
                }
            });
            builder.show();
        }

        if(this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs permission to record");
            builder.setMessage("Please grant recording permission");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
                }
            });
            builder.show();
        }

        //TODO if DEVICE_ID stops working due to updates, add permission for READ_PHONE_STATE here so we can get hardware serial number

    }

    public void onClickRecording(View v){

        if(audioRecord == null){
            Log.d(TAG, "onClickRecording: Setting up audioRecord");
            initAudioRecord();
        }

        if(audioRecord == null){
            Toast.makeText(this, "Error. Close the app and try again", Toast.LENGTH_SHORT).show();
        }else{
            if(!isAudioRecording){
                Log.d(TAG, "onClickRecording: Starting audio recording");
                FileOutputStream wavOut = null;
                isAudioRecording = true;
                btnRecord.setEnabled(false);

                audioRecord.startRecording();
                audioRecordThread = new Thread()
                {
                    public void run()
                    {
                        while(isAudioRecording){
                            int readBufferSize = audioRecord.read(audioBuffer, 0, bufferSize);
                            Log.d(TAG, "run: READING");
//                        for(int i = 0; i < readBufferSize; i++){
//                            dataOutputStream.writeShort(audioBuffer[i]);
//                        }
                        }
                    }
                };

                audioRecordThread.setPriority(Thread.MAX_PRIORITY);
                audioRecordThread.start();
                //String timestamp = Util.getTimeMillis(System.currentTimeMillis());
                //File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"+"recordsound");


                btnRecord.setText("STOP RECORDING");
                btnRecord.setEnabled(true);

            }else{
                Log.d(TAG, "onClickRecording: Stopping audio recording");
                isAudioRecording = false;
                btnRecord.setText("START RECORDING");
                btnRecord.setEnabled(false);
                audioRecord.stop();
                audioRecordThread.interrupt();
                audioRecordThread = null;
                btnRecord.setEnabled(true);
            }
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
     * Stop all background service once the data has been saved
     */
    public void stopBgService(){

        stopService(new Intent(this, SensorService.class));
        stopService(new Intent(this, LeBLEService.class));
        btnStartStop.setText("START");
        //btnStartStop.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_circle) );
        setLoading(false);
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
        setLoading(true);
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
        setLoading(false);
        //TODO: This should be uncomment whenever we are using the feedback feature (Notifications.requestFeedback)
        //Notifications.openFeedbackIfNotificationActive(HomeActivity.this.getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called");
    }

    public void setLoading(boolean b){
        if(b){
            Log.d(TAG, "setLoading: true");
            btnStartStop.setVisibility(View.GONE);
//            recActivitiesList.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }else{
            Log.d(TAG, "setLoading: false");
            btnStartStop.setVisibility(View.VISIBLE);
//            recActivitiesList.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }



}
