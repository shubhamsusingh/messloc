package com.MessLoc.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum VerificationStatus {
    PENDING,
    APPROVED,
    REJECTED;

    @JsonCreator
    public static VerificationStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        for (VerificationStatus s : VerificationStatus.values()) {
            if (s.name().equalsIgnoreCase(normalized)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid verification status: '" + value + "'. Allowed values: PENDING, APPROVED, REJECTED");
    }
}
