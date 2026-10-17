package com.MessLoc.dto.mess;

import com.MessLoc.enums.FoodType;
import com.MessLoc.enums.MessStatus;
import com.MessLoc.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class MessResponse {
    private String id;
    private String ownerId;
    private String name;
    private String description;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private String contactNumber;
    private Double monthlyPrice;
    private FoodType foodType;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private List<String> facilities;
    private VerificationStatus verificationStatus;
    private MessStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
