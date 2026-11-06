package com.MessLoc.controller;

import com.MessLoc.dto.request.UpdateUserProfileRequest;
import com.MessLoc.dto.response.ApiResponse;
import com.MessLoc.dto.response.UserProfileResponse;
import com.MessLoc.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for user profile operations available to all authenticated roles.
 * Mapped to /api/users (plural) — distinct from /api/user (singular, USER-role-only).
 */
@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the profile of the currently authenticated user.
     * Accessible to all authenticated roles (USER, OWNER, ADMIN).
     * Identity is derived solely from the SecurityContext — not from request parameters.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(Authentication authentication) {
        UserProfileResponse profile = userService.getAuthenticatedUserProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", profile));
    }

    /**
     * Updates permitted profile fields of the currently authenticated user.
     * Accessible to all authenticated roles (USER, OWNER, ADMIN).
     * Identity is derived solely from the SecurityContext — not from request parameters.
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UserProfileResponse profile = userService.updateAuthenticatedUserProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", profile));
    }
}
