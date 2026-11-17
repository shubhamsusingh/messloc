package com.example.messloc.data.model.menu;

import java.util.List;

public class WeeklyMenuResponse {

    private String messId;
    private String messName;
    private String startDate;
    private String endDate;
    private List<DailyMenu> menus;

    public String getMessId() {
        return messId;
    }

    public String getMessName() {
        return messName;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public List<DailyMenu> getMenus() {
        return menus;
    }
}