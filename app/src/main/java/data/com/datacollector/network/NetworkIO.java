package data.com.datacollector.network;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Util;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

/*  Use these from okhttp3 if retrofit is unused
import okhttp3.Response;
import okhttp3.Callback;
import okhttp3.Call;
 */

/*  Only have these imports if we are actually using retrofit*/
import retrofit2.Callback; //can only have one of these
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


import static data.com.datacollector.model.Const.BASE_SERVER_URL;
import static data.com.datacollector.model.Const.DEVICE_ID;

/**
 * For Network operations, like uploading files
 * Created by ritu on 11/10/17.
 */

public class NetworkIO {
    private static final String TAG = "DC_NetworkIO";
    public static boolean isTransfer;

    /**
     * To be called by an initiator who wants to upload data saved in memory to the server
     * @param context: context of the caller
     *//*   deprecated, does transfer with 4 separate files
    public static void uploadData(final Context context){
        Log.d(TAG, "uploadData");
        fileUploadInProgress = true;

        final File dir = new File(context.getApplicationContext().getFilesDir() + "/DC/");
        dir.mkdirs();

        //TODO: change these files into zipped files containing all in folder
        final File file1 = FileUtil.getAccelerometerFile(context);
        final File file2 = FileUtil.getGyroScopeFile(context);
        final File file3 = FileUtil.getBLEFile(context);
        final File file4 = FileUtil.getPPGFile(context);

        RequestBody filePart1 = RequestBody.create(MediaType.parse("text/plain"), file1);
        RequestBody filePart2 = RequestBody.create(MediaType.parse("text/plain"), file2);
        RequestBody filePart3 = RequestBody.create(MediaType.parse("text/plain"), file3);
        RequestBody filePart4 = RequestBody.create(MediaType.parse("text/plain"), file4);

        MultipartBody.Part fileMultiPartBody1  = MultipartBody.Part.createFormData("file", file1.getName(), filePart1);
        MultipartBody.Part fileMultiPartBody2  = MultipartBody.Part.createFormData("file", file2.getName(), filePart2);
        MultipartBody.Part fileMultiPartBody3  = MultipartBody.Part.createFormData("file", file3.getName(), filePart3);
        MultipartBody.Part fileMultiPartBody4  = MultipartBody.Part.createFormData("file", file4.getName(), filePart4);

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        int timeVal = 1;
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).writeTimeout(timeVal, TimeUnit.MINUTES)
                .readTimeout(timeVal, TimeUnit.MINUTES).connectTimeout(timeVal, TimeUnit.MINUTES).build();

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(BASE_SERVER_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create());

        Retrofit retrofit = builder.build();

            UserClient service = retrofit.create(UserClient.class);
        Call<ResponseBody> call = service.uploadfiles(fileMultiPartBody1, fileMultiPartBody2, fileMultiPartBody3, fileMultiPartBody4);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "uploadData:: successful:: "+response.toString());
                fileUploadInProgress = false;
                clearFilesContent(context);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "uploadData:: failure: "+t.toString());
                fileUploadInProgress = false;
            }
        });
    }
    */


    /*
        upload file to a server
            compresses files into a .zip archive, then transfers a single file
            compression used to maintain file structure

            files from past days are removed after successful transfer, file from current day is kept

     */
    public static void uploadData(final Context context) {
        Log.d(TAG, "uploadData::\n\n");
        FileUtil.fileUploadInProgress = true;

        //final File dir = new File(context.getApplicationContext().getFilesDir() + "/DC/");
        //dir.mkdirs();

        //zip contents of folder holding all BLE files, returns the path to the file
        final File dir = FileUtil.getCollectedDataDir(context, FileUtil.BASE_DIR);
        if (!dir.exists())
            dir.mkdirs();



        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        int timeVal = 1;

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).writeTimeout(timeVal, TimeUnit.MINUTES)
                .readTimeout(timeVal, TimeUnit.MINUTES).connectTimeout(timeVal, TimeUnit.MINUTES).build();

        /* todo: see if I need to use retrofit or not here..*/
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(BASE_SERVER_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create());

        Retrofit retrofit = builder.build();
        UserClient service = retrofit.create(UserClient.class);


        //todo test different multipart creation
        MultipartBody.Builder multipartBodyBuild = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        File[] dateDirs = dir.listFiles();
        Log.d(TAG, "Number of files to send: "+dateDirs.length);

        for (final File date : dateDirs) {
            /*the current day's file will have incomplete information, so do this only for previous days
            TODO comment out this condition to control if today's data is sent or not!! depends on the application
                If this is not commented our, transfers will send data from today, which is incomplete, but useful for testing

            if(date.getPath().contains(Util.getDateForDir())) {
                Log.d(TAG, "skipping current day's data; most likely incomplete");
                continue;
            }
    */
            //Only folders should be in this directory (1 per day of data), delete anything else
            if(!date.isDirectory()) {
                date.delete();
                continue;
            }

            //uploaded zip file includes device id (Serial's last 8 digits) and the date (assumed to be the last part of the filepath)
            String destPath = date.getPath() + "/DC_" + DEVICE_ID + "_" + FileUtil.getLastPathComponent(date.getPath()) + ".zip";

            Log.d(TAG, "uploadData:: upload from " + date.getPath());

            if (!FileUtil.zipFileAtPath(date.getPath(), destPath)) {
                //if we fail, do not attempt to upload a file, and log an error
                Log.e(TAG, "uploadDataBLE:: Zipping folder failed");
                continue;
            }
            //get the file we just zipped
            final File zipFile = new File(destPath);
            Log.d(TAG, "uploadData:: Attempt to send " + zipFile.getName() + " of size " + FileUtil.fileSize(zipFile) + " bytes");

            RequestBody filePart = RequestBody.create(MediaType.parse("text/plain"), zipFile);
            MultipartBody.Part fileMultiPartBody = MultipartBody.Part.createFormData("file", zipFile.getName(), filePart);

            //todo remove? only need if not using retrofit
            multipartBodyBuild.addPart(fileMultiPartBody);

            Call<ResponseBody> call = service.uploadfile(fileMultiPartBody);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    Log.d(TAG, "uploadData:: successful:: " + response.toString());
                    FileUtil.fileUploadInProgress = false;
                    String PathToDir = date.getPath();

                    if (!PathToDir.contains(Util.getDateForDir())) {
                        clearFilesContent(PathToDir);
                    }
                    zipFile.delete();
                    FileUtil.lastUploadResult = true;
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d(TAG, "uploadData:: failure: " + t.toString());
                    FileUtil.fileUploadInProgress = false;
                    FileUtil.lastUploadResult = false;
                }
            });


        }
    }


    private static void clearFilesContent(String path) {
        Log.d(TAG, "clearFilesContent:: at " + path);
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
