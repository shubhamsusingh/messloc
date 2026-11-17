
package com.example.messloc.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {

    private static final String PREF_NAME = "messloc_auth";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_EXPIRY = "token_expiry";
    private static final String KEY_ROLE = "user_role";

    private final SharedPreferences preferences;

    public TokenManager(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public void saveSession(String token, long expiresInMs) {
        long expiryTime = System.currentTimeMillis() + expiresInMs;

        preferences.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_EXPIRY, expiryTime)
                .apply();
    }

    // Save the logged-in user's role
    public void saveUserRole(String role) {
        preferences.edit()
                .putString(KEY_ROLE, role)
                .apply();
    }

    // Retrieve the logged-in user's role
    public String getUserRole() {
        return preferences.getString(KEY_ROLE, null);
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    public boolean hasValidSession() {
        String token = getToken();
        long expiryTime = preferences.getLong(KEY_EXPIRY, 0);

        return token != null
                && !token.isEmpty()
                && System.currentTimeMillis() < expiryTime;
    }

    public void clearSession() {
        preferences.edit().clear().apply();
    }
}
