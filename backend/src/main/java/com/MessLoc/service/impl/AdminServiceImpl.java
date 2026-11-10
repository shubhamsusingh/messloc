package com.MessLoc.service.impl;

import com.MessLoc.dto.request.CreateAdminRequest;
import com.MessLoc.dto.response.UserResponse;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.exception.AccountBlockedException;
import com.MessLoc.exception.AccountInactiveException;
import com.MessLoc.exception.EmailAlreadyExistsException;
import com.MessLoc.exception.ResourceNotFoundException;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation managing administrative business logic:
 * Admin creation, owner verification, and owner blocking.
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createAdmin(CreateAdminRequest request) {
        validateCurrentAdminStatus();

        String normalizedEmail = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Admin creation rejected: email already exists '{}'", normalizedEmail);
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
        log.info("Successfully created new ADMIN account: id='{}', email='{}'", savedAdmin.getId(), normalizedEmail);
        return UserResponse.fromEntity(savedAdmin);
    }

    @Override
    public List<UserResponse> getPendingOwners() {
        return userRepository.findByRoleAndStatus(Role.OWNER, AccountStatus.PENDING)
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void verifyOwner(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner user not found with id: " + userId));

        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        ownerProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setVerified(true);
            ownerProfileRepository.save(profile);
        });

        log.info("Owner '{}' verified and activated successfully by admin", userId);
    }

    @Override
    public void blockOwner(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner user not found with id: " + userId));

        user.setStatus(AccountStatus.BLOCKED);
        userRepository.save(user);

        log.info("Owner '{}' blocked successfully by admin", userId);
    }

    /**
     * Defense-in-depth verification of the invoking principal:
     * Validates that the executing security context contains an active, unblocked ADMIN.
     */
    private void validateCurrentAdminStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            User currentAdmin = userDetails.getUser();
            if (currentAdmin != null) {
                if (currentAdmin.getStatus() == AccountStatus.BLOCKED) {
                    throw new AccountBlockedException("Admin account is blocked and cannot perform administrative operations");
                }
                if (currentAdmin.getStatus() != AccountStatus.ACTIVE) {
                    throw new AccountInactiveException("Admin account is not active");
                }
                if (currentAdmin.getRole() != Role.ADMIN) {
                    throw new AccessDeniedException("Access denied: Only active administrators can create new admin accounts");
                }
            }
        }
    }
}

