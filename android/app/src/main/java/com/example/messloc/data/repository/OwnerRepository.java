package com.example.messloc.data.repository;

import android.util.Log;

import com.example.messloc.data.api.OwnerApiService;
import com.example.messloc.data.model.MessCreateResponse;
import com.example.messloc.data.model.MessRegistrationRequest;
import com.example.messloc.data.model.MessUpdateRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OwnerRepository {

    private final OwnerApiService apiService;

    public OwnerRepository(OwnerApiService apiService) {
        this.apiService = apiService;
    }

    public void registerMess(
            String token,
            MessRegistrationRequest request,
            OwnerCallback callback
    ) {
        apiService.registerMess(token, request)
                .enqueue(new Callback<MessCreateResponse>() {

                    @Override
                    public void onResponse(
                            Call<MessCreateResponse> call,
                            Response<MessCreateResponse> response
                    ) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            callback.onSuccess(response.body());

                        } else {
                            callback.onError(
                                    getErrorMessage(response)
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<MessCreateResponse> call,
                            Throwable t
                    ) {
                        callback.onError(
                                t.getMessage() != null
                                        ? t.getMessage()
                                        : "Network error"
                        );
                    }
                });
    }


    private String getErrorMessage(
            Response<MessCreateResponse> response
    ) {
        Log.d("Register_Mess",response.toString());
        switch (response.code()) {
            case 400:
                return "Please check the entered details.";
            case 401:
                return "Session expired. Please login again.";
            case 403:
                return "Your owner account is not verified.";
            case 409:
                return "This mess is already registered.";
            default:
                return "Unable to register mess. Error: "
                        + response.code();
        }
    }

    public interface OwnerCallback {
        void onSuccess(MessCreateResponse response);
        void onError(String message);
    }
}