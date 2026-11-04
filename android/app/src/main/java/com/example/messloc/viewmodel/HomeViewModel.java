package com.example.messloc.viewmodel;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.messloc.data.api.ApiClient;
import com.example.messloc.data.api.MessApiService;
import com.example.messloc.data.model.Mess;
import com.example.messloc.data.model.MessListResponse;
import com.example.messloc.data.repository.MessRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<List<Mess>> messes =
            new MutableLiveData<>();

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    private final MessRepository repository;

    public HomeViewModel() {
        MessApiService apiService =
                ApiClient.getInstance()
                        .create(MessApiService.class);

        repository = new MessRepository(apiService);
    }

    public MutableLiveData<List<Mess>> getMesses() {
        return messes;
    }

    public MutableLiveData<Boolean> getLoading() {
        return loading;
    }

    public MutableLiveData<String> getError() {
        return error;
    }


    public void fetchMesses() {

        Log.d("MESS_API", "fetchMesses() called");

        loading.setValue(true);
        error.setValue(null);

        repository.getAllMesses(
                new Callback<MessListResponse>() {

                    @Override
                    public void onResponse(
                            Call<MessListResponse> call,
                            Response<MessListResponse> response) {

                        loading.postValue(false);

                        Log.d("MESS_API", "onResponse() called");
                        Log.d("MESS_API", "HTTP Code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {

                            MessListResponse result = response.body();

                            Log.d("MESS_API", "Success: " + result.isSuccess());
                            Log.d("MESS_API", "Message: " + result.getMessage());
                            Log.d("MESS_API", "Data: " + result.getData());

                            if (result.isSuccess() && result.getData() != null) {
                                messes.postValue(result.getData());
                            } else {
                                error.postValue("No mess data received.");
                            }

                        } else {

                            Log.e("MESS_API", "API Error: " + response.code());

                            try {
                                if (response.errorBody() != null) {
                                    Log.e("MESS_API", "Error Body: "
                                            + response.errorBody().string());
                                }
                            } catch (Exception e) {
                                Log.e("MESS_API", "Error reading error body", e);
                            }

                            error.postValue("Unable to fetch messes.");
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<MessListResponse> call,
                            Throwable t) {

                        loading.postValue(false);

                        Log.e("MESS_API", "Request failed: " + t.getMessage(), t);

                        String errorMessage;
                        if (t instanceof java.net.SocketTimeoutException) {
                            errorMessage = "Server connection timed out. Please try again.";
                        } else if (t instanceof java.net.UnknownHostException) {
                            errorMessage = "No internet connection. Please check your network.";
                        } else if (t instanceof java.net.ConnectException) {
                            errorMessage = "Unable to connect to the server.";
                        } else {
                            errorMessage = t.getMessage() != null && !t.getMessage().isEmpty()
                                    ? t.getMessage()
                                    : "Request failed. Please try again.";
                        }

                        error.postValue(errorMessage);
                    }
                });
    }

}