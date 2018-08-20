package data.com.datacollector.network;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;

import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Util;

import static data.com.datacollector.model.Const.DEVICE_ID;
import static data.com.datacollector.model.Const.HOST_MACHINE_BT_NAME;
import static data.com.datacollector.model.Const.MY_UUID;


/**
 * Created by ROGER on 3/11/2018.
 */

public class BluetoothFileTransfer {
    private final String TAG = "BluetoothFileTransfer";
    private int DEFAULT_BUFFER_SIZE = 8192;
    private BluetoothDevice device = null;
    private BluetoothSocket btSocket = null;
    private OutputStream out = null;
    private InputStream socketInputStream = null;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean IS_THE_OTHER_STREAM_READY = false; //Used to determine when to close the sockets

    public BluetoothFileTransfer(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    public void sendData(final Context context) throws Exception{

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
            if(!mBluetoothAdapter.isEnabled()){
                Log.d(TAG, "sendData: Enabling BT");
                mBluetoothAdapter.enable();
            }

            if (!mBluetoothAdapter.isEnabled()) {
                //The bluetooth is off. Do not send data.
                Log.d(TAG, "The bluetooth is off and we could not turn it on. Data transfer cancelled.");
                FileUtil.lastUploadResult = false;
                FileUtil.fileUploadInProgress = false;
                Exception btOff = new Exception("BT is off");
                throw btOff;
            } else {
                try {
                    String BTServerAddress = getPairedAddress();
                    if(BTServerAddress.isEmpty()){
                        //We couldn't find the server
                        Log.d(TAG, "The bluetooth server device has not been paired. Data transfer cancelled.");
                        FileUtil.lastUploadResult = false;
                        FileUtil.fileUploadInProgress = false;
                        Exception deviceNotPaired = new Exception("Device is not paired");
                        throw deviceNotPaired; //This is captured by the Asynctask who called this method
                    } else {
                        //We found the address, and we are ready to open the socket
                        openSocket(context, BTServerAddress);
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
                    closeSocket();
                    throw e;
                } catch (InterruptedException e) {
                    Log.e(TAG, "There was an error while creating a timer before closing the socket: " + e.getMessage());
                    e.printStackTrace();
                    FileUtil.lastUploadResult = false;
                    FileUtil.fileUploadInProgress = false;
                    closeSocket();
                    throw e;
                }

            }
        }

    }

    private void closeSocket(){
        Log.d(TAG, "closeSocket: Closing the socket");
        if(btSocket != null) {
            //Prevents sockets from remaining open
            try {
                //Log.d(TAG, "sendData: Closing the socket on fallback");
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
    private void openSocket(Context context, String btMacAddress) throws IOException{
        device = mBluetoothAdapter.getRemoteDevice(btMacAddress);
        IS_THE_OTHER_STREAM_READY = false;
        Log.d(TAG, "openSocket: About to create socket");
        btSocket = device.createRfcommSocketToServiceRecord(MY_UUID); //IOException
        Log.d(TAG, "openSocket: Created socket");
        mBluetoothAdapter.cancelDiscovery();
        Log.d(TAG, "openSocket: About to connect socket");

        InputStreamThread iss = null;
        try {
            btSocket.connect(); //IOException
            Log.d(TAG, "openSocket: Connected");
            out = btSocket.getOutputStream(); //IOException
            socketInputStream = btSocket.getInputStream();
            iss = new InputStreamThread(context, socketInputStream); //For receiving the model
            iss.start();
            Log.d(TAG, "The BT connection was successful");
        } catch(IOException e) {
            Log.e(TAG,"Error connecting socket" + e.getMessage());
            try {
                //we could broadcast a message to our receiver and make him launch this again

                Log.e(TAG,"Trying fallback");
                //Solution: https://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3?rq=1
                btSocket =(BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                btSocket.connect();
                Log.d(TAG, "openSocket: Connected");
                out = btSocket.getOutputStream(); //IOException
                socketInputStream = btSocket.getInputStream();
                iss = new InputStreamThread(context, socketInputStream);
                iss.start();
                Log.d(TAG, "The BT connection was successful");
            } catch (Exception e2) {
                Log.e(TAG, "Couldn't establish Bluetooth connection! " + e2.getMessage());
                throw e;
            }
        }
    }

    private class InputStreamThread extends Thread  {
        private InputStream inputStream;
        private Context context;

        public InputStreamThread(Context context, InputStream inputStream) {
            this.inputStream = inputStream;
            this.context = context;
        }

        @Override
        public void run() {
            try {
                Log.d(TAG, "InputStreamThread: Waiting for data...");
                final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCmodels");
                final File fileModel = new File(dir, "latest.txt");

                if(!fileModel.exists()) {
                    try {
                        fileModel.getParentFile().mkdirs();
                        fileModel.createNewFile();
                    }catch(IOException e){}
                }

                FileOutputStream fos = new FileOutputStream(fileModel, false); //Overwrite the file

                byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
                int count;

                while ((count = inputStream.read(buf)) > 0) {
                    Log.d(TAG, "InputStreamThread: Received " + String.valueOf(count));
                    byte[] msg = Arrays.copyOfRange(buf, 0, count);
                    String message = new String(msg, "UTF-8");

                    // Since we cannot close the stream in order to stop the while .read since the
                    // other stream might still be used, we send a message to stop the while
                    if(message.equals("STOP_MODEL_TRANSFER")) {
                        Log.d(TAG, "InputStreamThread: Stop receiving model information");
                        break;
                    }

                    Log.d(TAG, "InputStreamThread: Received: " + message );
                    fos.write(buf,0,count);//Writing to our model file
                }

                //NOTE: It is important to notice that when the model is not available in the nuc, it will
                //send null and null will be saved on the file, therefore, validation on the file content
                //should be performed before using its information
                fos.close();
                Log.d(TAG, "InputStreamThread: The model has been received");
                closeSocketAfterStreams();
            }catch (Exception e){
                Log.d(TAG, "InputStreamThread: Error getting data from input stream NUC " + e.getMessage());
                e.printStackTrace();
                closeSocket(); //If closing the socket after stream fails. This might close the socket while we are receiving
                //  data from the nuc but its an expected behavior since we expect all the data to be transferred successfully
            }
        }

    }

    /*
     * Sends the data after a successful BT connection
     */
    private void send(File file) throws IOException, InterruptedException{
        Log.d(TAG,"send: Preparing file to send");

        byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
        InputStream in = new FileInputStream(file);
        int count;
        Log.d(TAG, "send: Sending file");
        while ((count = in.read(bytes)) > 0) {
            out.write(bytes, 0, count);
        }

        in.close();
        closeSocketAfterStreams();
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

    /**
     * Waits until both the input stream and output stream have finished to close the socket
     */
    public void closeSocketAfterStreams() throws IOException, InterruptedException{
        if (IS_THE_OTHER_STREAM_READY){
            Log.d(TAG, "closeSocketAfterStreams: All the streams have finished");
            //A stream has finished, therefore, we should close this
            if (btSocket.isConnected()) {
                Log.d(TAG, "closeSocketAfterStreams: Waiting a second before closing the socket");
                // We wait for a second just to make sure everything its been written. This shouldn't be
                // necessary since the flush handles this, however, sometimes id did not handle it properly
                // therefore, this second wait is just in case
                Thread.sleep(1000);
                //This ensures everything is written before closing
                out.flush();
                btSocket.close(); //This should close all the streams
                Log.d(TAG, "closeSocketAfterStreams: The socket has been closed");
            }
        }else{
            Log.d(TAG, "closeSocketAfterStreams: I finished but the other has not finished. IS_THE_OTHER_STREAM_READY=true");
            IS_THE_OTHER_STREAM_READY = true;
        }

    }
}