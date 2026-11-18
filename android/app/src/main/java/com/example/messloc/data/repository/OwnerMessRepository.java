
package com.example.messloc.data.repository;

import android.content.Context;

import com.example.messloc.data.api.ApiClient;
import com.example.messloc.data.api.OwnerMessApiService;
import com.example.messloc.data.model.MessCreateResponse;
import com.example.messloc.data.model.MessUpdateRequest;
import com.example.messloc.data.model.admin.AdminApiResponse;
import com.example.messloc.data.model.owner.OwnerMess;
import com.example.messloc.utils.TokenManager;

import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OwnerMessRepository {

    private final OwnerMessApiService apiService;
    private final TokenManager tokenManager;

    public interface RepositoryCallback<T> {
        void onSuccess(T data, String message);
        void onError(String error);
    }

    public OwnerMessRepository(Context context) {

        tokenManager = new TokenManager(context.getApplicationContext());

        apiService = ApiClient.getInstance()
                .create(OwnerMessApiService.class);
    }

    // Get all mess listings belonging to the authenticated owner
    public void getMyMesses(
            RepositoryCallback<List<OwnerMess>> callback
    ) {
        execute(
                apiService.getMyMesses(getAuthorizationHeader()),
                callback
        );
    }

    public void updateMess(
            String messId,
            MessUpdateRequest request,
            RepositoryCallback<MessCreateResponse> callback
    ) {
        String authorization = getAuthorizationHeader();

        if (authorization == null) {
            callback.onError("Session expired. Please log in again.");
            return;
        }

        apiService.updateMess(
                authorization,
                messId,
                request
        ).enqueue(new Callback<MessCreateResponse>() {

            @Override
            public void onResponse(
                    Call<MessCreateResponse> call,
                    Response<MessCreateResponse> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(
                            "Unable to update mess. HTTP " + response.code()
                    );
                    return;
                }

                MessCreateResponse body = response.body();

                if (body == null) {
                    callback.onError("Empty response from server.");
                    return;
                }

                if (!body.isSuccess()) {
                    callback.onError(
                            body.getMessage() != null
                                    ? body.getMessage()
                                    : "Mess update failed."
                    );
                    return;
                }

                callback.onSuccess(body, body.getMessage());
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

    // Get complete details of a specific mess
    public void getMyMessById(
            String messId,
            RepositoryCallback<OwnerMess> callback
    ) {
        if (messId == null || messId.trim().isEmpty()) {
            callback.onError("Invalid mess ID.");
            return;
        }

        execute(
                apiService.getMyMessById(
                        getAuthorizationHeader(),
                        messId
                ),
                callback
        );
    }

    // Build Authorization header
    private String getAuthorizationHeader() {

        String token = tokenManager.getToken();

        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        return "Bearer " + token;
    }

    // Common Retrofit response handler
    private <T> void execute(
            Call<AdminApiResponse<T>> call,
            RepositoryCallback<T> callback
    ) {

        String authorization = getAuthorizationHeader();

        if (authorization == null) {
            callback.onError(
                    "Session expired. Please log in again."
            );
            return;
        }

        call.enqueue(new Callback<AdminApiResponse<T>>() {

            @Override
            public void onResponse(
                    Call<AdminApiResponse<T>> call,
                    Response<AdminApiResponse<T>> response
            ) {

                if (!response.isSuccessful()) {
                    callback.onError(
                            extractErrorMessage(response)
                    );
                    return;
                }

                AdminApiResponse<T> body = response.body();

                if (body == null) {
                    callback.onError(
                            "Empty response from server."
                    );
                    return;
                }

                if (!body.isSuccess()) {
                    callback.onError(
                            body.getMessage() != null
                                    ? body.getMessage()
                                    : "Request failed."
                    );
                    return;
                }

                callback.onSuccess(
                        body.getData(),
                        body.getMessage()
                );
            }

            @Override
            public void onFailure(
                    Call<AdminApiResponse<T>> call,
                    Throwable throwable
            ) {

                String message = throwable.getMessage();

                if (message == null || message.trim().isEmpty()) {
                    message = "Unable to connect to the server.";
                }

                callback.onError(message);
            }
        });
    }

    // Extract backend error message
    private String extractErrorMessage(Response<?> response) {

        int code = response.code();

        if (code == 401) {
            return "Session expired or invalid token. Please log in again.";
        }

        if (code == 403) {
            return "You are not authorized to manage this mess.";
        }

        try {
            if (response.errorBody() != null) {

                String errorJson = response.errorBody().string();

                JSONObject jsonObject = new JSONObject(errorJson);

                String message = jsonObject.optString("message");

                if (!message.isEmpty()) {
                    return message;
                }
            }
        } catch (Exception ignored) {
            // Use HTTP status fallback below.
        }

        return "Request failed (HTTP " + code + ").";
    }
}
