package com.example.messloc.data.api;

import com.example.messloc.data.model.MessListResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

public interface MessApiService {

    @GET("messes")
    Call<MessListResponse> getAllMesses();
}