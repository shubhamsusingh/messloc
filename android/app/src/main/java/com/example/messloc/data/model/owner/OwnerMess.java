
package com.example.messloc.data.model.owner;

import java.util.List;

public class OwnerMess {

    private String id;
    private String ownerId;

    private String name;
    private String description;

    private String address;
    private String city;
    private String state;
    private String pincode;

    private Double latitude;
    private Double longitude;

    private String contactNumber;
    private Double monthlyPrice;

    private String foodType;
    private String openingTime;
    private String closingTime;

    private List<String> facilities;

    private String verificationStatus;
    private String status;

    private String createdAt;
    private String updatedAt;

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPincode() {
        return pincode;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public Double getMonthlyPrice() {
        return monthlyPrice;
    }

    public String getFoodType() {
        return foodType;
    }

    public String getOpeningTime() {
        return openingTime;
    }

    public String getClosingTime() {
        return closingTime;
    }

    public List<String> getFacilities() {
        return facilities;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
