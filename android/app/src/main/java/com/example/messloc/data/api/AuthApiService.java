package com.example.messloc.data.api;

import com.example.messloc.data.model.AuthResponse;
import com.example.messloc.data.model.LoginRequest;
import com.example.messloc.data.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import com.example.messloc.data.model.OwnerSignupRequest;
import com.example.messloc.data.model.OwnerSignupResponse;
public interface AuthApiService {

    @POST("auth/signup/user")
    Call<AuthResponse> register(
            @Body RegisterRequest request
    );

    @POST("auth/signin")
    Call<AuthResponse> login(
            @Body LoginRequest request
    );

    @POST("auth/signup/owner")
    Call<OwnerSignupResponse> registerOwner(
            @Body OwnerSignupRequest request
    );
}