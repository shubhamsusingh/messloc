
package com.example.messloc.data.model.subscription;

public class CreateSubscriptionRequest {

    private String messId;
    private String planId;

    public CreateSubscriptionRequest(String messId, String planId) {
        this.messId = messId;
        this.planId = planId;
    }

    public String getMessId() {
        return messId;
    }

    public String getPlanId() {
        return planId;
    }
}
