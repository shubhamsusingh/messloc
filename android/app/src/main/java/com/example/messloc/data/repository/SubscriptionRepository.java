
package com.example.messloc.data.repository;

import android.content.Context;

import com.example.messloc.data.api.ApiClient;
import com.example.messloc.data.api.SubscriptionApiService;
import com.example.messloc.data.model.admin.AdminApiResponse;
import com.example.messloc.data.model.subscription.CreateSubscriptionRequest;
import com.example.messloc.data.model.subscription.Subscription;
import com.example.messloc.utils.TokenManager;

import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionRepository {

    private final SubscriptionApiService apiService;
    private final TokenManager tokenManager;

    public interface RepositoryCallback<T> {
        void onSuccess(T data, String message);
        void onError(String error);
    }

    public SubscriptionRepository(Context context) {
        tokenManager = new TokenManager(
                context.getApplicationContext()
        );

        apiService = ApiClient.getInstance()
                .create(SubscriptionApiService.class);
    }

    // Create subscription
    public void createSubscription(
            CreateSubscriptionRequest request,
            RepositoryCallback<Subscription> callback
    ) {
        execute(
                apiService.createSubscription(
                        getAuthorizationHeader(),
                        request
                ),
                callback
        );
    }

    // Get logged-in user's subscriptions
    public void getMySubscriptions(
            RepositoryCallback<List<Subscription>> callback
    ) {
        execute(
                apiService.getMySubscriptions(
                        getAuthorizationHeader()
                ),
                callback
        );
    }

    // Cancel subscription
    public void cancelSubscription(
            String subscriptionId,
            RepositoryCallback<Subscription> callback
    ) {
        execute(
                apiService.cancelSubscription(
                        getAuthorizationHeader(),
                        subscriptionId
                ),
                callback
        );
    }

    // Authorization header
    private String getAuthorizationHeader() {
        String token = tokenManager.getToken();

        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        return "Bearer " + token;
    }

    // Common response handler
    private <T> void execute(
            Call<AdminApiResponse<T>> call,
            RepositoryCallback<T> callback
    ) {
        if (getAuthorizationHeader() == null) {
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

    // Extract backend error messages
    private String extractErrorMessage(Response<?> response) {
        int code = response.code();

        if (code == 401) {
            return "Session expired or invalid token. Please log in again.";
        }

        if (code == 403) {
            return "You are not authorized to manage this subscription.";
        }

        if (code == 404) {
            return "Subscription or mess not found.";
        }

        if (code == 409) {
            return "You already have an active subscription to this mess.";
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
            // Use HTTP status fallback.
        }

        return "Request failed (HTTP " + code + ").";
    }
}
