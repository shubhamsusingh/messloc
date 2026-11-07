package com.MessLoc.dto.mess;

import com.MessLoc.enums.FoodType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class PublicMessResponse {
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
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm")
    private LocalTime openingTime;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm")
    private LocalTime closingTime;
    private List<String> facilities;
}
