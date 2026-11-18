
package com.example.messloc.data.repository;

import android.content.Context;

import com.example.messloc.data.api.ApiClient;
import com.example.messloc.data.api.MenuApiService;
import com.example.messloc.data.model.admin.AdminApiResponse;
import com.example.messloc.data.model.menu.CreateMenuRequest;
import com.example.messloc.data.model.menu.DailyMenu;
import com.example.messloc.data.model.menu.MenuItemRequest;
import com.example.messloc.data.model.menu.UpdateMenuRequest;
import com.example.messloc.data.model.menu.WeeklyMenuResponse;
import com.example.messloc.utils.TokenManager;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuRepository {

    private final MenuApiService apiService;
    private final TokenManager tokenManager;

    public interface RepositoryCallback<T> {
        void onSuccess(T data, String message);
        void onError(String error);
    }

    public MenuRepository(Context context) {
        tokenManager = new TokenManager(context.getApplicationContext());

        apiService = ApiClient.getInstance()
                .create(MenuApiService.class);
    }

    // Create a menu for a mess and date
    public void createMenu(
            CreateMenuRequest request,
            RepositoryCallback<DailyMenu> callback
    ) {
        execute(
                apiService.createMenu(getAuthorizationHeader(), request),
                callback,
                true
        );
    }

    // Update the complete menu
    public void updateMenu(
            String menuId,
            UpdateMenuRequest request,
            RepositoryCallback<DailyMenu> callback
    ) {
        execute(
                apiService.updateMenu(
                        getAuthorizationHeader(),
                        menuId,
                        request
                ),
                callback,
                true
        );
    }

    // Add a menu item
    public void addMenuItem(
            String menuId,
            MenuItemRequest request,
            RepositoryCallback<DailyMenu> callback
    ) {
        execute(
                apiService.addMenuItem(
                        getAuthorizationHeader(),
                        menuId,
                        request
                ),
                callback,
                true
        );
    }

    // Update a menu item
    public void updateMenuItem(
            String menuId,
            String itemId,
            MenuItemRequest request,
            RepositoryCallback<DailyMenu> callback
    ) {
        execute(
                apiService.updateMenuItem(
                        getAuthorizationHeader(),
                        menuId,
                        itemId,
                        request
                ),
                callback,
                true
        );
    }

    // Delete a menu item
    public void deleteMenuItem(
            String menuId,
            String itemId,
            RepositoryCallback<DailyMenu> callback
    ) {
        execute(
                apiService.deleteMenuItem(
                        getAuthorizationHeader(),
                        menuId,
                        itemId
                ),
                callback,
                true
        );
    }

    // Delete a complete menu
    public void deleteMenu(
            String menuId,
            RepositoryCallback<Void> callback
    ) {
        execute(
                apiService.deleteMenu(
                        getAuthorizationHeader(),
                        menuId
                ),
                callback,
                true
        );
    }

    // Get weekly menu for authenticated owner
    public void getOwnerWeeklyMenu(
            String messId,
            String startDate,
            RepositoryCallback<WeeklyMenuResponse> callback
    ) {
        execute(
                apiService.getOwnerWeeklyMenu(
                        getAuthorizationHeader(),
                        messId,
                        startDate
                ),
                callback,
                true
        );
    }

    // Get today's public menu (no authentication)
    public void getTodayMenu(
            String messId,
            RepositoryCallback<DailyMenu> callback
    ) {
        execute(
                apiService.getTodayMenu(messId),
                callback,
                false
        );
    }

    // Get public weekly menu (no authentication)
    public void getPublicWeeklyMenu(
            String messId,
            String startDate,
            RepositoryCallback<WeeklyMenuResponse> callback
    ) {
        execute(
                apiService.getPublicWeeklyMenu(messId, startDate),
                callback,
                false
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
            RepositoryCallback<T> callback,
            boolean requiresAuthentication
    ) {

        if (requiresAuthentication &&
                getAuthorizationHeader() == null) {
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
            return "You are not authorized to manage this menu.";
        }

        if (code == 409) {
            return "A menu already exists for this date.";
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
