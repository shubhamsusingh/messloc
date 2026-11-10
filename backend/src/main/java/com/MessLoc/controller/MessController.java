package com.MessLoc.controller;

import com.MessLoc.dto.mess.CreateMessRequest;
import com.MessLoc.dto.mess.MessDetailsResponse;
import com.MessLoc.dto.mess.MessResponse;
import com.MessLoc.dto.mess.PublicMessResponse;
import com.MessLoc.dto.mess.UpdateMessRequest;
import com.MessLoc.dto.response.ApiResponse;
import com.MessLoc.entity.User;
import com.MessLoc.enums.FoodType;
import com.MessLoc.exception.UnauthorizedMessAccessException;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.service.MessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessController {

    private final MessService messService;
    private final UserRepository userRepository;

    // ==========================================
    // OWNER APIs
    // ==========================================

    @PostMapping({"/messes", "/owner/messes"})
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MessResponse>> createMess(
            @Valid @RequestBody CreateMessRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {

        if (userDetails == null && authentication != null && authentication.getPrincipal() instanceof CustomUserDetails cud) {
            userDetails = cud;
        }

        if (userDetails != null && userDetails.getUser() != null) {
            if (userDetails.getUser().getStatus() == com.MessLoc.enums.AccountStatus.PENDING) {
                throw new com.MessLoc.exception.OwnerPendingException("Your account is pending admin approval. Please wait for verification.");
            }
            if (userDetails.getUser().getStatus() != com.MessLoc.enums.AccountStatus.ACTIVE) {
                throw new com.MessLoc.exception.AccountInactiveException("Your account is currently inactive. Please contact MessLoc support.");
            }
        }

        String ownerId = resolveOwnerId(userDetails, authentication);
        MessResponse response = messService.createMess(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mess created successfully", response));
    }

    @GetMapping("/owner/messes")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<List<MessResponse>>> getOwnerMesses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {
        
        String ownerId = resolveOwnerId(userDetails, authentication);
        List<MessResponse> response = messService.getOwnerMesses(ownerId);
        return ResponseEntity.ok(ApiResponse.success("Messes fetched successfully", response));
    }

    @GetMapping("/owner/messes/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MessResponse>> getOwnerMess(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {
        
        String ownerId = resolveOwnerId(userDetails, authentication);
        MessResponse response = messService.getOwnerMess(id, ownerId);
        return ResponseEntity.ok(ApiResponse.success("Mess fetched successfully", response));
    }

    @PutMapping({"/owner/messes/{id}", "/messes/{id}"})
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MessResponse>> updateMess(
            @PathVariable String id,
            @Valid @RequestBody UpdateMessRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {
        
        String ownerId = resolveOwnerId(userDetails, authentication);
        MessResponse response = messService.updateMess(id, request, ownerId);
        return ResponseEntity.ok(ApiResponse.success("Mess updated successfully", response));
    }

    @DeleteMapping({"/owner/messes/{id}", "/messes/{id}"})
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteMess(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {
        
        String ownerId = resolveOwnerId(userDetails, authentication);
        messService.deleteMess(id, ownerId);
        return ResponseEntity.ok(ApiResponse.success("Mess deleted successfully", null));
    }

    @PatchMapping({"/owner/messes/{id}/activate", "/messes/{id}/activate"})
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> activateMess(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {
        
        String ownerId = resolveOwnerId(userDetails, authentication);
        messService.activateMess(id, ownerId);
        return ResponseEntity.ok(ApiResponse.success("Mess activated successfully", null));
    }

    @PatchMapping({"/owner/messes/{id}/deactivate", "/messes/{id}/deactivate"})
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> deactivateMess(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {
        
        String ownerId = resolveOwnerId(userDetails, authentication);
        messService.deactivateMess(id, ownerId);
        return ResponseEntity.ok(ApiResponse.success("Mess deactivated successfully", null));
    }

    // ==========================================
    // PUBLIC DISCOVERY APIs
    // ==========================================

    @GetMapping("/messes")
    public ResponseEntity<ApiResponse<List<PublicMessResponse>>> getPublicMesses(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String foodType) {

        FoodType parsedFoodType = null;
        if (foodType != null && !foodType.trim().isEmpty()) {
            parsedFoodType = FoodType.fromString(foodType);
        }

        List<PublicMessResponse> response = messService.getPublicMesses(city, parsedFoodType);
        return ResponseEntity.ok(ApiResponse.success("Messes fetched successfully", response));
    }

    @GetMapping("/messes/nearby")
    public ResponseEntity<ApiResponse<List<PublicMessResponse>>> getNearbyMesses(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false, defaultValue = "5.0") Double radius,
            @RequestParam(required = false) String foodType) {

        FoodType parsedFoodType = null;
        if (foodType != null && !foodType.trim().isEmpty()) {
            parsedFoodType = FoodType.fromString(foodType);
        }

        List<PublicMessResponse> response = messService.getNearbyMesses(latitude, longitude, radius, parsedFoodType);
        return ResponseEntity.ok(ApiResponse.success("Nearby messes fetched successfully", response));
    }

    @GetMapping("/messes/{id}")
    public ResponseEntity<ApiResponse<MessDetailsResponse>> getPublicMess(
            @PathVariable String id) {
        MessDetailsResponse response = messService.getPublicMess(id);
        return ResponseEntity.ok(ApiResponse.success("Mess fetched successfully", response));
    }

    // ==========================================
    // ADMIN APIs
    // ==========================================

    @GetMapping("/admin/messes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MessResponse>>> getAllMesses() {
        List<MessResponse> response = messService.getAllMesses();
        return ResponseEntity.ok(ApiResponse.success("All messes fetched successfully", response));
    }

    @GetMapping("/admin/messes/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MessResponse>>> getPendingMesses() {
        List<MessResponse> response = messService.getPendingMesses();
        return ResponseEntity.ok(ApiResponse.success("Pending messes fetched successfully", response));
    }

    @PatchMapping("/admin/messes/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveMess(
            @PathVariable String id) {
        messService.approveMess(id);
        return ResponseEntity.ok(ApiResponse.success("Mess approved successfully", null));
    }

    @PatchMapping("/admin/messes/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectMess(
            @PathVariable String id) {
        messService.rejectMess(id);
        return ResponseEntity.ok(ApiResponse.success("Mess rejected successfully", null));
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private String resolveOwnerId(CustomUserDetails userDetails, Authentication authentication) {
        if (userDetails != null && userDetails.getId() != null) {
            return userDetails.getId();
        }
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof CustomUserDetails cud && cud.getId() != null) {
                return cud.getId();
            }
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
