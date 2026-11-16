package com.MessLoc.dto.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for subscribing to an approved mess facility.
 * Security Note: userId and subscription dates are not accepted from the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {

    @NotBlank(message = "Mess ID is required")
    private String messId;

    private String planId;

    private String planName;

    @Positive(message = "Duration must be greater than 0 days")
    private Integer durationDays;
}

