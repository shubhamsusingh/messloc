package com.MessLoc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard authentication response returned on successful signup or signin.
 * Contains the signed JWT, token metadata, and the authenticated user's profile summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    @Builder.Default
    private boolean success = true;

    private String message;

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresInMs;

    private UserResponse user;
}
