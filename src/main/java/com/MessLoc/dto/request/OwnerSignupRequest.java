package com.MessLoc.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for mess OWNER registration.
 * Captures both user personal credentials and initial business metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerSignupRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be a valid 10-digit mobile number")
    private String phone;

    @NotBlank(message = "Business name cannot be blank")
    @Size(min = 3, max = 100, message = "Business name must be between 3 and 100 characters")
    private String businessName;

    @NotBlank(message = "Business description cannot be blank")
    @Size(min = 10, max = 500, message = "Business description must be between 10 and 500 characters")
    private String businessDescription;

    private String businessAddress;

    private String fssaiLicenseNumber;
}
