package com.MessLoc.service;

import com.MessLoc.dto.request.CreateAdminRequest;
import com.MessLoc.dto.response.UserResponse;

import java.util.List;

/**
 * Service interface defining administrative business operations.
 */
public interface AdminService {

    /**
     * Registers a new administrator account.
     * Enforces unique email check, password encryption, automatic assignment of
     * Role.ADMIN and AccountStatus.ACTIVE, and returns sanitized UserResponse.
     *
     * @param request admin registration payload
     * @return sanitized user response without password or hash
     */
    UserResponse createAdmin(CreateAdminRequest request);

    /**
     * Retrieves all mess owners with PENDING verification status.
     *
     * @return list of pending owner accounts
     */
    List<UserResponse> getPendingOwners();

    /**
     * Verifies and activates a pending owner account and marks their profile as verified.
     *
     * @param userId user identifier of the owner
     */
    void verifyOwner(String userId);

    /**
     * Blocks an owner account from accessing the platform.
     *
     * @param userId user identifier of the owner
     */
    void blockOwner(String userId);
}

