package data.com.datacollector.view;

import android.app.Application;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyApplication extends Application {


    /**
     * Called when the application is starting, before any activity, service, or receiver objects (excluding content providers) have been created.
     */
    public void onCreate() {
        super.onCreate();

        //File appDirectory = new File( Environment.getExternalStorageDirectory() + "/MyPersonalAppFolder" );
        File logDirectory = new File(this.getFilesDir().toString(), "/DC/logs/logcat");
        //File logDirectory = new File( appDirectory + "/log" );
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy_MM_dd");
        Date date = new Date();
        String filename = dateFormat.format(date);
        final File logFile = new File( logDirectory, "logcat" + filename + System.currentTimeMillis() + ".txt" );

        if ( !logDirectory.exists() ) {
            logDirectory.mkdirs();
        }

        Process process = null;
        try {
            process = Runtime.getRuntime().exec("logcat -c");
            process = Runtime.getRuntime().exec("logcat -f " + logFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}