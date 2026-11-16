package com.MessLoc.exception;

/**
 * Exception thrown when a client exceeds permissible request thresholds.
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}

