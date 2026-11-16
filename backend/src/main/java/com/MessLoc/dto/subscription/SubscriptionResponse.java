package com.MessLoc.dto.subscription;

import com.MessLoc.entity.Subscription;
import com.MessLoc.entity.User;
import com.MessLoc.enums.SubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Standard API response payload representing subscription details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionResponse {

    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String messId;
    private String messName;
    private String planId;
    private String planName;
    private Integer durationDays;
    private Double price;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private SubscriptionStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant cancelledAt;

    /**
     * Map entity to response without user details.
     */
    public static SubscriptionResponse fromEntity(Subscription subscription) {
        if (subscription == null) {
            return null;
        }
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUserId())
                .messId(subscription.getMessId())
                .messName(subscription.getMessName())
                .planId(subscription.getPlanId())
                .planName(subscription.getPlanName())
                .durationDays(subscription.getDurationDays())
                .price(subscription.getPrice())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .status(subscription.getStatus())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .cancelledAt(subscription.getCancelledAt())
                .build();
    }

    /**
     * Map entity to response including subscriber details.
     */
    public static SubscriptionResponse fromEntityWithUser(Subscription subscription, User user) {
        SubscriptionResponse response = fromEntity(subscription);
        if (response != null && user != null) {
            response.setUserName(user.getName());
            response.setUserEmail(user.getEmail());
            response.setUserPhone(user.getPhone());
        }
        return response;
    }
}

