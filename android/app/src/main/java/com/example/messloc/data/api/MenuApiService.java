package com.example.messloc.data.api;

import com.example.messloc.data.model.admin.AdminApiResponse;
import com.example.messloc.data.model.menu.CreateMenuRequest;
import com.example.messloc.data.model.menu.DailyMenu;
import com.example.messloc.data.model.menu.MenuItemRequest;
import com.example.messloc.data.model.menu.UpdateMenuRequest;
import com.example.messloc.data.model.menu.WeeklyMenuResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MenuApiService {

    // Create menu
    @POST("menus")
    Call<AdminApiResponse<DailyMenu>> createMenu(
            @Header("Authorization") String token,
            @Body CreateMenuRequest request
    );

    // Update complete menu
    @PUT("menus/{menuId}")
    Call<AdminApiResponse<DailyMenu>> updateMenu(
            @Header("Authorization") String token,
            @Path("menuId") String menuId,
            @Body UpdateMenuRequest request
    );

    // Add item to existing menu
    @POST("menus/{menuId}/items")
    Call<AdminApiResponse<DailyMenu>> addMenuItem(
            @Header("Authorization") String token,
            @Path("menuId") String menuId,
            @Body MenuItemRequest request
    );

    // Update existing menu item
    @PUT("menus/{menuId}/items/{itemId}")
    Call<AdminApiResponse<DailyMenu>> updateMenuItem(
            @Header("Authorization") String token,
            @Path("menuId") String menuId,
            @Path("itemId") String itemId,
            @Body MenuItemRequest request
    );

    // Delete menu item
    @DELETE("menus/{menuId}/items/{itemId}")
    Call<AdminApiResponse<DailyMenu>> deleteMenuItem(
            @Header("Authorization") String token,
            @Path("menuId") String menuId,
            @Path("itemId") String itemId
    );

    // Delete complete menu
    @DELETE("menus/{menuId}")
    Call<AdminApiResponse<Void>> deleteMenu(
            @Header("Authorization") String token,
            @Path("menuId") String menuId
    );

    // Owner weekly menu
    @GET("menus/owner/mess/{messId}/weekly")
    Call<AdminApiResponse<WeeklyMenuResponse>> getOwnerWeeklyMenu(
            @Header("Authorization") String token,
            @Path("messId") String messId,
            @Query("startDate") String startDate
    );

    // Public today's menu
    @GET("menus/mess/{messId}/today")
    Call<AdminApiResponse<DailyMenu>> getTodayMenu(
            @Path("messId") String messId
    );

    // Public weekly menu
    @GET("menus/mess/{messId}/weekly")
    Call<AdminApiResponse<WeeklyMenuResponse>> getPublicWeeklyMenu(
            @Path("messId") String messId,
            @Query("startDate") String startDate
    );
}