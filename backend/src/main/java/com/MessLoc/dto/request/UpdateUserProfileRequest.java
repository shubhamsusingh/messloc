package com.MessLoc.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating the authenticated user's profile.
 * Only permitted editable fields are exposed. Privileged fields like
 * id, email, password, role, and accountStatus cannot be modified via this DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be a valid 10-digit mobile number")
    private String phone;

    @Size(min = 2, max = 50, message = "City must be between 2 and 50 characters")
    private String city;

    @Size(max = 255, message = "Profile image URL must not exceed 255 characters")
    private String profileImage;
}

