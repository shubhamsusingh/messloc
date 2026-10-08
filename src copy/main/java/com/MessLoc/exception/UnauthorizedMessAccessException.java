package com.MessLoc.exception;

public class UnauthorizedMessAccessException extends RuntimeException {
    public UnauthorizedMessAccessException(String message) {
        super(message);
    }
}
