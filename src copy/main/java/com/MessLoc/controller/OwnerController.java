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
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('OWNER')")
public class OwnerController {

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<Map<String, String>>> testOwnerAccess(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Access granted to OWNER endpoint",
                Map.of(
                        "role", "OWNER",
                        "username", authentication.getName(),
                        "message", "Welcome Owner! You have successfully verified access to protected business owner features."
                )
        ));
    }
}
