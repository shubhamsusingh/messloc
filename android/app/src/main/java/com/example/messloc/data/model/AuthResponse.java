package com.example.messloc.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    private boolean success;
    private String message;

    private String token;

    private String tokenType;

    private long expiresInMs;

    private User user;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }

    public User getUser() {
        return user;
    }
}