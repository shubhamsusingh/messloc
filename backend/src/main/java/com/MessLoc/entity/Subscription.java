package com.MessLoc.entity;

import com.MessLoc.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/**
 * MongoDB document representing a customer's subscription to a mess facility.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "subscriptions")
@CompoundIndexes({
    @CompoundIndex(name = "user_mess_status_idx", def = "{'userId': 1, 'messId': 1, 'status': 1}"),
    @CompoundIndex(name = "mess_status_idx", def = "{'messId': 1, 'status': 1}"),
    @CompoundIndex(name = "user_status_idx", def = "{'userId': 1, 'status': 1}")
})
public class Subscription {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String messId;

    private String messName;

    private String planId;

    private String planName;

    private Integer durationDays;

    private Double price;

    private LocalDate startDate;

    private LocalDate endDate;

    private SubscriptionStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant cancelledAt;
}

