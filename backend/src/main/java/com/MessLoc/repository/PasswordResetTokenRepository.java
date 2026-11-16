package com.MessLoc.repository;

import com.MessLoc.entity.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing password reset token lifecycle operations.
 */
@Repository
public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    /**
     * Looks up a valid (unused) token by its SHA-256 hash.
     *
     * @param tokenHash SHA-256 hex-encoded hash of the submitted raw token
     * @return matching unused token, or empty if not found or already used
     */
    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);

    /**
     * Retrieves all unused tokens for a specific user (for bulk invalidation on new request).
     *
     * @param userId the user's document ID
     * @return list of unused tokens belonging to this user
     */
    List<PasswordResetToken> findByUserIdAndUsedFalse(String userId);

    /**
     * Counts how many tokens the user has requested since a given point in time.
     * Used for rate-limiting forgotten password requests.
     *
     * @param userId user document ID
     * @param since  the lower-bound timestamp (exclusive)
     * @return number of password reset token requests in the time window
     */
    int countByUserIdAndCreatedAtAfter(String userId, Instant since);

    /**
     * Deletes all tokens belonging to a user (useful for account deletion or hard reset).
     *
     * @param userId the user's document ID
     */
    void deleteByUserId(String userId);
}

