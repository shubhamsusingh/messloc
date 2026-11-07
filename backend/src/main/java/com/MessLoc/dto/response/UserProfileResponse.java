package com.MessLoc.dto.response;

import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Dedicated response DTO for the user profile endpoint.
 * Exposes only safe, non-sensitive fields from the User entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String city;
    private Role role;
    private AccountStatus accountStatus;
    private String profileImage;
    private LocalDateTime createdAt;

    /**
     * Factory mapping method from User entity to UserProfileResponse DTO.
     */
    public static UserProfileResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .city(user.getCity())
                .role(user.getRole())
                .accountStatus(user.getStatus())
                .profileImage(user.getProfileImage())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

