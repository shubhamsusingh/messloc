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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<Map<String, String>>> testAdminAccess(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Access granted to ADMIN endpoint",
                Map.of(
                        "role", "ADMIN",
                        "username", authentication.getName(),
                        "message", "Welcome Admin! You have successfully verified access to protected administrative controls."
                )
        ));
    }
}
