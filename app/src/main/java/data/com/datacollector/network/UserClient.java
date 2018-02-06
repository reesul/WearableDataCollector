package data.com.datacollector.network;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * interface to be used for Rest API communication
 * Created by ritu on 11/9/17.
 */

public interface UserClient {

    /*
    @Multipart
    @POST("uploadfiles")
    Call<ResponseBody> uploadfiles(
            @Part MultipartBody.Part file1,
            @Part MultipartBody.Part file2,
            @Part MultipartBody.Part file3,
            @Part MultipartBody.Part file4
    );
    */

    @Multipart
    @POST("uploadfiles")
    Call<ResponseBody> uploadBLEfile(
            @Part MultipartBody.Part fileBLE
    );

    @Multipart
    @POST("uploadfiles")
    Call<ResponseBody> uploadfile(
            @Part MultipartBody.Part file
    );
}
