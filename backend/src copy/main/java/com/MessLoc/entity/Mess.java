package com.MessLoc.entity;

import com.MessLoc.enums.FoodType;
import com.MessLoc.enums.MessStatus;
import com.MessLoc.enums.VerificationStatus;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "messes")
@CompoundIndexes({
    @CompoundIndex(name = "verification_status_city_idx", def = "{'verificationStatus': 1, 'status': 1, 'city': 1}")
})
public class Mess {

    @Id
    private String id;

    @Indexed
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

    @Builder.Default
    private List<String> facilities = new ArrayList<>();

    private VerificationStatus verificationStatus;
    private MessStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
