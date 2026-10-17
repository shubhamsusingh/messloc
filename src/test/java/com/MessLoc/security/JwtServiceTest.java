package com.MessLoc.security;

import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Set 256-bit test secret and 1 hour expiration
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
    }

    @Test
    void testGenerateAndValidateToken() {
        User user = User.builder()
                .id("test-user-id-123")
                .name("Shivam Kumar")
                .email("shivam@example.com")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertFalse(token.isBlank());

        assertTrue(jwtService.validateToken(token));
        assertEquals("shivam@example.com", jwtService.extractUsername(token));
        assertEquals("USER", jwtService.extractRole(token));
        assertEquals("test-user-id-123", jwtService.extractUserId(token));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void testInvalidTokenSignature() {
        String forgedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiJoYWNrZXJAZXhhbXBsZS5jb20iLCJyb2xlIjoiQURNSU4ifQ." +
                "invalid_signature_string";

        assertFalse(jwtService.validateToken(forgedToken));
    }
}
