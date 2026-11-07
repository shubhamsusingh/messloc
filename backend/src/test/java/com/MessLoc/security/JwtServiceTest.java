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
        // Set 256-bit test secret, 1 hour expiration, and test issuer
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtService, "jwtIssuer", "MessLoc");
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
        assertEquals("MessLoc", jwtService.extractIssuer(token));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void testInvalidTokenSignature() {
        String forgedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiJoYWNrZXJAZXhhbXBsZS5jb20iLCJyb2xlIjoiQURNSU4ifQ." +
                "invalid_signature_string";

        assertFalse(jwtService.validateToken(forgedToken));
    }

    @Test
    void testTamperedTokenPayload() {
        User user = User.builder()
                .id("test-user-id-123")
                .name("Shivam Kumar")
                .email("shivam@example.com")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        String validToken = jwtService.generateToken(user);
        String[] parts = validToken.split("\\.");
        assertEquals(3, parts.length);

        // Tamper with payload by appending a character
        String tamperedToken = parts[0] + "." + parts[1] + "a" + "." + parts[2];
        assertFalse(jwtService.validateToken(tamperedToken));
    }

    @Test
    void testExpiredTokenValidation() {
        JwtService shortLivedJwtService = new JwtService();
        ReflectionTestUtils.setField(shortLivedJwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        // Expiration in negative milliseconds (already expired)
        ReflectionTestUtils.setField(shortLivedJwtService, "jwtExpirationMs", -1000L);
        ReflectionTestUtils.setField(shortLivedJwtService, "jwtIssuer", "MessLoc");

        User user = User.builder()
                .id("expired-user")
                .name("Expired User")
                .email("expired@example.com")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        String expiredToken = shortLivedJwtService.generateToken(user);
        assertNotNull(expiredToken);
        assertFalse(shortLivedJwtService.validateToken(expiredToken));
        assertTrue(shortLivedJwtService.isTokenExpired(expiredToken));
    }

    @Test
    void testMalformedTokenValidation() {
        assertFalse(jwtService.validateToken("not.a.valid.jwt.token"));
        assertFalse(jwtService.validateToken("random-string"));
        assertFalse(jwtService.validateToken(""));
    }

    @Test
    void testTokenWithMismatchedIssuer() {
        JwtService foreignService = new JwtService();
        ReflectionTestUtils.setField(foreignService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(foreignService, "jwtExpirationMs", 3600000L);
        ReflectionTestUtils.setField(foreignService, "jwtIssuer", "ForeignIssuer");

        User user = User.builder()
                .id("foreign-user")
                .name("Foreign User")
                .email("foreign@example.com")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        String tokenWithForeignIssuer = foreignService.generateToken(user);
        assertNotNull(tokenWithForeignIssuer);
        // Valid for foreignService
        assertTrue(foreignService.validateToken(tokenWithForeignIssuer));
        // Invalid for jwtService expecting "MessLoc"
        assertFalse(jwtService.validateToken(tokenWithForeignIssuer));
    }
}
