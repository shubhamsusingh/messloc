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
import com.MessLoc.exception.OwnerPendingException;
import com.MessLoc.exception.ResourceNotFoundException;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.security.JwtService;
import com.MessLoc.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository,
                           OwnerProfileRepository ownerProfileRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.ownerProfileRepository = ownerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
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
}
