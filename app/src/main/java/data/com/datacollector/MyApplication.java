package data.com.datacollector;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import data.com.datacollector.receiver.DataCollectReceiver;


/**
 * This gets called whenever the application is created which makes our listener available during the
 * life time of our application. We have tested this and stills working even after the activity crashes
 * and the sticky property is used by the system if the services were running before the crash.
 * This won't work if the activity crashes and the services were not running
 */
public class MyApplication extends Application {
    private DataCollectReceiver mReceiver;
    private String TAG = "MyApplication";
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        mReceiver = new DataCollectReceiver();
        IntentFilter filter = new IntentFilter();

        //Triggered when the watch is connected to power
        filter.addAction(Intent.ACTION_POWER_CONNECTED);

        registerReceiver(mReceiver, filter);

        Log.d(TAG, "onCreate: Registered receiver");
    }
}
