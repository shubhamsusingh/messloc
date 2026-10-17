package com.MessLoc.controller;

import com.MessLoc.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('USER')")
public class UserController {

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<Map<String, String>>> testUserAccess(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Access granted to USER endpoint",
                Map.of(
                        "role", "USER",
                        "username", authentication.getName(),
                        "message", "Welcome User! You have successfully verified access to protected customer features."
                )
        ));
    }
}
