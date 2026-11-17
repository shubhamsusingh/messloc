package com.example.messloc.data.model.menu;

public class MenuItemRequest {

    private String mealType;
    private String foodName;
    private String description;

    public MenuItemRequest(String mealType, String foodName, String description) {
        this.mealType = mealType;
        this.foodName = foodName;
        this.description = description;
    }

    public String getMealType() {
        return mealType;
    }

    public String getFoodName() {
        return foodName;
    }

    public String getDescription() {
        return description;
    }
}