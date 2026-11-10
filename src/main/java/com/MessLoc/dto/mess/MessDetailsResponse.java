package com.MessLoc.dto.mess;

import com.MessLoc.enums.FoodType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessDetailsResponse {
    private String id;
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
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openingTime;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime closingTime;
    private List<String> facilities;
}

