package data.com.datacollector.utility;

import android.content.Context;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import data.com.datacollector.model.Const;

/**
 * For General Utility. It will have all the genral utility functions to facilitate
 * all modules across the application to use the utility functions.
 * Created by ritu on 10/29/17.
 */

public class Util {
    private static final String TAG = "DC_Util";

    /**
     * return time in format MM/dd/yy HH:mm:ss
     */
    public static String getTime(long time){
        Date date = new Date(time);
        DateFormat formatter = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }

    public static String getTimeMillis(long time) {
        Date date = new Date(time);
        DateFormat formatter = new SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }

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


}
