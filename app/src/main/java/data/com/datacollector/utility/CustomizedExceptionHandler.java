package data.com.datacollector.utility;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.util.Log;

/**
 * Handles any uncaught exception in our APP and reports it in a file
 * Inspired by: https://doandroid.wordpress.com/2011/11/19/writing-crash-reports-into-device-sd-card/
 */
public class CustomizedExceptionHandler implements UncaughtExceptionHandler {
    String TAG = "CustomizedExceptionHandler";

    private UncaughtExceptionHandler defaultUEH;
    private String localPath;

    public CustomizedExceptionHandler(String localPath) {
        this.localPath = localPath;
        //Getting the the default exception handler
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread t, Throwable e) {

        //Write a printable representation of this Throwable
        //The StringWriter gives the lock used to synchronize access to this writer.
        final Writer stringBuffSync = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringBuffSync);
        e.printStackTrace(printWriter);
        String stacktrace = stringBuffSync.toString();
        printWriter.close();


        if (localPath != null) {
            writeToFile(stacktrace);
        }

        //Used only to prevent from any code getting executed.
        defaultUEH.uncaughtException(t, e);
    }

    private void writeToFile(String currentStacktrace) {
        try {
            File dir = new File(this.localPath, "/DC/logs/crashes");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
            Date date = new Date();
            String filename = dateFormat.format(date) + ".STACKTRACE";

            // Write the file into the folder
            File reportFile = new File(dir, filename);
            FileWriter fileWriter = new FileWriter(reportFile);
            fileWriter.append(currentStacktrace);
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            Log.e(TAG, "Error saving the file: " + e.getMessage());
        }
    }

}