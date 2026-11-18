package com.example.messloc.data.api;

import com.example.messloc.data.model.Mess;
import com.example.messloc.data.model.MessCreateResponse;
import com.example.messloc.data.model.MessListResponse;
import com.example.messloc.data.model.MessRegistrationRequest;
import com.example.messloc.data.model.OwnerSignupRequest;
import com.example.messloc.data.model.OwnerSignupResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import com.example.messloc.data.model.MessUpdateRequest;
public interface OwnerApiService {

    @POST("owner/messes")
    Call<MessCreateResponse> registerMess(
            @Header("Authorization") String token,
            @Body MessRegistrationRequest request
    );

    @GET("owner/messes")
    Call<MessListResponse> getOwnerMesses(
            @Header("Authorization") String token
    );



}