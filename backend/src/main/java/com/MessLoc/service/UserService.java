package com.MessLoc.service;

import com.MessLoc.dto.request.UpdateUserProfileRequest;
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

    /**
     * Updates permitted profile fields of the currently authenticated user.
     *
     * @param email authenticated user's email (from SecurityContext)
     * @param request update payload containing only permitted editable fields
     * @return sanitized updated user profile response
     */
    UserProfileResponse updateAuthenticatedUserProfile(String email, UpdateUserProfileRequest request);
}


