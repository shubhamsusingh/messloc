package com.example.messloc.data.repository;

import android.content.Context;

import com.example.messloc.data.api.ApiClient;
import com.example.messloc.data.api.AuthApiService;
import com.example.messloc.data.model.AuthResponse;
import com.example.messloc.data.model.LoginRequest;
import com.example.messloc.data.model.RegisterRequest;
import com.example.messloc.utils.TokenManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;
public class AuthRepository {

    private final AuthApiService apiService;
    private final TokenManager tokenManager;

    public AuthRepository(Context context) {

        apiService = ApiClient.getInstance()
                .create(AuthApiService.class);

        tokenManager = new TokenManager(context);
    }

    public interface AuthCallback {

        void onSuccess(AuthResponse response);

        void onError(String message);
    }

    // REGISTER API
    public void register(
            RegisterRequest request,
            AuthCallback callback
    ) {

        apiService.register(request).enqueue(new Callback<AuthResponse>() {

            @Override
            public void onResponse(
                    Call<AuthResponse> call,
                    Response<AuthResponse> response
            ) {
                Log.d("AUTH_API", "REGISTER RESPONSE");
                Log.d("AUTH_API", "URL: " + call.request().url());
                Log.d("AUTH_API", "Status Code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {

                    AuthResponse authResponse = response.body();

                    // Save token if registration returns one
                    if (authResponse.getToken() != null) {

                        tokenManager.saveSession(
                                authResponse.getToken(),
                                authResponse.getExpiresInMs()
                        );
                    }

                    callback.onSuccess(authResponse);

                } else {

                    callback.onError(
                            extractErrorMessage(response)
                    );
                }
            }

            @Override
            public void onFailure(
                    Call<AuthResponse> call,
                    Throwable t
            ) {

                callback.onError(
                        getNetworkError(t)
                );
            }
        });
    }

    // LOGIN API
    public void login(
            LoginRequest request,
            AuthCallback callback
    ) {

        apiService.login(request).enqueue(new Callback<AuthResponse>() {

            @Override
            public void onResponse(
                    Call<AuthResponse> call,
                    Response<AuthResponse> response
            ) {

                if (response.isSuccessful() && response.body() != null) {

                    AuthResponse authResponse = response.body();

                    String token = authResponse.getToken();

                    if (token != null && !token.isEmpty()) {

                        tokenManager.saveSession(
                                authResponse.getToken(),
                                authResponse.getExpiresInMs()
                        );

                        callback.onSuccess(authResponse);

                    } else {

                        callback.onError(
                                "Authentication token was not received."
                        );
                    }

                } else {

                    callback.onError(
                            extractErrorMessage(response)
                    );
                }
            }

            @Override
            public void onFailure(
                    Call<AuthResponse> call,
                    Throwable t
            ) {

                callback.onError(
                        getNetworkError(t)
                );
            }
        });
    }

    // EXTRACT BACKEND ERROR MESSAGE
    private String extractErrorMessage(
            Response<?> response
    ) {

        int statusCode = response.code();

        try {

            if (response.errorBody() != null) {

                String errorBody = response.errorBody().string();

                JSONObject jsonObject = new JSONObject(errorBody);

                // Extract general message
                String message = jsonObject.optString("message");

                // Handle field-level validation errors
                JSONObject errors = jsonObject.optJSONObject("errors");

                if (errors != null && errors.length() > 0) {

                    StringBuilder errorBuilder = new StringBuilder();

                    Iterator<String> keys = errors.keys();

                    while (keys.hasNext()) {

                        String field = keys.next();

                        String fieldError = errors.optString(field);

                        errorBuilder.append(field)
                                .append(": ")
                                .append(fieldError);

                        if (keys.hasNext()) {
                            errorBuilder.append("\n");
                        }
                    }

                    return errorBuilder.toString();
                }

                // Handle validation errors represented as an array
                JSONArray fieldErrors =
                        jsonObject.optJSONArray("errors");

                if (fieldErrors != null && fieldErrors.length() > 0) {

                    StringBuilder errorBuilder = new StringBuilder();

                    for (int i = 0; i < fieldErrors.length(); i++) {

                        errorBuilder.append(fieldErrors.optString(i));

                        if (i < fieldErrors.length() - 1) {
                            errorBuilder.append("\n");
                        }
                    }

                    return errorBuilder.toString();
                }

                if (!message.isEmpty()) {
                    return message;
                }
            }

        } catch (IOException | JSONException e) {

            e.printStackTrace();
        }

        // Fallback messages
        switch (statusCode) {

            case 400:
                return "Please check your input details.";

            case 401:
                return "Invalid email or password.";

            case 403:
                return "You don't have permission to perform this action.";

            case 404:
                return "Requested resource was not found.";

            case 409:
                return "An account with this email already exists.";

            case 422:
                return "Please check the submitted information.";

            case 500:
                return "Something went wrong. Please try again later.";

            default:
                return "Request failed. Please try again.";
        }
    }

    // NETWORK ERROR HANDLING
    private String getNetworkError(Throwable t) {

        if (t instanceof java.net.UnknownHostException) {
            return "No internet connection. Please check your network.";
        }

        if (t instanceof java.net.SocketTimeoutException) {
            return "Connection timed out. Please try again.";
        }

        if (t instanceof java.net.ConnectException) {
            return "Unable to connect to the server.";
        }

        return "Something went wrong. Please try again.";
    }
}