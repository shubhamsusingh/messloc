
package com.example.messloc.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.messloc.data.api.ApiClient;
import com.example.messloc.data.api.UserApiService;
import com.example.messloc.data.model.UserProfileResponse;
import com.example.messloc.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    private final UserApiService userApiService;
    private final TokenManager tokenManager;

    public UserRepository(Context context) {
        userApiService = ApiClient.getInstance()
                .create(UserApiService.class);

        tokenManager = new TokenManager(context);
    }

    public interface ProfileCallback {
        void onSuccess(UserProfileResponse response);
        void onError(String message);
    }

    public void getMyProfile(ProfileCallback callback) {

        if (!tokenManager.hasValidSession()) {
            callback.onError("Your session has expired. Please login again.");
            return;
        }

        String token = tokenManager.getToken();

        if (token == null || token.trim().isEmpty()) {
            callback.onError("Authentication token not found.");
            return;
        }

        userApiService.getMyProfile("Bearer " + token)
                .enqueue(new Callback<UserProfileResponse>() {


                    @Override
                    public void onResponse(
                            Call<UserProfileResponse> call,
                            Response<UserProfileResponse> response
                    ) {
                        Log.d("PROFILE_API", "HTTP Code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {

                            UserProfileResponse body = response.body();

                            Log.d("PROFILE_API", "Success: " + body.isSuccess());
                            Log.d("PROFILE_API", "Message: " + body.getMessage());
                            Log.d("PROFILE_API", "Message: " + body.getData().getAccountStatus());

                            if (body.isSuccess() && body.getData() != null) {
                                callback.onSuccess(body);
                            } else {
                                callback.onError(
                                        body.getMessage() != null
                                                ? body.getMessage()
                                                : "Profile data is missing."
                                );
                            }

                        } else {

                            String errorBody = "";

                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                }
                            } catch (Exception e) {
                                Log.e("PROFILE_API", "Error reading response", e);
                            }

                            Log.e("PROFILE_API", "HTTP Error: " + response.code());
                            Log.e("PROFILE_API", "Error Body: " + errorBody);

                            callback.onError(
                                    "Profile request failed. HTTP " + response.code()
                            );
                        }
                    }


                    @Override
                    public void onFailure(
                            Call<UserProfileResponse> call,
                            Throwable t
                    ) {
                        callback.onError(
                                t.getMessage() != null
                                        ? t.getMessage()
                                        : "Network error. Please try again."
                        );
                    }
                });
    }
}
