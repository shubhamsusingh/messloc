package com.MessLoc.repository;

import com.MessLoc.entity.Mess;
import com.MessLoc.enums.MessStatus;
import com.MessLoc.enums.VerificationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessRepository extends MongoRepository<Mess, String> {

    List<Mess> findByOwnerId(String ownerId);

    Optional<Mess> findByIdAndOwnerId(String id, String ownerId);

    List<Mess> findByVerificationStatusAndStatus(VerificationStatus verificationStatus, MessStatus status);

    List<Mess> findByVerificationStatus(VerificationStatus verificationStatus);
}
