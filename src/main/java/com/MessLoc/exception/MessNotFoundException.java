package com.MessLoc.exception;

public class MessNotFoundException extends RuntimeException {
    public MessNotFoundException(String message) {
        super(message);
    }
}
