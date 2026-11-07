package com.example.messloc.data.repository;

import com.example.messloc.data.api.MessApiService;
import com.example.messloc.data.model.MessListResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessRepository {

    private final MessApiService apiService;

    public MessRepository(MessApiService apiService) {
        this.apiService = apiService;
    }

    public void getAllMesses(
            Callback<MessListResponse> callback) {

        Call<MessListResponse> call =
                apiService.getAllMesses();

        call.enqueue(callback);
    }
}