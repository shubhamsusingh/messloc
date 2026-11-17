
package com.example.messloc.data.repository;

import android.content.Context;

import com.example.messloc.data.api.AdminApiService;
import com.example.messloc.data.api.ApiClient;
import com.example.messloc.data.model.admin.AdminApiResponse;
import com.example.messloc.data.model.admin.AdminHealth;
import com.example.messloc.data.model.admin.AdminMess;
import com.example.messloc.data.model.admin.AdminOwner;
import com.example.messloc.data.model.admin.CreateAdminRequest;
import com.example.messloc.utils.TokenManager;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;
public class AdminRepository {
    private static final RequestBody EMPTY_BODY =
            RequestBody.create(
                    MediaType.parse("application/json"),
                    new byte[0]
            );
    private final AdminApiService apiService;
    private final TokenManager tokenManager;

    public interface RepositoryCallback<T> {
        void onSuccess(T data, String message);
        void onError(String error);
    }

    public AdminRepository(Context context) {
        tokenManager = new TokenManager(context.getApplicationContext());
        apiService = ApiClient.getInstance().create(AdminApiService.class);
    }

    // Get all messes
    public void getAllMesses(RepositoryCallback<List<AdminMess>> callback) {
        execute(
                apiService.getAllMesses(getAuthorizationHeader()),
                callback
        );
    }

    // Get pending messes
    public void getPendingMesses(RepositoryCallback<List<AdminMess>> callback) {
        execute(
                apiService.getPendingMesses(getAuthorizationHeader()),
                callback
        );
    }

    // Approve a mess
    public void approveMess(
            String messId,
            RepositoryCallback<Object> callback
    ) {
        execute(
                apiService.approveMess(
                        getAuthorizationHeader(),
                        messId,
                        EMPTY_BODY
                ),
                callback
        );
    }

    // Reject a mess
    public void rejectMess(
            String messId,
            RepositoryCallback<Object> callback
    ) {
        execute(
                apiService.rejectMess(
                        getAuthorizationHeader(),
                        messId,
                        EMPTY_BODY
                ),
                callback
        );
    }

    // Get pending owners
    public void getPendingOwners(RepositoryCallback<List<AdminOwner>> callback) {
        execute(
                apiService.getPendingOwners(getAuthorizationHeader()),
                callback
        );
    }

    // Verify and activate an owner
    public void verifyOwner(
            String userId,
            RepositoryCallback<Object> callback
    ) {
        execute(
                apiService.verifyOwner(
                        getAuthorizationHeader(),
                        userId,
                        EMPTY_BODY
                ),
                callback
        );
    }

    // Block an owner
    public void blockOwner(
            String userId,
            RepositoryCallback<Object> callback
    ) {
        execute(
                apiService.blockOwner(
                        getAuthorizationHeader(),
                        userId,
                        EMPTY_BODY
                ),
                callback
        );
    }

    // Create another admin
    public void createAdmin(
            CreateAdminRequest request,
            RepositoryCallback<AdminOwner> callback
    ) {
        execute(
                apiService.createAdmin(getAuthorizationHeader(), request),
                callback
        );
    }

    // Check admin access
    public void checkAdminAccess(RepositoryCallback<AdminHealth> callback) {
        execute(
                apiService.checkAdminAccess(getAuthorizationHeader()),
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
            callback.onError("Session expired. Please log in again.");
            return;
        }

        call.enqueue(new Callback<AdminApiResponse<T>>() {
            @Override
            public void onResponse(
                    Call<AdminApiResponse<T>> call,
                    Response<AdminApiResponse<T>> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response));
                    return;
                }

                AdminApiResponse<T> body = response.body();

                if (body == null) {
                    callback.onError("Empty response from server.");
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

                callback.onSuccess(body.getData(), body.getMessage());
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
            return "Access denied. Admin permission is required.";
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
            // Use the HTTP status fallback below.
        }

        return "Request failed (HTTP " + code + ").";
    }
}
