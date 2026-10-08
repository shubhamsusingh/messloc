package com.MessLoc.repository;

import com.MessLoc.entity.User;
import com.MessLoc.enums.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the User entity.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Finds a user by their unique email address.
     *
     * @param email user's email
     * @return Optional containing the User if found, or empty
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user already exists with the given email.
     *
     * @param email email to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Checks if any user exists with the specified role.
     * Used during application startup to determine if the initial ADMIN needs seeding.
     *
     * @param role role to check
     * @return true if at least one user with the role exists
     */
    boolean existsByRole(Role role);
}
