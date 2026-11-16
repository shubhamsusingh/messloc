package com.MessLoc.controller;

import com.MessLoc.dto.request.ChangePasswordRequest;
import com.MessLoc.dto.request.ForgotPasswordRequest;
import com.MessLoc.dto.request.OwnerSignupRequest;
import com.MessLoc.dto.request.ResetPasswordRequest;
import com.MessLoc.dto.request.SigninRequest;
import com.MessLoc.dto.request.UserSignupRequest;
import com.MessLoc.dto.response.ApiResponse;
import com.MessLoc.dto.response.AuthResponse;
import com.MessLoc.dto.response.UserResponse;
import com.MessLoc.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    
    @PostMapping("/signup/user")
    public ResponseEntity<AuthResponse> signupUser(@Valid @RequestBody UserSignupRequest request) {
        AuthResponse response = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    
    @PostMapping("/signup/owner")
    public ResponseEntity<AuthResponse> signupOwner(@Valid @RequestBody OwnerSignupRequest request) {
        AuthResponse response = authService.registerOwner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    
    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(@Valid @RequestBody SigninRequest request) {
        AuthResponse response = authService.signin(request);
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Not authenticated"));
        }
        UserResponse userResponse = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "User retrieved successfully", userResponse));
    }

    @PatchMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Not authenticated"));
        }
        authService.changePassword(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Password changed successfully", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "If the email is registered, password reset instructions have been sent.",
                null
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Password has been successfully reset. You can now log in with your new password.",
                null
        ));
    }
}
