package com.MessLoc.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MessStatus {
    ACTIVE,
    INACTIVE;

    @JsonCreator
    public static MessStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        for (MessStatus s : MessStatus.values()) {
            if (s.name().equalsIgnoreCase(normalized)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid mess status: '" + value + "'. Allowed values: ACTIVE, INACTIVE");
    }
}
