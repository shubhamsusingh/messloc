
package com.example.messloc.data.api;

import com.example.messloc.data.model.MessCreateResponse;
import com.example.messloc.data.model.MessUpdateRequest;
import com.example.messloc.data.model.admin.AdminApiResponse;
import com.example.messloc.data.model.owner.OwnerMess;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface OwnerMessApiService {

    // Get all mess listings belonging to the authenticated owner
    @GET("owner/messes")
    Call<AdminApiResponse<List<OwnerMess>>> getMyMesses(
            @Header("Authorization") String authorization
    );

    // Get complete details of a specific owner mess
    @GET("owner/messes/{id}")
    Call<AdminApiResponse<OwnerMess>> getMyMessById(
            @Header("Authorization") String authorization,
            @Path("id") String messId
    );

    @PUT("owner/messes/{id}")
    Call<MessCreateResponse> updateMess(
            @Header("Authorization") String token,
            @Path("id") String messId,
            @Body MessUpdateRequest request
    );
}
