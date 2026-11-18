package com.example.messloc.data.model.payment;

public class DemoPaymentResult {

    private final String transactionId;
    private final String status;
    private final String paymentMethod;
    private final double amount;
    private final String paymentDate;

    public DemoPaymentResult(
            String transactionId,
            String status,
            String paymentMethod,
            double amount,
            String paymentDate
    ) {
        this.transactionId = transactionId;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.paymentDate = paymentDate;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getStatus() {
        return status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public double getAmount() {
        return amount;
    }

    public String getPaymentDate() {
        return paymentDate;
    }
}