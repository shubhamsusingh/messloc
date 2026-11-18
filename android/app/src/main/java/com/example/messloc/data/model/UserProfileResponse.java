
package com.example.messloc.data.model;

public class UserProfileResponse {

    private boolean success;
    private int status;
    private String message;
    private UserProfile data;
    private String timestamp;

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public UserProfile getData() {
        return data;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
