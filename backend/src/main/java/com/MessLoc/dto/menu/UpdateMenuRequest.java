package com.MessLoc.dto.menu;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
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
public class UpdateMenuRequest {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate menuDate;

    @Valid
    @Builder.Default
    private List<MenuItemRequest> items = new ArrayList<>();
}

