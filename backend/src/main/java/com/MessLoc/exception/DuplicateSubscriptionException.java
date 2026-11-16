package com.MessLoc.exception;

/**
 * Exception thrown when a user attempts to create multiple active subscriptions for the same mess.
 */
public class DuplicateSubscriptionException extends RuntimeException {
    public DuplicateSubscriptionException(String message) {
        super(message);
    }
}

