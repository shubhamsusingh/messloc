package com.MessLoc.controller;

import com.MessLoc.dto.response.ApiResponse;
import com.MessLoc.dto.subscription.CreateSubscriptionRequest;
import com.MessLoc.dto.subscription.SubscriptionResponse;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.SubscriptionStatus;
import com.MessLoc.exception.AccountBlockedException;
import com.MessLoc.exception.AccountInactiveException;
import com.MessLoc.exception.OwnerPendingException;
import com.MessLoc.exception.ResourceNotFoundException;
import com.MessLoc.exception.UnauthorizedMessAccessException;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Mess Subscription operations across USER, OWNER, and ADMIN roles.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    // ==========================================
    // USER SUBSCRIPTION APIs
    // ==========================================

    /**
     * Subscribe to an approved and active mess.
     */
    @PostMapping("/subscriptions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {

        String userId = resolveUserId(userDetails, authentication);
        SubscriptionResponse response = subscriptionService.createSubscription(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Subscription created successfully", response));
    }

    /**
     * Get all subscriptions belonging to the authenticated customer.
     */
    @GetMapping("/subscriptions/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getMySubscriptions(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {

        String userId = resolveUserId(userDetails, authentication);
        SubscriptionStatus parsedStatus = parseStatus(status);
        List<SubscriptionResponse> response = subscriptionService.getUserSubscriptions(userId, parsedStatus);
        return ResponseEntity.ok(ApiResponse.success("Subscriptions fetched successfully", response));
    }

    /**
     * Get details of a specific subscription. Accessible by the subscriber or an administrator.
     */
    @GetMapping("/subscriptions/{subscriptionId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscriptionById(
            @PathVariable String subscriptionId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {

        String currentUserId = resolveUserId(userDetails, authentication);
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        SubscriptionResponse response = subscriptionService.getSubscriptionById(subscriptionId, currentUserId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Subscription retrieved successfully", response));
    }

    /**
     * Cancel an active subscription belonging to the authenticated customer.
     */
    @PatchMapping("/subscriptions/{subscriptionId}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancelSubscription(
            @PathVariable String subscriptionId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {

        String userId = resolveUserId(userDetails, authentication);
        SubscriptionResponse response = subscriptionService.cancelSubscription(subscriptionId, userId);
        return ResponseEntity.ok(ApiResponse.success("Subscription cancelled successfully", response));
    }

    // ==========================================
    // OWNER SUBSCRIPTION APIs
    // ==========================================

    /**
     * View subscriptions associated with a mess owned by the authenticated mess owner.
     */
    @GetMapping("/subscriptions/owner/mess/{messId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getOwnerMessSubscriptions(
            @PathVariable String messId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication) {

        String ownerId = resolveOwnerId(userDetails, authentication);
        SubscriptionStatus parsedStatus = parseStatus(status);
        List<SubscriptionResponse> response = subscriptionService.getOwnerMessSubscriptions(messId, ownerId, parsedStatus);
        return ResponseEntity.ok(ApiResponse.success("Owner mess subscriptions fetched successfully", response));
    }

    // ==========================================
    // ADMIN SUBSCRIPTION APIs
    // ==========================================

    /**
     * View all subscriptions across the platform with optional status filtering.
     */
    @GetMapping("/admin/subscriptions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getAllSubscriptions(
            @RequestParam(required = false) String status) {

        SubscriptionStatus parsedStatus = parseStatus(status);
        List<SubscriptionResponse> response = subscriptionService.getAllSubscriptions(parsedStatus);
        return ResponseEntity.ok(ApiResponse.success("All platform subscriptions fetched successfully", response));
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private SubscriptionStatus parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        return SubscriptionStatus.fromString(status);
    }

    private String resolveUserId(CustomUserDetails userDetails, Authentication authentication) {
        if (userDetails == null && authentication != null && authentication.getPrincipal() instanceof CustomUserDetails cud) {
            userDetails = cud;
        }

        if (userDetails != null && userDetails.getUser() != null) {
            if (userDetails.getUser().getStatus() == AccountStatus.BLOCKED) {
                throw new AccountBlockedException("Your account is blocked. Please contact support.");
            }
            if (userDetails.getUser().getStatus() != AccountStatus.ACTIVE) {
                throw new AccountInactiveException("Your account is currently inactive. Please contact support.");
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
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
            }
        }

        throw new UnauthorizedMessAccessException("Authentication required: user is not authenticated");
    }

    private String resolveOwnerId(CustomUserDetails userDetails, Authentication authentication) {
        if (userDetails == null && authentication != null && authentication.getPrincipal() instanceof CustomUserDetails cud) {
            userDetails = cud;
        }

        if (userDetails != null && userDetails.getUser() != null) {
            if (userDetails.getUser().getStatus() == AccountStatus.PENDING) {
                throw new OwnerPendingException("Your account is pending admin approval. Please wait for verification.");
            }
            if (userDetails.getUser().getStatus() != AccountStatus.ACTIVE) {
                throw new AccountInactiveException("Your account is currently inactive. Please contact support.");
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

