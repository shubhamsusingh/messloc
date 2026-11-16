package com.MessLoc.enums;

/**
 * Enumeration representing the lifecycle statuses of a mess subscription.
 */
public enum SubscriptionStatus {
    ACTIVE,
    CANCELLED,
    EXPIRED;

    public static SubscriptionStatus fromString(String status) {
        if (status == null) {
            return null;
        }
        for (SubscriptionStatus s : values()) {
            if (s.name().equalsIgnoreCase(status.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid subscription status: " + status + ". Allowed values: ACTIVE, CANCELLED, EXPIRED");
    }
}

