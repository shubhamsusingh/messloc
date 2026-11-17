package com.example.messloc.data.model.admin;

public class AdminApiResponse<T> {

    private boolean success;
    private int status;
    private String message;
    private T data;
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

    public T getData() {
        return data;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
