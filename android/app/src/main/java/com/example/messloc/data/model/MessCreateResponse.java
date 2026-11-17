package com.example.messloc.data.model;

public class MessCreateResponse {

    private boolean success;
    private int status;
    private String message;
    private Mess data;

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Mess getData() {
        return data;
    }
}