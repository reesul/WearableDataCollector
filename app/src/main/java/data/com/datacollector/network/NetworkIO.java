package data.com.datacollector.network;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.concurrent.TimeUnit;

import data.com.datacollector.utility.FileUtil;
import data.com.datacollector.utility.Util;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
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
    public static boolean fileUploadInProgress = false;

    public static boolean lastUploadResult = true;

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
    public static void uploadData(final Context context){
        Log.d(TAG, "uploadData::\n\n");
        fileUploadInProgress = true;

        //final File dir = new File(context.getApplicationContext().getFilesDir() + "/DC/");
        //dir.mkdirs();

        //zip contents of folder holding all BLE files, returns the path to the file
        final File dir = FileUtil.getCollectedDataDir(context, FileUtil.BASE_DIR);
        if(!dir.exists())
            dir.mkdirs();

        String sourcePath = dir.getPath();
        String destPath = sourcePath + "/DC.zip";

        //attempt to zip the files at the sourcePath (includes all subdirectories and files)
        if(!FileUtil.zipFileAtPath(sourcePath, destPath)) {
            //if we fail, do not attempt to upload a file, and log an error
            Log.e(TAG, "uploadDataBLE:: Zipping folder failed");
            lastUploadResult = false;
            fileUploadInProgress = false;
            return;
        }
        //final String bleZipPath  = FileUtil.zipFolder(context, FileUtil.BLE_DIR);
        File fileBLE = new File(destPath);

        RequestBody filePart = RequestBody.create(MediaType.parse("text/plain"), fileBLE);
        MultipartBody.Part fileMultiPartBody = MultipartBody.Part.createFormData("file", fileBLE.getName(), filePart);

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        int timeVal = 1;
        //connection will timeout after one minute
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).writeTimeout(timeVal, TimeUnit.MINUTES)
                .readTimeout(timeVal, TimeUnit.MINUTES).connectTimeout(timeVal, TimeUnit.MINUTES).build();

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(BASE_SERVER_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create());

        Retrofit retrofit = builder.build();

        UserClient service = retrofit.create(UserClient.class);
        Call<ResponseBody> call = service.uploadBLEfile(fileMultiPartBody);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "uploadData:: successful:: "+response.toString());
                fileUploadInProgress = false;
                String PathToDir = FileUtil.getCollectedDataDir(context.getApplicationContext(), FileUtil.BASE_DIR).getPath();
                clearFilesContent(PathToDir);
                //clearFilesContentBLE(context, PathToDir);
                lastUploadResult = true;
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "uploadData:: failure: "+t.toString());
                fileUploadInProgress = false;
                lastUploadResult = false;
            }
        });



    }

    public static void uploadDataBLE(final Context context) {
        Log.d(TAG, "uploadDataBLE::\n\n");
        fileUploadInProgress = true;

        //final File dir = new File(context.getApplicationContext().getFilesDir() + "/DC/");
        //dir.mkdirs();

        //zip contents of folder holding all BLE files, returns the path to the file
        final File dir = FileUtil.getCollectedDataDir(context, FileUtil.BASE_DIR);
        if(!dir.exists())
            dir.mkdirs();

        String sourcePath = dir.getPath();
        String destPath = sourcePath + "/DC.zip";

        //attempt to zip the files at the sourcePath (includes all subdirectories and files)
        if(!FileUtil.zipFileAtPath(sourcePath, destPath)) {
            //if we fail, do not attempt to upload a file, and log an error
            Log.e(TAG, "uploadDataBLE:: Zipping folder failed");
            lastUploadResult = false;
            fileUploadInProgress = false;
            return;
        }
        //final String bleZipPath  = FileUtil.zipFolder(context, FileUtil.BLE_DIR);
        File fileBLE = new File(destPath);

        RequestBody filePart = RequestBody.create(MediaType.parse("text/plain"), fileBLE);
        MultipartBody.Part fileMultiPartBody = MultipartBody.Part.createFormData("file", fileBLE.getName(), filePart);

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
        Call<ResponseBody> call = service.uploadBLEfile(fileMultiPartBody);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "uploadData:: successful:: "+response.toString());
                fileUploadInProgress = false;
                String PathToBLEDir = FileUtil.getCollectedDataDir(context.getApplicationContext(), FileUtil.BASE_DIR).getPath();
                clearFilesContentBLE(context, PathToBLEDir);
                lastUploadResult = true;
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "uploadData:: failure: "+t.toString());
                fileUploadInProgress = false;
                lastUploadResult = false;
            }
        });


    }

    /**
     * Called after data is successfully uploaded to the server. The method clears the COntent from all the sensor files.
     * @param context
     */
    private static void clearFilesContent(Context context){
        Log.d(TAG, "clearFilesContent");
        final File dir = new File(context.getApplicationContext().getFilesDir() + "/DC/");
        dir.mkdirs();
        final File file1 = FileUtil.getAccelerometerFile(context);
        final File file2 = FileUtil.getGyroScopeFile(context);
        final File file3 = FileUtil.getBLEFile(context);
        final File file4 = FileUtil.getPPGFile(context);

        boolean fileBleExists = file1.exists();
        try {
            if(!fileBleExists) {
                file1.getParentFile().mkdirs();
                file1.createNewFile();
            }
        }catch(Exception e){}
        FileUtil.clearFilesContent(new File[]{file1, file2, file3, file4});
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

    /*
    @parameter path: path of the file to be removed;

    function is used to delete files at the path provided
    */
    private static void clearFilesContentBLE(Context context, String path) {
        Log.d(TAG, "clearFilesContent:: at " + path);
        final File file = new File(path);


        //if the file is a directory, then we also need to delete the files under it
        if(file.isDirectory()) {
            File[] fileList = file.listFiles();

            //if folder contains a date other than today in its path, delete this folder and its contained files
            if(!file.getAbsolutePath().contains(Util.getDateForDir())) {

            }

            for (File subfile : fileList) {
                //remove all files within directory recursively; also remove subfolders in the process

                //however if the file is from today, do not clear it
                //if(subfile.getAbsolutePath().contains(Util.getDateForDir()))
                //    continue;
                clearFilesContentBLE(context, subfile.getPath());
            }
            file.delete();  //then delete the directory
        }
        else
            file.delete();

        /* @deprecated, file structure changed
        final File file = FileUtil.getBLEFile(context);

        boolean fileBleExists = file.exists();
        try {
            if(!fileBleExists) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        }catch(Exception e){}
        FileUtil.clearFileContentBLE(file);*/
    }

}
