package com.MessLoc.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MealType {
    BREAKFAST,
    LUNCH,
    DINNER;

    @JsonCreator
    public static MealType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (MealType mt : MealType.values()) {
            if (mt.name().equalsIgnoreCase(normalized)) {
                return mt;
            }
        }
        throw new IllegalArgumentException("Invalid mealType: '" + value + "'. Allowed values: BREAKFAST, LUNCH, DINNER");
    }
}

