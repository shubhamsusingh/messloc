
package com.example.messloc.data.model;

import java.util.List;

public class MessUpdateRequest {

    private String name;
    private String description;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private double latitude;
    private double longitude;
    private String contactNumber;
    private double monthlyPrice;
    private String foodType;
    private String openingTime;
    private String closingTime;
    private List<String> facilities;
    private String status;

    public MessUpdateRequest(
            String name,
            String description,
            String address,
            String city,
            String state,
            String pincode,
            double latitude,
            double longitude,
            String contactNumber,
            double monthlyPrice,
            String foodType,
            String openingTime,
            String closingTime,
            List<String> facilities,
            String status
    ) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.contactNumber = contactNumber;
        this.monthlyPrice = monthlyPrice;
        this.foodType = foodType;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.facilities = facilities;
        this.status = status;
    }
}
