package com.MessLoc.dto.menu;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMenuResponse {
    private String menuId;
    private String messId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate menuDate;
    private boolean isUploaded;

    @Builder.Default
    private List<MenuItemResponse> breakfast = new ArrayList<>();

    @Builder.Default
    private List<MenuItemResponse> lunch = new ArrayList<>();

    @Builder.Default
    private List<MenuItemResponse> dinner = new ArrayList<>();
}

