package data.com.datacollector.network;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Util;

import static data.com.datacollector.model.Const.DEVICE_ID;
import static data.com.datacollector.model.Const.HOST_MACHINE_BT_NAME;
import static data.com.datacollector.model.Const.MY_UUID;


/**
 * Created by ROGER on 3/11/2018.
 * TODO: Verify control variables like "is uploading". It might be required to implement this code under NetworkIO.java
 * TODO: Only one thread should be attempting to upload data. Http OR Bluetooth.
 */

public class BluetoothFileTransfer {
    private final String TAG = "DC_BluetoothIO";
    private int DEFAULT_BUFFER_SIZE = 8192;
    private BluetoothDevice device = null;
    private BluetoothSocket btSocket = null;
    private OutputStream out = null;
    private BluetoothAdapter mBluetoothAdapter;

    public BluetoothFileTransfer(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void sendData(final Context context){


        //zip contents of folder holding all BLE files, returns the path to the file
        final File dir = FileUtil.getCollectedDataDir(context, FileUtil.BASE_DIR);
        if (!dir.exists())
            dir.mkdirs();

        File[] dateDirs = dir.listFiles();

        //We assume that if the folder stills here meaning that it has not been deleted, then, we assume
        //it has not yet been transferred because we remove the folders right after the transfer is complete
        for (final File date : dateDirs) {
            FileUtil.fileUploadInProgress = true;

            //Only folders should be in this directory (1 per day of data), delete anything else
            if(!date.isDirectory()) {
                date.delete();
                continue;
            }

            //uploaded zip file includes device id (Serial's last 8 digits) and the date (assumed to be the last part of the filepath)
            String destPath = date.getPath() + "/DC_" + DEVICE_ID + "_" + FileUtil.getLastPathComponent(date.getPath()) + ".zip";

            Log.d(TAG, "sendData: upload from " + date.getPath());

            if (!FileUtil.zipFileAtPath(date.getPath(), destPath)) {
                //if we fail, do not attempt to upload a file, and log an error
                Log.e(TAG, "sendData: Zipping folder failed");
                continue;
            }
            //get the file we just zipped
            File file = new File(destPath);

            Log.d(TAG, "sendData: Attempt to send " + file.getName() + " of size " + FileUtil.fileSize(file) + " bytes");

            if (!mBluetoothAdapter.isEnabled()) {
                //The bluetooth is off. Do not send data.
                Log.d(TAG, "The bluetooth is off. Data transfer cancelled.");
                //TODO: Handle this, should we turn it on?
                FileUtil.lastUploadResult = false;
                FileUtil.fileUploadInProgress = false;
            } else {
                try {
                    String BTServerAddress = getPairedAddress();
                    if(BTServerAddress.isEmpty()){
                        //We couldn't find the server
                        Log.d(TAG, "The bluetooth server device has not been paired. Data transfer cancelled.");
                        FileUtil.lastUploadResult = false;
                        FileUtil.fileUploadInProgress = false;
                    } else {
                        //We found the address, and we are ready to open the socket
                        openSocket(BTServerAddress);
                        send(file);
                        Log.d(TAG, "uploadData:: successful:: Now Removing the data");

                        String PathToDir = date.getPath();

                        //We prevent from removing today's folder so we keep collecting data
                        if (!PathToDir.contains(Util.getDateForDir())) {
                            clearFilesContent(PathToDir);
                        }
                        file.delete();

                        FileUtil.lastUploadResult = true;
                        FileUtil.fileUploadInProgress = false;

                    }
                } catch (IOException e){
                    Log.e(TAG,"There was an error while trying to send data through the BT: " + e.getMessage());
                    e.printStackTrace();
                    FileUtil.lastUploadResult = false;
                    FileUtil.fileUploadInProgress = false;
                    if(btSocket != null) {
                        //Prevents sockets from remaining open
                        try {
                            Log.d(TAG, "sendData: Closing the socket on fallback");
                            // We just make sure we are not trying to close an already closed socket
                            // which has been observed to cause segmentation fault errors
                            if(btSocket.isConnected()){
                                btSocket.close();
                            }
                        } catch (IOException e1) {
                            Log.e(TAG,"There was an error trying to close the socket after IOException " + e1.getMessage());
                            e1.printStackTrace();
                        }
                        btSocket = null;
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "There was an error while creating a timer before closing the socket: " + e.getMessage());
                    e.printStackTrace();
                    FileUtil.lastUploadResult = false;
                    FileUtil.fileUploadInProgress = false;

                }
            }
        }

    }

    /*
     * The machine hosting the Bluetooth Server should previously be paired with the wearable
     * We assume the device will always have a specific name: HOST_MACHINE_BT_NAME
     */
    private String getPairedAddress(){

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        String bluetoothServerMac = "";

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device and look for HOST_MACHINE_BT_NAME
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (deviceName.equals(HOST_MACHINE_BT_NAME)){
                    Log.d(TAG,"Found paired device " + HOST_MACHINE_BT_NAME + " with mac: " + deviceHardwareAddress);
                    bluetoothServerMac = deviceHardwareAddress;
                    break;
                }
            }
        }
        return bluetoothServerMac;
    }

    /*
     * Attempts to open the BT socket for data transfer
     */
    private void openSocket(String btMacAddress) throws IOException{
        device = mBluetoothAdapter.getRemoteDevice(btMacAddress);
        Log.d(TAG, "openSocket: About to create socket");
        btSocket = device.createRfcommSocketToServiceRecord(MY_UUID); //IOException
        Log.d(TAG, "openSocket: Created socket");
        mBluetoothAdapter.cancelDiscovery();
        Log.d(TAG, "openSocket: About to connect socket");
        try {
            btSocket.connect(); //IOException
            Log.d(TAG, "openSocket: Connected");
            out = btSocket.getOutputStream(); //IOException
            Log.d(TAG, "The BT connection was successful");
        } catch(IOException e) {
            Log.e(TAG,"Error connecting socket" + e.getMessage());
            try {
                //TODO: Since even this fallback may fall, set up an alarm that will attempt to send the files again ONLY if stills connected\
                //we could broadcast a message to our receiver and make him launch this again

                Log.e(TAG,"Trying fallback");
                //Solution: https://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3?rq=1
                btSocket =(BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                btSocket.connect();
                Log.d(TAG, "openSocket: Connected");
                out = btSocket.getOutputStream(); //IOException
                Log.d(TAG, "The BT connection was successful");
            } catch (Exception e2) {
                Log.e(TAG, "Couldn't establish Bluetooth connection! " + e2.getMessage());
                throw e;
            }
        }
    }

    /*
     * Sends the data after a successful BT connection
     */
    private void send(File file) throws IOException, InterruptedException{
        Log.d(TAG,"send: Preparing file to send");

        long length = file.length();
        byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
        InputStream in = new FileInputStream(file);
        int count;
        Log.d(TAG, "send: Sending file");
        while ((count = in.read(bytes)) > 0) {
            out.write(bytes, 0, count);
        }

        Log.d(TAG, "send: Waiting a second before closing the socket");

        //TODO: Change this sleep for something more robust
        Thread.sleep(1000);

        in.close();

        /*To avoid a possible segmentation fault that is somethimes caused by closiing a socket twice
        * we fisrt verify the connection is open before clossing it.*/
        if(btSocket.isConnected()){
            out.flush();
            out.close();
        }
        //btSocket.close() This might cause problems since closing the outputstream also closes the socket
    }

    private void clearFilesContent(String path) {
        Log.i(TAG, "clearFilesContent: removing files in: " + path);
        final File file = new File(path);

        //if folder contains a today's date, do not delete its' files
        if(file.getAbsolutePath().contains(Util.getDateForDir())) {
            return;
        }
        //if the file is a directory, then we also need to delete the files under it
        if(file.isDirectory()) {


            File[] fileList = file.listFiles();
            for (File subfile : fileList) {
                //remove all files within directory recursively; also remove subfolders in the process

                //however if the file is from today, do not clear it
                //if(subfile.getAbsolutePath().contains(Util.getDateForDir()))
                //    continue;
                clearFilesContent(subfile.getPath());
            }
            file.delete();  //then delete the directory
        }
        else
            file.delete();

    }
}