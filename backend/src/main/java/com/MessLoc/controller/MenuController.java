package com.MessLoc.controller;

import com.MessLoc.dto.menu.CreateMenuRequest;
import com.MessLoc.dto.menu.DailyMenuResponse;
import com.MessLoc.dto.menu.MenuItemRequest;
import com.MessLoc.dto.menu.MenuResponse;
import com.MessLoc.dto.menu.UpdateMenuRequest;
import com.MessLoc.dto.menu.WeeklyMenuResponse;
import com.MessLoc.dto.response.ApiResponse;
import com.MessLoc.entity.User;
import com.MessLoc.exception.AccountInactiveException;
import com.MessLoc.exception.OwnerPendingException;
import com.MessLoc.exception.UnauthorizedMessAccessException;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final UserRepository userRepository;

    // ==========================================
    // OWNER MENU MANAGEMENT APIs
    // ==========================================

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MenuResponse>> createMenu(
            @Valid @RequestBody CreateMenuRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {

        String ownerId = resolveOwnerId(userDetails, authentication);
        MenuResponse response = menuService.createMenu(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Daily menu created successfully", response));
    }

    @PutMapping("/{menuId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MenuResponse>> updateMenu(
            @PathVariable String menuId,
            @Valid @RequestBody UpdateMenuRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {

        String ownerId = resolveOwnerId(userDetails, authentication);
        MenuResponse response = menuService.updateMenu(menuId, request, ownerId);
        return ResponseEntity.ok(ApiResponse.success("Daily menu updated successfully", response));
    }

    @PostMapping("/{menuId}/items")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MenuResponse>> addMenuItem(
            @PathVariable String menuId,
            @Valid @RequestBody MenuItemRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {

        String ownerId = resolveOwnerId(userDetails, authentication);
        MenuResponse response = menuService.addMenuItem(menuId, request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu item added successfully", response));
    }

    @PutMapping("/{menuId}/items/{itemId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MenuResponse>> updateMenuItem(
            @PathVariable String menuId,
            @PathVariable String itemId,
            @Valid @RequestBody MenuItemRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {

        String ownerId = resolveOwnerId(userDetails, authentication);
        MenuResponse response = menuService.updateMenuItem(menuId, itemId, request, ownerId);
        return ResponseEntity.ok(ApiResponse.success("Menu item updated successfully", response));
    }

    @DeleteMapping("/{menuId}/items/{itemId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MenuResponse>> deleteMenuItem(
            @PathVariable String menuId,
            @PathVariable String itemId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {

        String ownerId = resolveOwnerId(userDetails, authentication);
        MenuResponse response = menuService.deleteMenuItem(menuId, itemId, ownerId);
        return ResponseEntity.ok(ApiResponse.success("Menu item deleted successfully", response));
    }

    @DeleteMapping("/{menuId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(
            @PathVariable String menuId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {

        String ownerId = resolveOwnerId(userDetails, authentication);
        menuService.deleteMenu(menuId, ownerId);
        return ResponseEntity.ok(ApiResponse.success("Daily menu deleted successfully", null));
    }

    @GetMapping("/owner/mess/{messId}/weekly")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<WeeklyMenuResponse>> getOwnerWeeklyMenu(
            @PathVariable String messId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {

        String ownerId = resolveOwnerId(userDetails, authentication);
        WeeklyMenuResponse response = menuService.getOwnerWeeklyMenu(messId, startDate, ownerId);
        return ResponseEntity.ok(ApiResponse.success("Owner weekly menu fetched successfully", response));
    }

    // ==========================================
    // PUBLIC MENU VIEWING APIs
    // ==========================================

    @GetMapping("/mess/{messId}/today")
    public ResponseEntity<ApiResponse<DailyMenuResponse>> getPublicTodayMenu(
            @PathVariable String messId) {

        DailyMenuResponse response = menuService.getPublicTodayMenu(messId);
        String message = response.isUploaded() ? "Today's menu fetched successfully" : "Today's menu has not been uploaded yet";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @GetMapping("/mess/{messId}/weekly")
    public ResponseEntity<ApiResponse<WeeklyMenuResponse>> getPublicWeeklyMenu(
            @PathVariable String messId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {

        WeeklyMenuResponse response = menuService.getPublicWeeklyMenu(messId, startDate);
        return ResponseEntity.ok(ApiResponse.success("Weekly menu fetched successfully", response));
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private String resolveOwnerId(CustomUserDetails userDetails, Authentication authentication) {
        if (userDetails == null && authentication != null && authentication.getPrincipal() instanceof CustomUserDetails cud) {
            userDetails = cud;
        }

        if (userDetails != null && userDetails.getUser() != null) {
            if (userDetails.getUser().getStatus() == com.MessLoc.enums.AccountStatus.PENDING) {
                throw new OwnerPendingException("Your account is pending admin approval. Please wait for verification.");
            }
            if (userDetails.getUser().getStatus() != com.MessLoc.enums.AccountStatus.ACTIVE) {
                throw new AccountInactiveException("Your account is currently inactive. Please contact MessLoc support.");
            }
            if (userDetails.getId() != null) {
                return userDetails.getId();
            }
        }

        if (authentication != null) {
            String email = authentication.getName();
            if (email != null && !email.isBlank()) {
                return userRepository.findByEmail(email.toLowerCase().trim())
                        .map(User::getId)
                        .orElseThrow(() -> new UnauthorizedMessAccessException("Owner user not found with email: " + email));
            }
        }

        throw new UnauthorizedMessAccessException("Authentication required: owner is not authenticated");
    }
}

