package com.MessLoc.service.impl;

import com.MessLoc.dto.request.ChangePasswordRequest;
import com.MessLoc.dto.request.ForgotPasswordRequest;
import com.MessLoc.dto.request.OwnerSignupRequest;
import com.MessLoc.dto.request.ResetPasswordRequest;
import com.MessLoc.dto.request.SigninRequest;
import com.MessLoc.dto.request.UserSignupRequest;
import com.MessLoc.dto.response.AuthResponse;
import com.MessLoc.dto.response.UserResponse;
import com.MessLoc.entity.OwnerProfile;
import com.MessLoc.entity.PasswordResetToken;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.exception.AccountBlockedException;
import com.MessLoc.exception.AccountInactiveException;
import com.MessLoc.exception.EmailAlreadyExistsException;
import com.MessLoc.exception.InvalidCredentialsException;
import com.MessLoc.exception.InvalidResetTokenException;
import com.MessLoc.exception.OwnerPendingException;
import com.MessLoc.exception.RateLimitExceededException;
import com.MessLoc.exception.ResourceNotFoundException;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.PasswordResetTokenRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.security.JwtService;
import com.MessLoc.service.AuthService;
import com.MessLoc.service.EmailService;
import com.MessLoc.util.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl = "http://localhost:3000";

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(UserRepository userRepository,
                           OwnerProfileRepository ownerProfileRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.ownerProfileRepository = ownerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public AuthResponse registerUser(UserSignupRequest request) {
        User savedUser = buildAndSaveUser(
                request.getName(),
                request.getEmail(),
                request.getPassword(),
                request.getPhone(),
                Role.USER,
                AccountStatus.ACTIVE
        );

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
        User savedUser = buildAndSaveUser(
                request.getName(),
                request.getEmail(),
                request.getPassword(),
                request.getPhone(),
                Role.OWNER,
                AccountStatus.PENDING
        );

        OwnerProfile ownerProfile = OwnerProfile.builder()
                .userId(savedUser.getId())
                .businessName(request.getBusinessName().trim())
                .businessDescription(request.getBusinessDescription().trim())
                .businessAddress(request.getBusinessAddress())
                .fssaiLicenseNumber(request.getFssaiLicenseNumber())
                .verified(false)
                .build();

        ownerProfileRepository.save(ownerProfile);
        log.info("Registered new OWNER with ID: {} pending admin verification", savedUser.getId());

        return AuthResponse.builder()
                .success(true)
                .message("Owner registered successfully. Account is pending admin verification.")
                .token(null)
                .tokenType(null)
                .expiresInMs(null)
                .user(UserResponse.fromEntity(savedUser))
                .build();
    }

    @Override
    public AuthResponse signin(SigninRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            log.warn("Authentication failed for email: {}", normalizedEmail);
            throw new InvalidCredentialsException("Invalid email or password");
        } catch (LockedException ex) {
            log.warn("Blocked user '{}' attempted signin", normalizedEmail);
            throw new AccountBlockedException("Your account has been blocked. Please contact MessLoc support.");
        } catch (DisabledException ex) {
            log.warn("Disabled/pending user '{}' attempted signin", normalizedEmail);
            User user = userRepository.findByEmail(normalizedEmail).orElse(null);
            if (user != null && user.getStatus() == AccountStatus.PENDING) {
                throw new OwnerPendingException("Your account is pending admin approval. Please wait for verification.");
            }
            throw new AccountInactiveException("Your account is currently inactive. Please contact MessLoc support.");
        } catch (AuthenticationException ex) {
            log.warn("Authentication exception for user '{}': {}", normalizedEmail, ex.getMessage());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        String jwt = jwtService.generateToken(user);
        log.info("User '{}' ({}) signed in successfully", user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .success(true)
                .message("Login successful")
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

    private User buildAndSaveUser(String name, String email, String rawPassword, String phone, Role role, AccountStatus status) {
        String normalizedEmail = email.toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("An account is already registered with email: " + normalizedEmail);
        }

        User user = User.builder()
                .name(name.trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(rawPassword))
                .phone(phone.trim())
                .role(role)
                .status(status)
                .build();

        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException ex) {
            throw new EmailAlreadyExistsException("An account is already registered with email: " + normalizedEmail);
        }
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, String authenticatedUserEmail) {
        String normalizedEmail = authenticatedUserEmail.toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + normalizedEmail));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation password do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as the current password");
        }

        PasswordValidator.validatePassword(request.getNewPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for user '{}'", normalizedEmail);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email '{}'. Generic response returned.", normalizedEmail);
            return;
        }

        User user = userOpt.get();

        // Enforce rate limiting: max 3 requests per 15 minutes per user
        Instant fifteenMinutesAgo = Instant.now().minus(Duration.ofMinutes(15));
        int recentAttempts = passwordResetTokenRepository.countByUserIdAndCreatedAtAfter(user.getId(), fifteenMinutesAgo);
        if (recentAttempts >= 3) {
            log.warn("Rate limit exceeded for password reset attempts by user '{}'", normalizedEmail);
            throw new RateLimitExceededException("Too many password reset requests. Please wait a few minutes before trying again.");
        }

        // Invalidate previous unused reset tokens
        List<PasswordResetToken> unusedTokens = passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId());
        for (PasswordResetToken token : unusedTokens) {
            token.setUsed(true);
            token.setUsedAt(Instant.now());
        }
        if (!unusedTokens.isEmpty()) {
            passwordResetTokenRepository.saveAll(unusedTokens);
        }

        // Generate 32-byte cryptographically secure raw token
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = HexFormat.of().formatHex(randomBytes);

        // Store SHA-256 hash of token
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiryDate(Instant.now().plus(Duration.ofMinutes(15)))
                .used(false)
                .createdAt(Instant.now())
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Construct reset link and dispatch email
        String resetLink = (frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) : frontendBaseUrl)
                + "/reset-password?token=" + rawToken;

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetLink);
        } catch (Exception ex) {
            log.warn("Failed to dispatch password reset email to '{}': {}", user.getEmail(), ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation password do not match");
        }

        PasswordValidator.validatePassword(request.getNewPassword());

        String rawToken = request.getToken().trim();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() -> new InvalidResetTokenException("Invalid or already consumed password reset token"));

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            resetToken.setUsed(true);
            resetToken.setUsedAt(Instant.now());
            passwordResetTokenRepository.save(resetToken);
            throw new InvalidResetTokenException("Password reset token has expired");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new InvalidResetTokenException("User associated with reset token no longer exists"));

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as the current password");
        }

        // Invalidate all tokens for this user
        List<PasswordResetToken> allUserTokens = passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId());
        for (PasswordResetToken token : allUserTokens) {
            token.setUsed(true);
            token.setUsedAt(Instant.now());
        }
        passwordResetTokenRepository.saveAll(allUserTokens);

        // Mark current token consumed
        resetToken.setUsed(true);
        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        // Update password with BCrypt hash
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password reset successfully completed for user '{}'", user.getEmail());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available in JVM environment", ex);
        }
    }
}
