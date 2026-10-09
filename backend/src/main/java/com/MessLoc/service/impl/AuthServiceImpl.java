package com.MessLoc.service.impl;

import com.MessLoc.dto.request.OwnerSignupRequest;
import com.MessLoc.dto.request.SigninRequest;
import com.MessLoc.dto.request.UserSignupRequest;
import com.MessLoc.dto.response.AuthResponse;
import com.MessLoc.dto.response.UserResponse;
import com.MessLoc.entity.OwnerProfile;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.exception.AccountBlockedException;
import com.MessLoc.exception.AccountInactiveException;
import com.MessLoc.exception.EmailAlreadyExistsException;
import com.MessLoc.exception.InvalidCredentialsException;
import com.MessLoc.exception.ResourceNotFoundException;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.JwtService;
import com.MessLoc.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production implementation of AuthService.
 * Coordinates user creation, password verification, account status checks, and token generation.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           OwnerProfileRepository ownerProfileRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.ownerProfileRepository = ownerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse registerUser(UserSignupRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("An account is already registered with email: " + normalizedEmail);
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone().trim())
                .role(Role.USER) // Role is strictly enforced on the server
                .status(AccountStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new USER with ID: {}", savedUser.getId());

        String jwt = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .success(true)
                .message("User registered successfully")
                .token(jwt)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getJwtExpirationMs())
                .user(UserResponse.fromEntity(savedUser))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse registerOwner(OwnerSignupRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("An account is already registered with email: " + normalizedEmail);
        }

        // 1. Create and persist User record
        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone().trim())
                .role(Role.OWNER) // Role is strictly enforced on the server
                .status(AccountStatus.PENDING) // Requires Admin verification before operating mess
                .build();

        User savedUser = userRepository.save(user);

        // 2. Create and persist separate OwnerProfile record
        OwnerProfile ownerProfile = OwnerProfile.builder()
                .userId(savedUser.getId())
                .businessName(request.getBusinessName().trim())
                .businessDescription(request.getBusinessDescription().trim())
                .businessAddress(request.getBusinessAddress())
                .fssaiLicenseNumber(request.getFssaiLicenseNumber())
                .verified(false)
                .build();

        ownerProfileRepository.save(ownerProfile);
        log.info("Registered new OWNER with ID: {} and pending verification", savedUser.getId());

        String jwt = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .success(true)
                .message("Owner registered successfully. Account is pending admin verification.")
                .token(jwt)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getJwtExpirationMs())
                .user(UserResponse.fromEntity(savedUser))
                .build();
    }

    @Override
    public AuthResponse signin(SigninRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        // 1. Find user by email (safe generic error prevents user enumeration)
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // 2. Verify raw password against stored BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for user: {}", normalizedEmail);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // 3. Verify Account Status
        if (user.getStatus() == AccountStatus.BLOCKED) {
            log.warn("Blocked user '{}' attempted to log in", normalizedEmail);
            throw new AccountBlockedException("Your account has been blocked. Please contact MessMate support.");
        }

        if (user.getStatus() == AccountStatus.INACTIVE) {
            log.warn("Inactive user '{}' attempted to log in", normalizedEmail);
            throw new AccountInactiveException("Your account is currently inactive. Please contact MessMate support.");
        }

        // 4. Generate JWT
        String jwt = jwtService.generateToken(user);
        log.info("User '{}' ({}) authenticated successfully", user.getEmail(), user.getRole());

        String message = "Login successful";
        if (user.getRole() == Role.OWNER && user.getStatus() == AccountStatus.PENDING) {
            message = "Login successful. Notice: Your mess owner account is currently pending admin verification.";
        }

        return AuthResponse.builder()
                .success(true)
                .message(message)
                .token(jwt)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getJwtExpirationMs())
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Override
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for email: " + email));
        return UserResponse.fromEntity(user);
    }
}
