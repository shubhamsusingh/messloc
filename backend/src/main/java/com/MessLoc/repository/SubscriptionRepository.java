package com.MessLoc.repository;

import com.MessLoc.entity.Subscription;
import com.MessLoc.enums.SubscriptionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends MongoRepository<Subscription, String> {

    List<Subscription> findByUserId(String userId);

    List<Subscription> findByUserIdAndStatus(String userId, SubscriptionStatus status);

    List<Subscription> findByMessId(String messId);

    List<Subscription> findByMessIdAndStatus(String messId, SubscriptionStatus status);

    List<Subscription> findByStatus(SubscriptionStatus status);

    Optional<Subscription> findByUserIdAndMessIdAndStatus(String userId, String messId, SubscriptionStatus status);

    boolean existsByUserIdAndMessIdAndStatus(String userId, String messId, SubscriptionStatus status);
}

