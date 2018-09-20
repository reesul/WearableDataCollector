package data.com.datacollector.utility;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import data.com.datacollector.model.BTDevice;
import data.com.datacollector.model.PPGData;
import data.com.datacollector.model.SensorData;
import data.com.datacollector.service.BaseExternalSensorService;

import static data.com.datacollector.model.Const.DEVICE_ID;
import static data.com.datacollector.model.Const.FILE_NAME_ACCELEROMETER;
import static data.com.datacollector.model.Const.FILE_NAME_BLE;
import static data.com.datacollector.model.Const.FILE_NAME_FEEDBACK;
import static data.com.datacollector.model.Const.FILE_NAME_GYROSCOPE;
import static data.com.datacollector.model.Const.FILE_NAME_PPG;
import static data.com.datacollector.model.Const.FILE_NAME_ACTIVITY;
import static data.com.datacollector.model.Const.FILE_NAME_PREDICTIONS;

/**
 * Utility to handle all operations related to saving and accessing files stored in memory.
 * Created by ritu on 10/29/17.
 */

public class FileUtil {
    private static final String TAG = "DC_FileUtil";

    private static boolean bleFileAlreadyExists;
    private static boolean accFileAlreadyExists;
    private static boolean gyroFileAlreadyExists;
    private static boolean ppgFileAlreadyExists;
    private static boolean actFileAlreadyExists;
    private static boolean feedbackFileAlreadyExists;
    private static boolean predictionsFileAlreadyExists;

    //Used to be in NetworkIO. Now here since it can be set by any of the transfer methods
    //Set by either NetworkIO or BluetoothFileTransfer
    public static boolean fileUploadInProgress = false;
    public static boolean lastUploadResult = true;

    /**
     * called by SensorService to store data of Accelerometer and Gyroscope Sensors to File in device memory
     * @param context : context of the caller
     * @param listAccelData : list of Accelerometer Data
     * @param listGyroData: list of Gyroscopre Data
     */
    public static synchronized void saveGyroNAcceleroDataToFile(Context context, List<SensorData> listAccelData, List<SensorData> listGyroData){
        if(fileUploadInProgress){
            Log.d(TAG, "saveDataToFile:: fileUploadInProgress, will save data in the next call");
            return;
        }

        //final File dir = new File(context.getFilesDir() + "/DC/");
        //dir.mkdirs();
        final File fileAccel = getAccelerometerFile(context);
        final File fileGyro = getGyroScopeFile(context);
        Log.d(TAG, "saveDataToFile::  absolute path: fileAccel: "+fileAccel.getAbsolutePath());
        Log.d(TAG, "saveDataToFile::  absolute path: fileGyro: "+fileGyro.getAbsolutePath());

        boolean fileAccelExists = fileAccel.exists();
        boolean fileGyroExists = fileGyro.exists();
        try {
            if(!fileAccelExists) {
                fileAccel.getParentFile().mkdirs();
                fileAccel.createNewFile();
            }
        }catch(Exception e){}

        try {
            if(!fileGyroExists) {
                fileGyro.getParentFile().mkdirs();
                fileGyro.createNewFile();
            }
        }catch(Exception e){}


        //first write ACC data to file
        if(listAccelData.size() == 0){
            Log.d(TAG, "saveDataToFile:: No Accelerometer data to save");
            //check if file is empty, if so, delete it
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileAccel));
                if(br.readLine() == null)
                    fileAccel.delete();
            } catch (Exception e) { Log.e(TAG, "saveAccelToFile:: Error deleting empty file",e); }


        } else {
             try {
                FileOutputStream fos = new FileOutputStream(fileAccel, true);

                if(!accFileAlreadyExists) {
                    fos.write(DEVICE_ID.getBytes());
                }
                fos.write("\r\n".getBytes());

                for (int i = 0; i < listAccelData.size() ; i++) {
                    //Allocates unnecessary memory. do not use
                    //SensorData sensorData = listAccelData.get(i);

                    fos.write((listAccelData.get(i).getTimestamp() + "," + listAccelData.get(i).getX() + "," + listAccelData.get(i).getY() + "," + listAccelData.get(i).getZ()).getBytes());
                    if (i != listAccelData.size() - 1)
                        fos.write("\r\n".getBytes());
                }
                fos.close();
                Log.d(TAG, "saveDataToFile:: accelerometer data saved successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(listGyroData.size() == 0){
            Log.d(TAG, "saveDataToFile:: No Gyro data to save");
            //check if file is empty: if so, delete it
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileGyro));
                if(br.readLine() == null)
                    fileGyro.delete();
            } catch (Exception e) { Log.e(TAG, "saveGyroToFile:: Error",e); }
        }else {
            try {
                FileOutputStream fos = new FileOutputStream(fileGyro, true);

                if(!gyroFileAlreadyExists) {
                    fos.write(DEVICE_ID.getBytes());
                }

                fos.write("\r\n".getBytes());
                for (int i = 0; i < listGyroData.size() ; i++) {
                    //SensorData sensorData = listGyroData.get(i);

                    fos.write((listGyroData.get(i).getTimestamp() + "," + listGyroData.get(i).getX() + "," + listGyroData.get(i).getY() + "," + listGyroData.get(i).getZ()).getBytes());
                    if (i != listGyroData.size() - 1)
                        fos.write("\r\n".getBytes());
                }
                fos.close();
                Log.d(TAG, "saveDataToFile:: gyro data saved successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * called by LeBleService to store data of Accelerometer and Gyroscope Sensors to File in device memory
     * @param context : context of the caller
     * @param btDeviceList : list of BLE Data
     */
    public static synchronized void saveBLEDataToFile(Context context, List<BTDevice> btDeviceList){
        if(fileUploadInProgress){
            Log.d(TAG, "saveBLEDataToFile:: fileUploadInProgress, will save data in the next call");
            return;
        }

        final File fileBle = getBLEFile(context);

        Log.d(TAG, "saveBLEDataToFile::  absolute path: fileBle: "+fileBle.getAbsolutePath());
        Log.d(TAG, "saveBLEDataToFile::  saving " + btDeviceList.size() + " devices to file");

        boolean fileBleExists = fileBle.exists();
        try {
            if(!fileBleExists) {
                fileBle.getParentFile().mkdirs();
                fileBle.createNewFile();
            }
        }catch(Exception e){}

        if(btDeviceList.size() == 0){
            Log.d(TAG, "saveBLEDataToFile:: No BLE data to save");
            //check if file is empty, if so, delete it
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileBle));
                if(br.readLine() == null)
                    fileBle.delete();
            } catch (Exception e) { Log.e(TAG, "saveBLEToFile:: Error",e); }
        }
        else {

            //HashSet<String> macList = new HashSet<>();    //macList implemented while taking scans, no need to check now
            try {
                FileOutputStream fos = new FileOutputStream(fileBle, true);

                //first two lines has the device ID to verify which device these scans are from, but only write if file is new
                if(!bleFileAlreadyExists) {
                    fos.write(DEVICE_ID.getBytes());
                }

                fos.write("\r\n".getBytes());

                for (int i = 0; i < btDeviceList.size(); i++) {
                    //BTDevice btDevice = btDeviceList.get(i);

                    //if(macList.contains(btDevice.getMac())) continue;

                    fos.write(btDeviceList.get(i).toString().getBytes());
                    if (i != btDeviceList.size() - 1)
                        fos.write("\r\n".getBytes());

                   // macList.add(btDevice.getMac());
                }
                fos.close();
                Log.d(TAG, "saveBLEDataToFile:: BLE data saved successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * called by SensorService to store data of Accelerometer and Gyroscope Sensors to File in device memory
     * @param context : context of the caller
     * @param listPPGData : list of PPG Data
     */
    public static synchronized void savePPGDataToFile(Context context, List<PPGData> listPPGData){
        if(fileUploadInProgress){
            Log.d(TAG, "savePPGDataToFile:: fileUploadInProgress, will save data in the next call");
            return;
        }

        //final File dir = new File(context.getFilesDir() + "/DC/");
        //dir.mkdirs();

        final File filePPG = getPPGFile(context);

        Log.d(TAG, "savePPGDataToFile::  absolute path: filePPG: "+filePPG.getAbsolutePath());

        boolean filePPGExists = filePPG.exists();
        try {
            if(!filePPGExists) {
                filePPG.getParentFile().mkdirs();
                filePPG.createNewFile();
            }
        }catch(Exception e){}

        if(listPPGData.size() == 0){
            Log.d(TAG, "savePPGDataToFile:: No PPG data to save");
            //check if file is empty, if so, delete it
            try {
                BufferedReader br = new BufferedReader(new FileReader(filePPG));
                if(br.readLine() == null)
                    filePPG.delete();
            } catch (Exception e) { Log.e(TAG, "savePPGToFile:: Error",e); }
        } else {

            try {
                FileOutputStream fos = new FileOutputStream(filePPG, true);

                if(!ppgFileAlreadyExists) {
                    fos.write(DEVICE_ID.getBytes());
                }
                fos.write("\r\n".getBytes());
                for (int i = 0; i < listPPGData.size() ; i++) {
                    //PPGData ppgData = listPPGData.get(i);

                    fos.write((listPPGData.get(i).getTimestamp() + "," + listPPGData.get(i).getHeartRate()).getBytes());
                    if (i != listPPGData.size() - 1)
                        fos.write("\r\n".getBytes());
                }
                fos.close();
                Log.d(TAG, "savePPGDataToFile:: PPG data saved successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stores the tag selected by the user from the recycler view
     * @param timeStamp The time stamp created when the user touches the tag
     * @param activity The activity selected by the user
     * @param status can be end or start and indicates the beginning or ending of an activity
     */
    public static synchronized void saveActivityDataToFile(Context context, String timeStamp, String activity, String status) throws IOException{
        if(fileUploadInProgress){
            //This means that no label its saved if the watch is uploading (connected to power)
            //TODO: Think how to solve this issue. If this fails to upload, the activity will not hold the tag for so long
            //maybe it would be required to create a service or something that will wait until it is possible to write and then write the tag
            //to file. For now this tag is lost.
            Log.d(TAG, "saveActivityDataToFile:: fileUploadInProgress, will save data in the next call");

            //TODO: Handle with Exception
            return;
        }

        final File fileActivity = getActFile(context);

        Log.d(TAG, "saveActivityDataToFile::  absolute path: fileActivity: "+fileActivity.getAbsolutePath());

        boolean fileActivityExists = fileActivity.exists();
        try {
            if(!fileActivityExists) {
                fileActivity.getParentFile().mkdirs();
                fileActivity.createNewFile();
            }
        }catch(Exception e){}

        try {
            FileOutputStream fos = new FileOutputStream(fileActivity, true);

            if(!actFileAlreadyExists) {
                fos.write(DEVICE_ID.getBytes());
            }
            fos.write("\r\n".getBytes());
            fos.write((timeStamp + "," + activity + "," + status).getBytes());
            fos.close();
            Log.d(TAG, "saveActivityDataToFile:: Activity data saved successfully");
        } catch (IOException e) {
            throw e;
        }

    }

    /**
     * Stores the feedback information provided by the user
     * @param timeStamp The time stamp belonging to the features
     * @param predictedLabel The label that was predicted by the model
     * @param correctLabel The actual label provided by the user
     */
    public static synchronized void saveFeedbackDataToFile(Context context, String timeStamp, String predictedLabel, String correctLabel, double features[]) throws IOException{
        if(fileUploadInProgress){
            //TODO: Verify how could this affect our collection process if the data is being transfered and the user disconnects the watch and uses it within the nuc range
            //TODO: Should we add a loading screen when the data is being transferred?
            Log.d(TAG, "saveFeedbackDataToFile:: fileUploadInProgress, will save data in the next call");
            return;
        }

        final File fileFeedback = getFeedbackFile(context);

        Log.d(TAG, "saveFeedbackDataToFile:  absolute path: fileFeedback: " + fileFeedback.getAbsolutePath());

        boolean fileFeedbackExists = fileFeedback.exists();
        try {
            if(!fileFeedbackExists) {
                fileFeedback.getParentFile().mkdirs();
                fileFeedback.createNewFile();
            }
        }catch(Exception e){}

        try {
            FileOutputStream fos = new FileOutputStream(fileFeedback, true);

            if(!feedbackFileAlreadyExists) {
                fos.write(DEVICE_ID.getBytes());
            }
            fos.write("\r\n".getBytes());
            String feat = "";
            if(features!= null) {
                for (int i = 0; i < features.length; i++) {
                    feat += String.valueOf(features[i]);
                    if (i + 1 < features.length) {
                        feat += ",";
                    }
                }
            }else{
                Log.d(TAG, "saveFeedbackDataToFile: Features were null for some reason");
            }
            //TODO: If the model is with context, the features changes. So, it is good to add a column indicating the type of model
            fos.write((timeStamp + "," + predictedLabel + "," + correctLabel + "," + feat).getBytes());
            fos.close();
            Log.d(TAG, "saveFeedbackDataToFile:: Feedback data saved successfully");
        } catch (IOException e) {
            throw e;
        }

    }

    /**
     * Stores the predictions information in a file
     * @param predictions The strings that will be saved
     */
    public static synchronized void savePredictionsToFile(Context context, List<String> predictions) throws IOException{
        if(fileUploadInProgress){
            //TODO: Verify how could this affect our collection process if the data is being transfered and the user disconnects the watch and uses it within the nuc range
            //TODO: Should we add a loading screen when the data is being transferred?
            Log.d(TAG, "savePredictionsToFile:: fileUploadInProgress, will save data in the next call");
            return;
        }

        if(predictions.size()==0){
            return;
        }

        final File filePredictions = getPredictionsFile(context);

        Log.d(TAG, "savePredictionsToFile:  absolute path: filePredictions: " + filePredictions.getAbsolutePath());

        boolean filePredictionsExists = filePredictions.exists();
        try {
            if(!filePredictionsExists) {
                filePredictions.getParentFile().mkdirs();
                filePredictions.createNewFile();
            }
        }catch(Exception e){}

        try {
            FileOutputStream fos = new FileOutputStream(filePredictions, true);

            if(!predictionsFileAlreadyExists) {
                fos.write(DEVICE_ID.getBytes());
            }
            fos.write("\r\n".getBytes());
            for (int i=0; i<predictions.size(); i++){
                fos.write((predictions.get(i)).getBytes());
                if (i != predictions.size() - 1)
                    fos.write("\r\n".getBytes());
            }
            //TODO: If the model is with context, the features changes. So, it is good to add a column indicating the type of model

            fos.close();
            Log.d(TAG, "savePredictionsToFile:: Predictions data saved successfully");
        } catch (IOException e) {
            throw e;
        }

    }

    /**
     * Clears data from one file
     * @param file: file from which data is to be cleared
     */
    public static void clearFileContent(File file){
        try {
            PrintWriter writer = new PrintWriter(file);
            writer.print("");
            writer.close();
        }catch(FileNotFoundException e){
            Log.e(TAG, "clearFileContent:: file not found");
        }
    }

    /**
     * clears data from all the files
     * @param files: array of files from which data is to be cleared.
     */
    public static void clearFilesContent(File[] files){
        for(File file : files){
            try {
                PrintWriter writer = new PrintWriter(file);
                writer.print("");
                writer.close();
            }catch(FileNotFoundException e){
                Log.e(TAG, "clearFileContent:: file not found");
            }
        }

    }

    /*
    public static void clearFileContentBLE(File file) {
        try {
            PrintWriter writer = new PrintWriter(file);
            writer.print("");
            writer.close();
        }catch(FileNotFoundException e){
            Log.e(TAG, "clearFileContent:: file not found");
        }
    }*/

    /*
    No idea what this function is supposed to be for..
     */
    public static String getMimeType(File file) {

        String extension = "txt";

        if (extension.length() > 0)
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.substring(1));

        return "application/octet-stream";
    }

    public static byte[] getFileByteArray(File file){
        byte[] b = new byte[(int) file.length()];
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(b);
            for (int i = 0; i < b.length; i++) {
                System.out.print((char)b[i]);
            }
        } catch (FileNotFoundException e) {
            System.out.println("File Not Found.");
            e.printStackTrace();
        }
        catch (IOException e1) {
            System.out.println("Error Reading The File.");
            e1.printStackTrace();
        }
        return b;
    }

    /**
     * creates if not already created, and sends to the caller accelerometer file
     * @param context
     * @return
     */
    public static File getAccelerometerFile(Context context){
        final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/" + DEVICE_ID + "/" + Util.getDateForDir());

        final File fileAccel = new File(dir, FILE_NAME_ACCELEROMETER);
        if(!fileAccel.exists()) {
            try {
                fileAccel.getParentFile().mkdirs();
                fileAccel.createNewFile();
                accFileAlreadyExists = false;
            }catch(IOException e){}
        }
        else accFileAlreadyExists = true;
        return fileAccel;
    }

    /**
     * creates if not already created, and sends to the caller Gyroscope file
     * @param context
     * @return
     */
    public static File getGyroScopeFile(Context context){
        final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/" + DEVICE_ID + "/" + Util.getDateForDir());

        final File fileGyro = new File(dir, FILE_NAME_GYROSCOPE);
        if(!fileGyro.exists()) {
            try {
                fileGyro.getParentFile().mkdirs();
                fileGyro.createNewFile();
                gyroFileAlreadyExists = false;
            }catch(IOException e){}
        }
        else gyroFileAlreadyExists = true;
        return fileGyro;
    }

    /**
     * creates if not already created, and sends to the caller BLE file
     * @param context
     * @return
     */
    public static File getBLEFile(Context context){

        //format is /{app_files}/DC/{DEVICE_ID}/{DATE}/ble_data.txt
        final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/" + DEVICE_ID + "/" + Util.getDateForDir());
        final File fileBLE = new File(dir, FILE_NAME_BLE);

        if(!fileBLE.exists()) {
            try {
                fileBLE.getParentFile().mkdirs();
                fileBLE.createNewFile();
                bleFileAlreadyExists = false;
            }catch(IOException e){}
        }
        else bleFileAlreadyExists = true;
        return fileBLE;
    }

    /**
     * creates if not already created, and sends to the caller PPG file
     * @param context
     * @return
     */
    public static File getPPGFile(Context context){
        //format is /{app_files}/DC/{DEVICE_ID}/{DATE}/ppg_data.txt
        final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/" + DEVICE_ID + "/" + Util.getDateForDir());

        final File filePPG = new File(dir, FILE_NAME_PPG);
        if(!filePPG.exists()) {
            try {
                filePPG.getParentFile().mkdirs();
                filePPG.createNewFile();
                ppgFileAlreadyExists = false;
            }catch(IOException e){}
        }
        else ppgFileAlreadyExists = true;
        return filePPG;
    }

    /**
     * creates if not already created, and sends to the caller activity tag file
     * @param context
     * @return
     */
    public static File getActFile(Context context){
        //format is /{app_files}/DC/{DEVICE_ID}/{DATE}/actTag_data.txt
        final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/" + DEVICE_ID + "/" + Util.getDateForDir());
        final File fileAct = new File(dir, FILE_NAME_ACTIVITY);

        if(!fileAct.exists()) {
            try {
                fileAct.getParentFile().mkdirs();
                fileAct.createNewFile();
                actFileAlreadyExists = false;
            }catch(IOException e){}
        }
        else actFileAlreadyExists = true;
        return fileAct;
    }

    public static File getFeedbackFile(Context context){
        //format is /{app_files}/DC/{DEVICE_ID}/{DATE}/actTag_data.txt
        final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/" + DEVICE_ID + "/" + Util.getDateForDir());
        final File fileFeedback = new File(dir, FILE_NAME_FEEDBACK);

        if(!fileFeedback.exists()) {
            try {
                fileFeedback.getParentFile().mkdirs();
                fileFeedback.createNewFile();
                feedbackFileAlreadyExists = false;
            }catch(IOException e){}
        }
        else feedbackFileAlreadyExists = true;
        return fileFeedback;
    }

    public static File getPredictionsFile(Context context){
        //format is /{app_files}/DC/{DEVICE_ID}/{DATE}/actTag_data.txt
        final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/" + DEVICE_ID + "/" + Util.getDateForDir());
        final File filePredictions = new File(dir, FILE_NAME_PREDICTIONS);

        if(!filePredictions.exists()) {
            try {
                filePredictions.getParentFile().mkdirs();
                filePredictions.createNewFile();
                predictionsFileAlreadyExists = false;
            }catch(IOException e){}
        }
        else predictionsFileAlreadyExists = true;
        return filePredictions;
    }


    public static final int BLE_DIR = 0;
    public static final int ACC_DIR = 1;
    public static final int GYRO_DIR = 2;
    public static final int PPG_DIR = 3;
    public static final int BASE_DIR = -1;
    /*
    @param folderID: use one of the 4 IDs to get the folder (File)
        pass in -1 (or number >3) to receive base directory for saved files
    IDs:    BLE_DIR, ACC_DIR, GYRO_DIR, and PPG_DIR
     */
    public static File getCollectedDataDir(Context context, int folderID) {

        switch(folderID) {
            case BLE_DIR:
                return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/" + DEVICE_ID + "/BLE/");
            case ACC_DIR:
                return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/" + DEVICE_ID + "/ACC/");
            case GYRO_DIR:
                return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/" + DEVICE_ID + "/GYRO/");
            case PPG_DIR:
                return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/" + DEVICE_ID + "/PPG/");

            default:
                //in this case just return the default folder (one level up);
                return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/" + DEVICE_ID + "/");

        }
    }



/*      todo: when large amount of data is compressed, it can cause the app to crash due to memory constraints
 *
 * Zips a file at a location and places the resulting zip file at the toLocation
 * Example: zipFileAtPath("downloads/myfolder", "downloads/myFolder.zip");
 *
 * Note that this will overwrite the last instance of the zipped file (i.e. does not append; overwrites)
 */

    public static boolean zipFileAtPath(String sourcePath, String toLocation) {
        final int BUFFER = 2048;

        Log.d(TAG,"zipFileAtPath:: Begin zipping folder: " + sourcePath);

        File sourceFile = new File(sourcePath);



        try {
            //first get stream to handle IO between file and new zipped file

            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(toLocation);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            if (sourceFile.isDirectory()) {
                Log.d(TAG, "zipFileAtPath:: directory has size " + fileSize(sourceFile) + " bytes");
                zipSubFolder(out, sourceFile, sourceFile.getParent().length());
            } else {
                byte data[] = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(sourcePath);
                origin = new BufferedInputStream(fi, BUFFER);

                //not so sure about this line
                ZipEntry entry = new ZipEntry(getLastPathComponent(sourcePath));
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        Log.d(TAG, "zipFileAtPath:: Done with destination path"  + toLocation);
        return true;
    }

/*
 *
 * Zips a subfolder
 *
 */

    private static void zipSubFolder(ZipOutputStream out, File folder,
                              int basePathLength) throws IOException {

        final int BUFFER = 2048;

        File[] fileList = folder.listFiles();
        BufferedInputStream origin = null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                Log.d(TAG, "zipSubFolder:: " + file.getPath());
                zipSubFolder(out, file, basePathLength);
            } else {
                String unmodifiedFilePath = file.getPath();

                //The .zip file is created before data is added to it, making an empty .zip file show in zipped folder
                if(unmodifiedFilePath.endsWith(".zip"))
                    continue;   //if we find that the file is a .zip file, we ignore it, and continue to next file

                byte data[] = new byte[BUFFER];
                String relativePath = unmodifiedFilePath
                        .substring(basePathLength);
                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }

    /*
     * gets the last path component
     *
     * Example: getLastPathComponent("downloads/example/fileToZip");
     * Result: "fileToZip"
     */
    public static String getLastPathComponent(String filePath) {
        String[] segments = filePath.split("/");
        if (segments.length == 0)
            return "";
        String lastPathComponent = segments[segments.length - 1];
        Log.d(TAG, "getLastPathComponent: " + lastPathComponent);
        return lastPathComponent;
    }

    /*
        Returns the size of the directory or file in bytes as a long
     */
    public static long fileSize(File file) {

        if (file.exists()) {
            long result = 0;
            if (file.isDirectory()) {
                File[] fileList = file.listFiles();
                for (int i = 0; i < fileList.length; i++) {
                    // Recursive call if it's a directory
                    if (fileList[i].isDirectory()) {
                        result += fileSize(fileList[i]);
                    } else {
                        // Sum the file size in bytes
                        result += fileList[i].length();
                    }
                }
            }
            else result = file.length();
            return result; // return the file size
        }
        return 0;
    }


    public synchronized static void baseExternalSensorSaveToFile(BaseExternalSensorService service, List<Object> sensorData){
        //Here the toString method from each element on sensorData will return the format on which the row should be saved on the files
        String fileName = service.getClass().getSimpleName() + ".txt";
        String TAG = "Saving_Data_" + fileName;

        if(fileUploadInProgress){
            Log.d(TAG, "baseExternalSensorSaveToFile:: fileUploadInProgress, will save data in the next call");
            return;
        }

        final File fileExternalS = getExternalSensorFile(service, fileName);

        Log.d(TAG, "baseExternalSensorSaveToFile::  absolute path: fileExternalS: "+fileExternalS.getAbsolutePath());

        boolean filePPGExists = fileExternalS.exists();
        try {
            if(!filePPGExists) {
                fileExternalS.getParentFile().mkdirs();
                fileExternalS.createNewFile();
            }
        }catch(Exception e){}

        if(sensorData.size() == 0){
            Log.d(TAG, "baseExternalSensorSaveToFile:: No external sensor data to save");
            //check if file is empty, if so, delete it
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileExternalS));
                if(br.readLine() == null)
                    fileExternalS.delete();
            } catch (Exception e) { Log.e(TAG, "baseExternalSensorSaveToFile:: Error",e); }
        } else {

            try {
                FileOutputStream fos = new FileOutputStream(fileExternalS, true);

                if(fileExternalS.length() == 0) {
                    fos.write(DEVICE_ID.getBytes());
                }
                fos.write("\r\n".getBytes());
                for (int i = 0; i < sensorData.size() ; i++) {

                    fos.write((sensorData.get(i).toString()).getBytes());
                    if (i != sensorData.size() - 1)
                        fos.write("\r\n".getBytes());
                }
                fos.close();
                Log.d(TAG, "baseExternalSensorSaveToFile:: External sensor data data saved successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * creates if not already created, and sends to the caller PPG file
     * @param context
     * @return
     */
    public static File getExternalSensorFile(Context context, String fileName){
        //format is /{app_files}/DC/{DEVICE_ID}/{DATE}/filename.txt
        final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DC/" + DEVICE_ID + "/" + Util.getDateForDir());

        final File fileExternalS = new File(dir, fileName);
        if(!fileExternalS.exists()) {
            try {
                fileExternalS.getParentFile().mkdirs();
                fileExternalS.createNewFile();
            }catch(IOException e){}
        }
        return fileExternalS;
    }



}
