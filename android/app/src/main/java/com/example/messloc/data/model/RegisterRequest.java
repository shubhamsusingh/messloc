package com.example.messloc.data.model;

public class RegisterRequest {

    private String name;
    private String email;
    private String phone;
    private String password;

    private String city;
    private String state;
    private String address;
    private Double latitude;
    private Double longitude;

    public RegisterRequest(
            String name,
            String email,
            String phone,
            String password,
            String city,
            String state,
            String address,
            Double latitude,
            Double longitude
    ) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.city = city;
        this.state = state;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getAddress() {
        return address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}