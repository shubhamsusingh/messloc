package com.MessLoc.exception;

/**
 * Exception thrown when a requested subscription document is not found.
 */
public class SubscriptionNotFoundException extends RuntimeException {
    public SubscriptionNotFoundException(String message) {
        super(message);
    }
}

