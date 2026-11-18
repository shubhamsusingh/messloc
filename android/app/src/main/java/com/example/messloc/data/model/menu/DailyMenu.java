package com.example.messloc.data.model.menu;

import java.util.List;

public class DailyMenu {

    private String menuId;
    private String messId;
    private String menuDate;

    private List<MenuItem> breakfast;
    private List<MenuItem> lunch;
    private List<MenuItem> dinner;

    public String getMenuId() {
        return menuId;
    }

    public String getMessId() {
        return messId;
    }

    public String getMenuDate() {
        return menuDate;
    }

    public List<MenuItem> getBreakfast() {
        return breakfast;
    }

    public List<MenuItem> getLunch() {
        return lunch;
    }

    public List<MenuItem> getDinner() {
        return dinner;
    }
}