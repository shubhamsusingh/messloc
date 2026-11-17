
package com.example.messloc.data.api;

import com.example.messloc.data.model.UserProfileResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface UserApiService {

    @GET("auth/me")
    Call<UserProfileResponse> getMyProfile(
            @Header("Authorization") String authorization
    );
}
