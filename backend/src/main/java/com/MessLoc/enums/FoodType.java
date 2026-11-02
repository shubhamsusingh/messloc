package com.MessLoc.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FoodType {
    VEG,
    NON_VEG,
    BOTH;

    @JsonCreator
    public static FoodType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (FoodType ft : FoodType.values()) {
            if (ft.name().equalsIgnoreCase(normalized)) {
                return ft;
            }
        }
        throw new IllegalArgumentException("Invalid foodType: '" + value + "'. Allowed values: VEG, NON_VEG, BOTH");
    }
}
