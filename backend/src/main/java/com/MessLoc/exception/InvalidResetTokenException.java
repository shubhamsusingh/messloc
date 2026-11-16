package com.MessLoc.exception;

/**
 * Exception thrown when a submitted password reset token is missing, expired,
 * malformed, or already consumed.
 */
public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException(String message) {
        super(message);
    }
}

