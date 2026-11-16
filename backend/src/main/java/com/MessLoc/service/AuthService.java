package com.MessLoc.service;

import com.MessLoc.dto.request.ChangePasswordRequest;
import com.MessLoc.dto.request.ForgotPasswordRequest;
import com.MessLoc.dto.request.OwnerSignupRequest;
import com.MessLoc.dto.request.ResetPasswordRequest;
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

    /**
     * Changes the authenticated user's password.
     * Enforces current password validation, new password complexity, and confirmation match.
     *
     * @param request change password payload
     * @param authenticatedUserEmail the email extracted securely from the SecurityContext
     */
    void changePassword(ChangePasswordRequest request, String authenticatedUserEmail);

    /**
     * Initiates a password reset workflow.
     * Generates a cryptographically secure token, hashes it, stores the hash, and triggers an email.
     * Uses a generic response to prevent account enumeration.
     *
     * @param request forgot password payload containing the target email
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * Completes a password reset using a one-time token.
     * Validates the token, ensures it is neither expired nor used, enforces password policy,
     * updates the user's password, and invalidates outstanding tokens.
     *
     * @param request reset password payload containing token, new password, and confirmation
     */
    void resetPassword(ResetPasswordRequest request);
}
