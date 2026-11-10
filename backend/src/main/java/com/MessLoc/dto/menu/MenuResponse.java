package com.MessLoc.dto.menu;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuResponse {
    private String id;
    private String messId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate menuDate;
    private List<MenuItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
}

