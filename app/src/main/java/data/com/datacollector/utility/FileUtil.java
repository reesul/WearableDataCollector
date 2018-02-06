package data.com.datacollector.utility;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import data.com.datacollector.model.BTDevice;
import data.com.datacollector.model.Const;
import data.com.datacollector.model.PPGData;
import data.com.datacollector.model.SensorData;
import data.com.datacollector.network.NetworkIO;

import static data.com.datacollector.model.Const.DEVICE_ID;
import static data.com.datacollector.model.Const.FILE_NAME_ACCELEROMETER;
import static data.com.datacollector.model.Const.FILE_NAME_BLE;
import static data.com.datacollector.model.Const.FILE_NAME_GYROSCOPE;
import static data.com.datacollector.model.Const.FILE_NAME_PPG;
import static data.com.datacollector.model.Const.FILE_NAME_ACTIVITY_TAG;
import static data.com.datacollector.model.Const.MAX_PER_MIN_SENSOR_DATA_ALLOWED;

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
    private static boolean actTagFileAlreadyExists;
    /**
     * called by SensorService to store data of Accelerometer and Gyroscope Sensors to File in device memory
     * @param context : context of the caller
     * @param listAccelData : list of Accelerometer Data
     * @param listGyroData: list of Gyroscopre Data
     */
    public static synchronized void saveGyroNAcceleroDataToFile(Context context, List<SensorData> listAccelData, List<SensorData> listGyroData){
        if(NetworkIO.fileUploadInProgress){
            Log.d(TAG, "saveDataToFile:: fileUploadInProgress, will save data in the next call");
            return;
        }

        List<SensorData> tempAccelList = new ArrayList<>(listAccelData);
        List<SensorData> tempGyroList = new ArrayList<>(listGyroData);

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
        if(tempAccelList.size() == 0){
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
                //fos.write("\r\n".getBytes()); TODO see if this fixes empty lines when saving between deep sleep

                for (int i = 0; i < tempAccelList.size() && i < MAX_PER_MIN_SENSOR_DATA_ALLOWED; i++) {
                    SensorData sensorData = tempAccelList.get(i);

                    fos.write((sensorData.getTimestamp() + "   " + sensorData.getX() + "   " + sensorData.getY() + "   " + sensorData.getZ()).getBytes());
                   // if (i != tempAccelList.size() - 1) TODO see if this fixes empty lines when saving between deep sleep
                        fos.write("\r\n".getBytes());
                }
                fos.close();
                Log.d(TAG, "saveDataToFile:: accelerometer data saved successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(tempGyroList.size() == 0){
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
                for (int i = 0; i < tempGyroList.size() && i < MAX_PER_MIN_SENSOR_DATA_ALLOWED; i++) {
                    SensorData sensorData = tempGyroList.get(i);

                    fos.write((sensorData.getTimestamp() + "   " + sensorData.getX() + "   " + sensorData.getY() + "   " + sensorData.getZ()).getBytes());
                    if (i != tempGyroList.size() - 1)
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
        if(NetworkIO.fileUploadInProgress){
            Log.d(TAG, "saveBLEDataToFile:: fileUploadInProgress, will save data in the next call");
            return;
        }

        List<BTDevice> tempBleList = new ArrayList<>(btDeviceList); //copy the current BLE device list
        //final File dir = new File(context.getFilesDir() + "/DC/" + ); getBLEFile() should handle this; TODO test this idea
        //dir.mkdirs();
        final File fileBle = getBLEFile(context);

        Log.d(TAG, "saveBLEDataToFile::  absolute path: fileBle: "+fileBle.getAbsolutePath());

        boolean fileBleExists = fileBle.exists();
        try {
            if(!fileBleExists) {
                fileBle.getParentFile().mkdirs();
                fileBle.createNewFile();
            }
        }catch(Exception e){}

        if(tempBleList.size() == 0){
            Log.d(TAG, "saveBLEDataToFile:: No BLE data to save");
            //check if file is empty, if so, delete it
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileBle));
                if(br.readLine() == null)
                    fileBle.delete();
            } catch (Exception e) { Log.e(TAG, "savePPGToFile:: Error",e); }
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

                for (int i = 0; i < tempBleList.size() && i < MAX_PER_MIN_SENSOR_DATA_ALLOWED; i++) {
                    BTDevice btDevice = tempBleList.get(i);

                    //if(macList.contains(btDevice.getMac())) continue;

                    fos.write(btDevice.toString().getBytes());
                    if (i != tempBleList.size() - 1)
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
        if(NetworkIO.fileUploadInProgress){
            Log.d(TAG, "savePPGDataToFile:: fileUploadInProgress, will save data in the next call");
            return;
        }

        List<PPGData> tempPPGList = new ArrayList<>(listPPGData);
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

        if(tempPPGList.size() == 0){
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
                for (int i = 0; i < tempPPGList.size() && i < MAX_PER_MIN_SENSOR_DATA_ALLOWED; i++) {
                    PPGData ppgData = tempPPGList.get(i);

                    fos.write((ppgData.getTimestamp() + "  " + ppgData.getHeartRate()).getBytes());
                    if (i != tempPPGList.size() - 1)
                        fos.write("\r\n".getBytes());
                }
                fos.close();
                Log.d(TAG, "savePPGDataToFile:: BLE data saved successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stores the tag selected by the user from the recycler view
     * @param timeStamp The time stamp created when the user touches the tag
     * @param activityTag The tag corresponding to the user touch
     */
    public static synchronized void saveActTagDataToFile(Context context, String timeStamp, String activityTag) throws IOException{
        if(NetworkIO.fileUploadInProgress){
            //TODO: Think how to solve this issue. If this fails to upload, the activity will not hold the tag for so long
            //maybe it would be required to create a service or something that will wait until it is possible to write and then write the tag
            //to file. For now this tag is lost.
            Log.d(TAG, "saveActTagDataToFile:: fileUploadInProgress, will save data in the next call");

            //TODO: Handle with Exception
            return;
        }

        final File fileActTag = getActTagFile(context);

        Log.d(TAG, "saveActTacDataToFile::  absolute path: fileActTag: "+fileActTag.getAbsolutePath());

        boolean fileActTagExists = fileActTag.exists();
        try {
            if(!fileActTagExists) {
                fileActTag.getParentFile().mkdirs();
                fileActTag.createNewFile();
            }
        }catch(Exception e){}

        try {
            FileOutputStream fos = new FileOutputStream(fileActTag, true);

            if(!actTagFileAlreadyExists) {
                fos.write(DEVICE_ID.getBytes());
            }
            fos.write("\r\n".getBytes());
            fos.write((timeStamp + "  " + activityTag).getBytes());
            fos.close();
            Log.d(TAG, "saveActTacDataToFile:: Activity tag data saved successfully");
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
        final File dir = new File(context.getFilesDir() + "/DC/" + DEVICE_ID + "/" + Util.getDateForDir());

        //2 old versions for file names
        //final File dir = new File(context.getFilesDir() + "/DC/" + DEVICE_ID + "/PPG/" + Util.getDateForDir()+"/");
        //final File fileAccel = new File(dir, Build.SERIAL.substring(6) + "_" + FILE_NAME_ACCELEROMETER);+

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
        final File dir = new File(context.getFilesDir() + "/DC/" + DEVICE_ID + "/" + Util.getDateForDir());

        //2 old versions for file names
        //final File dir = new File(context.getFilesDir() + "/DC/" + DEVICE_ID + "/GYRO/" + Util.getDateForDir() + "/");
        //final File fileGyro = new File(dir, Build.SERIAL.substring(6) + "_" + FILE_NAME_GYROSCOPE);

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
        final File dir = new File(context.getFilesDir() + "/DC/" + DEVICE_ID + "/" + Util.getDateForDir());
        final File fileBLE = new File(dir, FILE_NAME_BLE);
        //final File fileBLE = new File(dir, Util.getTimeForFileName(System.currentTimeMillis())+ "_" + FILE_NAME_BLE);

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
        final File dir = new File(context.getFilesDir() + "/DC/" + DEVICE_ID + "/" + Util.getDateForDir());

        //2 old versions for file names
        //final File dir = new File(context.getFilesDir() + "/DC/" + DEVICE_ID + "/PPG/" + Util.getDateForDir()+ "/");
        //final File filePPG = new File(dir, Build.SERIAL.substring(6) + "_" + FILE_NAME_PPG);

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
    public static File getActTagFile(Context context){
        //format is /{app_files}/DC/{DEVICE_ID}/{DATE}/actTag_data.txt
        final File dir = new File(context.getFilesDir() + "/DC/" + DEVICE_ID + "/" + Util.getDateForDir());
        final File fileActTag = new File(dir, FILE_NAME_ACTIVITY_TAG);

        if(!fileActTag.exists()) {
            try {
                fileActTag.getParentFile().mkdirs();
                fileActTag.createNewFile();
                actTagFileAlreadyExists = false;
            }catch(IOException e){}
        }
        else actTagFileAlreadyExists = true;
        return fileActTag;
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
                return new File(context.getFilesDir() + "/DC/" + DEVICE_ID + "/BLE/");
      /*    TODO: enable once BLE is working so we collect other data too
            case ACC_DIR:
                return new File(context.getFilesDir() + "/DC/" + DEVICE_ID + "/ACC/");
            case GYRO_DIR:
                return new File(context.getFilesDir() + "/DC/" + DEVICE_ID + "/GYRO/"););
            case PPG_DIR:
                return new File(context.getFilesDir() + "/DC/" + DEVICE_ID + "/PPG/");
        */
            default:
                //in this case just return the default folder (one level up);
                return new File(context.getFilesDir() + "/DC/" + DEVICE_ID + "/");

        }
    }



/*
 *
 * Zips a file at a location and places the resulting zip file at the toLocation
 * Example: zipFileAtPath("downloads/myfolder", "downloads/myFolder.zip");
 *
 * Note that this will overwrite the last instance of the zipped file (i.e. does not append; overwrites)
 */

    public static boolean zipFileAtPath(String sourcePath, String toLocation) {
        final int BUFFER = 2048;

        Log.d(TAG,"Begin zipping folder: " + sourcePath);

        File sourceFile = new File(sourcePath);

        try {
            //first get stream to handle IO between file and new zipped file

            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(toLocation);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            if (sourceFile.isDirectory()) {

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

        Log.i(TAG, "zipFileAtPath:: Destination Path"  + toLocation);
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



}
