package com.MessLoc.service;

import com.MessLoc.dto.subscription.CreateSubscriptionRequest;
import com.MessLoc.dto.subscription.SubscriptionResponse;
import com.MessLoc.enums.SubscriptionStatus;

import java.util.List;

public interface SubscriptionService {

    /**
     * Creates a new mess subscription for the authenticated user.
     */
    SubscriptionResponse createSubscription(CreateSubscriptionRequest request, String userId);

    /**
     * Retrieves all subscriptions belonging to the authenticated user with optional status filter.
     */
    List<SubscriptionResponse> getUserSubscriptions(String userId, SubscriptionStatus status);

    /**
     * Retrieves detailed information of a subscription. Accessible by subscriber or admin.
     */
    SubscriptionResponse getSubscriptionById(String subscriptionId, String currentUserId, boolean isAdmin);

    /**
     * Cancels an active subscription belonging to the authenticated user.
     */
    SubscriptionResponse cancelSubscription(String subscriptionId, String userId);

    /**
     * Retrieves subscriptions for a mess owned by the authenticated mess owner.
     */
    List<SubscriptionResponse> getOwnerMessSubscriptions(String messId, String ownerId, SubscriptionStatus status);

    /**
     * Administrative endpoint to list all platform subscriptions with optional status filter.
     */
    List<SubscriptionResponse> getAllSubscriptions(SubscriptionStatus status);
}

