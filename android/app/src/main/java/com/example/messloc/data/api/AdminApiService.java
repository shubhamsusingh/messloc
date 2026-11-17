
package com.example.messloc.data.api;

import com.example.messloc.data.model.admin.AdminApiResponse;
import com.example.messloc.data.model.admin.AdminHealth;
import com.example.messloc.data.model.admin.AdminMess;
import com.example.messloc.data.model.admin.AdminOwner;
import com.example.messloc.data.model.admin.CreateAdminRequest;

import java.util.List;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AdminApiService {

    @GET("admin/messes")
    Call<AdminApiResponse<List<AdminMess>>> getAllMesses(
            @Header("Authorization") String authorization
    );

    @GET("admin/messes/pending")
    Call<AdminApiResponse<List<AdminMess>>> getPendingMesses(
            @Header("Authorization") String authorization
    );

    @HTTP(method = "PATCH", path = "admin/messes/{id}/approve", hasBody = true)
    Call<AdminApiResponse<Object>> approveMess(
            @Header("Authorization") String authorization,
            @Path("id") String messId,
            @Body RequestBody body
    );

    @HTTP(method = "PATCH", path = "admin/messes/{id}/reject", hasBody = true)
    Call<AdminApiResponse<Object>> rejectMess(
            @Header("Authorization") String authorization,
            @Path("id") String messId,
            @Body RequestBody body
    );

    @GET("admin/owners/pending")
    Call<AdminApiResponse<List<AdminOwner>>> getPendingOwners(
            @Header("Authorization") String authorization
    );

    @HTTP(method = "PATCH", path = "admin/owners/{userId}/verify", hasBody = true)
    Call<AdminApiResponse<Object>> verifyOwner(
            @Header("Authorization") String authorization,
            @Path("userId") String userId,
            @Body RequestBody body
    );

    @HTTP(method = "PATCH", path = "admin/owners/{userId}/block", hasBody = true)
    Call<AdminApiResponse<Object>> blockOwner(
            @Header("Authorization") String authorization,
            @Path("userId") String userId,
            @Body RequestBody body
    );

    @POST("admin/create")
    Call<AdminApiResponse<AdminOwner>> createAdmin(
            @Header("Authorization") String authorization,
            @Body CreateAdminRequest request
    );

    @GET("admin/test")
    Call<AdminApiResponse<AdminHealth>> checkAdminAccess(
            @Header("Authorization") String authorization
    );
}
