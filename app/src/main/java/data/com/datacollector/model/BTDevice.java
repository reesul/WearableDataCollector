package data.com.datacollector.model;

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import data.com.datacollector.utility.Util;

/**
 * Model class for making objects of BLE device
 * The object would contain all the required information for BLE
 * Created by Ritu Raj on 9/23/2017.
 */

public class BTDevice implements Serializable {

    private final String TAG = "RawBTD:";

    /* Data members (original 5)*/
    private String name;    //name of BLE device, null if not included in advertisement packet
    private String mac;     //MAC address of device
    private int accessCount;    //number of times a device has been scanned within an interval

    /*  Added from old BLE sniffing project - expansion on Ritu's original data members */
    private ScanRecord scanRecord;  //object retrieved from actual scan
    private byte[] rawScanRecord;  //raw bytes of the advertisement packet
    private int minPeriod;          //lowest time between scans of same device (in milliseconds)
    private long time;              //time of most recent scan

    private List<Integer> rssi = new ArrayList<>();
    private List<String> timeStamp = new ArrayList<>();


    public BTDevice(ScanResult result, long scanTime) {
        scanRecord = result.getScanRecord();

        rawScanRecord = scanRecord.getBytes();
        time = scanTime;

        name = result.getDevice().getName();
        mac = result.getDevice().getAddress();
        minPeriod = Integer.MAX_VALUE;
        accessCount = 1;

        rssi.add(result.getRssi());
        timeStamp.add(Util.getTime(time));

        truncateRawData();
    }


    /* Parses the raw data file and removes data past the end of the scan
        Technically, advertisement packets should be no longer than 31 bytes,
            but some devices do no follow this specification

        Function looks at the size of each subfield, and truncates when it
            reaches a field with length 0 (signifying the end of a packet)
     */
    private void truncateRawData() {
        int index = 0;
        int nextFieldIndex;
        try {
            while (index < rawScanRecord.length) {
                //byte at index should hold length of that field, so that length + 1 gives the next field's length byte
                //if this is 0, the advertisement packet is done, ignore next information
                nextFieldIndex = index + rawScanRecord[index] + 1;
                if (nextFieldIndex >= rawScanRecord.length)
                    break;  //packet is max size, make no changes
                if (rawScanRecord[nextFieldIndex] == 0) {
                    rawScanRecord = Arrays.copyOf(rawScanRecord, nextFieldIndex);
                    break;

                }
                index = nextFieldIndex;
            }
        } catch (IndexOutOfBoundsException e) {
            Log.d(TAG, "Reached end of array; do not modify scan record");
        }
    }

    /*
    Potentially add a device that had a repeat MAC address
        If the device showed previously in the same 500ms BLE scan window, it is ignored
        not ignored if it has been 4500 ms since the last scan of this device

    @param: ScanResult is the object returned by the scan of this device
    */
    public void addRepeatDevice(ScanResult scanResult, long newScanTime) {

        //first find out if new scan is too soon to be relevant (scan within the last 4.5 seconds
        //BLE scans are for 500 ms every 5 seconds, so 0.5 seconds is minimum for device to be in separate scans

        if (newScanTime - time > 500) {
            //if repeat device is valid, add extra entry for rssi and timestamp
            timeStamp.add(Util.getTime(newScanTime));
            rssi.add(scanResult.getRssi());
            //update most recent scan time to be that of the new device
            time = newScanTime;

            //Log.d(TAG, "addRepeatDevice:: new instance of device recorded");
        } //else Log.d(TAG, "addRepeatDevice:: new instance of device too recent to save");
    }

    /*
    toString method for BTDevice. This is used to write the device's data to file
        may contain multiple entries for rssi and timestamp
        format in single line: time:{[Date1], Date2], ...} mac:{[xx:xx:xx:xx:xx:xx]}
            name:{[name or null]} rssi:{[RSSI1],[RSSI2], ...} raw_data:{[0 to ~31 bytes in hex]}
        Date in format MM/dd/yy HH:mm:ss
     */
    public String toString() {

        //String builder more efficient for concatenation of strings
        StringBuilder builder = new StringBuilder("time(s):{" + timeStamp.get(0));


        for (int i = 1; i < timeStamp.size(); i++) {
            builder.append(", ").append(timeStamp.get(i));
        }

        builder.append("} mac:{").append(mac)
                .append("} name:{").append(name)
                .append("} rssi:{").append(rssi.get(0));

        for (int i = 1; i < rssi.size(); i++) {
            builder.append(", ").append(rssi.get(i));
        }

        //if more than 1 scan of single device, add period and number of scans to string
        //if(accessCount>1) {s += " period:{" + minPeriod + "} scans:{" + accessCount+"}";
        //}
        //keep all on same line, and contain data within braces
        builder.append(/*"\r\n" + */  "} raw_data:{").append(Util.scanBytesToHexStr(rawScanRecord, false)).append("}");

        return builder.toString();

    }

    // TODO add GSON for this class

    public ScanRecord getScanRecord() {
        return scanRecord;
    }

    public void setScanRecord(ScanRecord scanRecord) {
        this.scanRecord = scanRecord;
    }

    public byte[] getRawScanRecord() {
        return rawScanRecord;
    }

    public void setRawScanRecord(byte[] rawScanRecord) {
        this.rawScanRecord = rawScanRecord;
    }

    public int getMinPeriod() {
        return minPeriod;
    }

    public void setMinPeriod(int minPeriod) {
        this.minPeriod = minPeriod;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }


/*  timestamp and rssi as single values, deprecated

    //private String timeStamp;   //time of first scan within the interval
    //private int rssi;           //relative signal strength
//    public String getTimeStamp() {
//        return timeStamp;
//    }
//
//    public void setTimeStamp(String timeStamp) {
//        this.timeStamp = timeStamp;
//    }
//
//    public int getRssi() {
//        return rssi;
//    }
//
//    public void setRssi(int rssi) {
//        this.rssi = rssi;
//    }
*/

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(int accessCount) {
        this.accessCount = accessCount;
    }


}
