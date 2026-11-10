package com.MessLoc.controller;

import com.MessLoc.dto.request.CreateAdminRequest;
import com.MessLoc.dto.response.ApiResponse;
import com.MessLoc.dto.response.UserResponse;
import com.MessLoc.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for administrative management.
 * Protected by class-level RBAC requiring ROLE_ADMIN and active account status.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Admin-to-Admin Registration.
     * Only authenticated active administrators can create new admin accounts.
     * Mapped to /create-admin and aliased to /create for full backward compatibility.
     */
    @PostMapping(value = {"/create-admin", "/create"})
    public ResponseEntity<ApiResponse<UserResponse>> createAdmin(
            @Valid @RequestBody CreateAdminRequest request) {
        UserResponse response = adminService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "New admin account created successfully", response));
    }

    /**
     * Administrative RBAC test endpoint.
     */
    @GetMapping("/test")
    public ResponseEntity<ApiResponse<Map<String, String>>> testAdminAccess(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Access granted to ADMIN endpoint",
                Map.of(
                        "role", "ADMIN",
                        "username", authentication.getName(),
                        "message", "Welcome Admin! You have successfully verified access to protected administrative controls."
                )
        ));
    }

    /**
     * Lists all mess owners with PENDING verification status.
     */
    @GetMapping("/owners/pending")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getPendingOwners() {
        List<UserResponse> pendingOwners = adminService.getPendingOwners();
        return ResponseEntity.ok(ApiResponse.success("Pending owners fetched successfully", pendingOwners));
    }

    /**
     * Verifies and activates a pending owner account.
     */
    @PatchMapping("/owners/{userId}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyOwner(@PathVariable String userId) {
        adminService.verifyOwner(userId);
        return ResponseEntity.ok(ApiResponse.success("Owner verified and activated successfully", null));
    }

    /**
     * Blocks an owner account.
     */
    @PatchMapping("/owners/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> blockOwner(@PathVariable String userId) {
        adminService.blockOwner(userId);
        return ResponseEntity.ok(ApiResponse.success("Owner account blocked successfully", null));
    }
}
