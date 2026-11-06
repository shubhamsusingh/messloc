package com.MessLoc.service;

import com.MessLoc.dto.response.UserProfileResponse;

/**
 * Service interface defining user profile operations.
 */
public interface UserService {

    /**
     * Retrieves the profile of the currently authenticated user by their email.
     *
     * @param email authenticated user's email (from SecurityContext)
     * @return sanitized user profile response
     */
    UserProfileResponse getAuthenticatedUserProfile(String email);
}

