package com.example.messloc.data.model;

public class RegisterRequest {

    private String name;
    private String email;
    private String phone;
    private String password;

    public RegisterRequest(
            String name,
            String email,
            String phone,
            String password
    ) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }
}