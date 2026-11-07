package com.example.messloc.data.model;

import java.util.List;

public class MessListResponse {

    private boolean success;
    private int status;
    private String message;
    private List<Mess> data;

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<Mess> getData() {
        return data;
    }
}