package com.example.messloc.data.model.payment;

public class CheckoutDetails {

    private final String messId;
    private final String messName;
    private final String planId;
    private final String planName;
    private final double amount;
    private final int durationDays;

    public CheckoutDetails(
            String messId,
            String messName,
            String planId,
            String planName,
            double amount,
            int durationDays
    ) {
        this.messId = messId;
        this.messName = messName;
        this.planId = planId;
        this.planName = planName;
        this.amount = amount;
        this.durationDays = durationDays;
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

    public double getAmount() {
        return amount;
    }

    public int getDurationDays() {
        return durationDays;
    }
}