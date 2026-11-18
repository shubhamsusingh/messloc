package com.example.messloc.data.model.menu;

import java.util.List;

public class UpdateMenuRequest {

    private List<MenuItemRequest> items;

    public UpdateMenuRequest(List<MenuItemRequest> items) {
        this.items = items;
    }

    public List<MenuItemRequest> getItems() {
        return items;
    }
}