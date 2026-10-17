package com.MessLoc.service;

import com.MessLoc.dto.request.OwnerSignupRequest;
import com.MessLoc.dto.request.SigninRequest;
import com.MessLoc.dto.request.UserSignupRequest;
import com.MessLoc.dto.response.AuthResponse;
import com.MessLoc.dto.response.UserResponse;

/**
 * Service interface defining authentication, user registration, and identity contracts.
 */
public interface AuthService {

    /**
     * Registers a new regular USER (Customer).
     *
     * @param request user registration payload
     * @return authentication response containing JWT and user profile
     */
    AuthResponse registerUser(UserSignupRequest request);

    /**
     * Registers a new mess OWNER with PENDING status.
     *
     * @param request owner registration payload including business metadata
     * @return authentication response containing JWT and user profile (pending verification)
     */
    AuthResponse registerOwner(OwnerSignupRequest request);

    /**
     * Authenticates an existing user and returns a signed JWT.
     *
     * @param request signin credentials
     * @return authentication response containing JWT and user profile
     */
    AuthResponse signin(SigninRequest request);

    /**
     * Retrieves the profile of the currently authenticated user by their principal email.
     *
     * @param email authenticated user's email
     * @return sanitized user response
     */
    UserResponse getCurrentUser(String email);
}
