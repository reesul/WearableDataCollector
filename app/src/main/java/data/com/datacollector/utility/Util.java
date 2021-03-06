package data.com.datacollector.utility;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import data.com.datacollector.model.Const;

import static android.content.Context.SENSOR_SERVICE;

/**
 * For General Utility. It will have all the genral utility functions to facilitate
 * all modules across the application to use the utility functions.
 * Created by ritu on 10/29/17.
 */

public class Util {
    private static final String TAG = "DC_Util";

     /*
        startTime is used as a reference for sensor event's timestamps
        This is a long corresponding the system time that the watch was turned on at
        timestamps from events should be added to this value to get the realtime version of said timestamp
     */
    private static long startTime;

    public static long getStartTime() {
        return startTime;
    }

    public static void setStartTime(long startTime) {
        if(startTime != 0) {
            Util.startTime = startTime;
        }
    }

    /**
     * return time in format MM/dd/yy HH:mm:ss
     */
    public static String getTime(long time){
        Date date = new Date(time);
        DateFormat formatter = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }

    /**
     * Gets an absolute timestamp to use for calculating date
     * @param time
     * @return
     */
    public static String getTimeMillis(long time) {
        Date date = new Date(time);
        DateFormat formatter = new SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }

    /**
     * Converts a string timestamp back into the number date
     * @param timeStamp
     * @return
     */
    public static long getMillisFromDate(String timeStamp) {
        String expectedPattern = "MM/dd/yy HH:mm:ss";
        SimpleDateFormat format = new SimpleDateFormat(expectedPattern);
        try {
            Date date = format.parse(timeStamp);
            return date.getTime();
        } catch (ParseException e) {}
        return 0;
    }

    //used for folder name; contains date only
    public static String getDateForDir() {
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        DateFormat formatter = new SimpleDateFormat("MM-dd-yy");
        String dateFormatted = formatter.format(date);
        //Log.d(TAG, dateFormatted);
        return dateFormatted;
    }

    //used for name of a file; contains no slashes or whitespace
    public static String getTimeForFileName(long time) {
        Date date = new Date(time);
        DateFormat formatter = new SimpleDateFormat("MM-dd-yy_HH:mm:ss");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }

    /*
 @param in: byte array to be converted into a string of hexadecimal digits
 @param readable: inserts a space between each byte to make it easier to evaulate; used mainly for logging purposes
*/
    public static String scanBytesToHexStr(byte[] in, boolean readable) {
        final StringBuilder builder = new StringBuilder();
        int count = 0;
        for(byte b : in) {
            //only 31 bytes of the packet have any meaning, as this is the max size of the advertising payload
            //the rest would be part of the scan response, which we don't care about
            if(/*count>31 ||*/ count > (in.length-1))
                break;
            count++;
            builder.append(String.format("%02X",b));
            if(readable) builder.append(" ");

        }
        return builder.toString();
    }

    /*
    Remove spaces from arrays in a list
        Use case is saving activity labels to file without white space
     */
    public static String[] removeSpaces(String[] array) {
        String[] newArray = new String[array.length];

        for(int i = 0; i < array.length; i++) {
            newArray[i] = array[i].replace(" ", "");

        }

        return newArray;
    }

    /**
     * Use sensorManager and event listener to correlate the timestamp the IMU reports with the date timestamp
     *  These two differ; IMU (sensor) reports timestamp in ns since device was powered on.
     */
    private static SensorManager manager;
    private static SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long realtime = System.currentTimeMillis();
            long timestamp = event.timestamp;
            //convert to milliseconds
            timestamp /= Const.NANOS_TO_MILLIS;

            Log.d(TAG, "initTimeStamps: sensor read");

            setStartTime(realtime-timestamp);
            Log.d(TAG, "Current time is " + System.currentTimeMillis() + "\tSystem start time is " + startTime);

            manager.unregisterListener(this);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    //This function sets a value for startTime, used as a reference for timestamps in the future
    //Take a single value from a sensor, and use its timestampf ro synchronizing
    public static void initTimeStamps(Context context) {
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor acc = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        manager.registerListener(listener, acc, SensorManager.SENSOR_DELAY_FASTEST);
    }

}
