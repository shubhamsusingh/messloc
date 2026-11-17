
package com.example.messloc.data.model.subscription;

public class Subscription {

    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;

    private String messId;
    private String messName;

    private String planId;
    private String planName;
    private int durationDays;
    private double price;

    private String startDate;
    private String endDate;
    private String status;

    private String createdAt;
    private String updatedAt;
    private String cancelledAt;

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public String getMessId() {
        return messId;
    }

    public String getMessName() {
        return messName;
    }

    public String getPlanId() {
        return planId;
    }

    public String getPlanName() {
        return planName;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public double getPrice() {
        return price;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
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

    public String getCancelledAt() {
        return cancelledAt;
    }
}
