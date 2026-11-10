package com.MessLoc.service;

import com.MessLoc.dto.menu.CreateMenuRequest;
import com.MessLoc.dto.menu.DailyMenuResponse;
import com.MessLoc.dto.menu.MenuItemRequest;
import com.MessLoc.dto.menu.MenuResponse;
import com.MessLoc.dto.menu.UpdateMenuRequest;
import com.MessLoc.dto.menu.WeeklyMenuResponse;

import java.time.LocalDate;

public interface MenuService {

    // OWNER Operations
    MenuResponse createMenu(CreateMenuRequest request, String ownerId);
    MenuResponse updateMenu(String menuId, UpdateMenuRequest request, String ownerId);
    MenuResponse addMenuItem(String menuId, MenuItemRequest request, String ownerId);
    MenuResponse updateMenuItem(String menuId, String itemId, MenuItemRequest request, String ownerId);
    MenuResponse deleteMenuItem(String menuId, String itemId, String ownerId);
    void deleteMenu(String menuId, String ownerId);
    WeeklyMenuResponse getOwnerWeeklyMenu(String messId, LocalDate startDate, String ownerId);

    // PUBLIC / USER Operations
    DailyMenuResponse getPublicTodayMenu(String messId);
    WeeklyMenuResponse getPublicWeeklyMenu(String messId, LocalDate startDate);
}

