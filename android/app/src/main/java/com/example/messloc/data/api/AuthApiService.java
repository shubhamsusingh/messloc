package com.example.messloc.data.api;

import com.example.messloc.data.model.AuthResponse;
import com.example.messloc.data.model.LoginRequest;
import com.example.messloc.data.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("auth/signup/user")
    Call<AuthResponse> register(
            @Body RegisterRequest request
    );

    @POST("auth/signin")
    Call<AuthResponse> login(
            @Body LoginRequest request
    );
}