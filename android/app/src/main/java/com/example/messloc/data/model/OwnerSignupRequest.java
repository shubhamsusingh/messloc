package com.example.messloc.data.model;

public class OwnerSignupRequest {

    private String name;
    private String email;
    private String password;
    private String phone;
    private String businessName;
    private String businessDescription;
    private String businessAddress;
    private String fssaiLicenseNumber;

    public OwnerSignupRequest(
            String name,
            String email,
            String password,
            String phone,
            String businessName,
            String businessDescription,
            String businessAddress,
            String fssaiLicenseNumber
    ) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.businessName = businessName;
        this.businessDescription = businessDescription;
        this.businessAddress = businessAddress;
        this.fssaiLicenseNumber = fssaiLicenseNumber;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPhone() {
        return phone;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getBusinessDescription() {
        return businessDescription;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public String getFssaiLicenseNumber() {
        return fssaiLicenseNumber;
    }
}