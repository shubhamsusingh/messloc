package com.MessLoc.dto.menu;

import com.MessLoc.enums.MealType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemRequest {

    @NotNull(message = "Meal type is required. Allowed values: BREAKFAST, LUNCH, DINNER")
    private MealType mealType;

    @NotBlank(message = "Food name is required")
    @Size(max = 100, message = "Food name must not exceed 100 characters")
    private String foodName;

    @Size(max = 250, message = "Description must not exceed 250 characters")
    private String description;
}

