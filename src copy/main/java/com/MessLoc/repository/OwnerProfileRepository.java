package com.MessLoc.repository;

import com.MessLoc.entity.OwnerProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the OwnerProfile entity.
 */
@Repository
public interface OwnerProfileRepository extends MongoRepository<OwnerProfile, String> {

    /**
     * Finds an owner profile by the associated user ID.
     *
     * @param userId ID of the User
     * @return Optional containing the OwnerProfile if found
     */
    Optional<OwnerProfile> findByUserId(String userId);

    /**
     * Checks if an owner profile exists for the given user ID.
     *
     * @param userId ID of the User
     * @return true if exists, false otherwise
     */
    boolean existsByUserId(String userId);

    /**
     * Finds all owner profiles matching the given verification status.
     *
     * @param verified verification flag
     * @return list of matching profiles
     */
    List<OwnerProfile> findByVerified(boolean verified);
}
