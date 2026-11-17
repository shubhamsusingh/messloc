package com.example.messloc.data.model.menu;

import java.util.List;

public class CreateMenuRequest {

    private String messId;
    private String menuDate;
    private List<MenuItemRequest> items;

    public CreateMenuRequest(
            String messId,
            String menuDate,
            List<MenuItemRequest> items
    ) {
        this.messId = messId;
        this.menuDate = menuDate;
        this.items = items;
    }

    public String getMessId() {
        return messId;
    }

    public String getMenuDate() {
        return menuDate;
    }

    public List<MenuItemRequest> getItems() {
        return items;
    }
}