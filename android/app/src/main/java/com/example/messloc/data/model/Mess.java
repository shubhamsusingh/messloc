package com.example.messloc.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Mess {

    private String id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String description;
    private String contactNumber;
    private String foodType;
    private String openingTime;
    private String closingTime;
    private double latitude;
    private double longitude;
    private double monthlyPrice;
    private List<String> facilities;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPincode() { return pincode; }
    public String getDescription() { return description; }
    public String getContactNumber() { return contactNumber; }
    public String getFoodType() { return foodType; }
    public String getOpeningTime() { return openingTime; }
    public String getClosingTime() { return closingTime; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getMonthlyPrice() { return monthlyPrice; }
    public List<String> getFacilities() { return facilities; }
}