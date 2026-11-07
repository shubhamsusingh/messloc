package com.MessLoc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Stores mess business-specific operational and verification details.
 * Kept separate from the core 'users' collection to maintain high-performance
 * authentication lookups while scaling business metadata.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "owner_profiles")
public class OwnerProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String businessName;

    private String businessDescription;

    private String businessAddress;

    private String fssaiLicenseNumber;

    @Builder.Default
    private boolean verified = false;

    private String verifiedByAdminId;

    private LocalDateTime verifiedAt;

    private String rejectionReason;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
