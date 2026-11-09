package com.MessLoc.controller;

import com.MessLoc.dto.request.UserSignupRequest;
import com.MessLoc.dto.response.ApiResponse;
import com.MessLoc.dto.response.UserResponse;
import com.MessLoc.entity.OwnerProfile;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.exception.EmailAlreadyExistsException;
import com.MessLoc.exception.ResourceNotFoundException;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<UserResponse>> createAdmin(
            @Valid @RequestBody UserSignupRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("An account is already registered with email: " + normalizedEmail);
        }

        User admin = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone().trim())
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        User savedAdmin = userRepository.save(admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("New admin account created successfully", UserResponse.fromEntity(savedAdmin)));
    }

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

    @GetMapping("/owners/pending")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getPendingOwners() {
        List<UserResponse> pendingOwners = userRepository.findByRoleAndStatus(Role.OWNER, AccountStatus.PENDING)
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Pending owners fetched successfully", pendingOwners));
    }

    @PatchMapping("/owners/{userId}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyOwner(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner user not found with id: " + userId));

        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        ownerProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setVerified(true);
            ownerProfileRepository.save(profile);
        });

        return ResponseEntity.ok(ApiResponse.success("Owner verified and activated successfully", null));
    }

    @PatchMapping("/owners/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> blockOwner(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner user not found with id: " + userId));

        user.setStatus(AccountStatus.BLOCKED);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("Owner account blocked successfully", null));
    }
}
