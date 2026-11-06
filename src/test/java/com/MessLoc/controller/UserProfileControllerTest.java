package com.MessLoc.controller;

import com.MessLoc.dto.response.UserProfileResponse;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.exception.ResourceNotFoundException;
import com.MessLoc.repository.MessRepository;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.security.CustomUserDetailsService;
import com.MessLoc.security.JwtService;
import com.MessLoc.service.AuthService;
import com.MessLoc.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    // Match the same @MockitoBean set as AuthControllerTest to share ApplicationContext
    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OwnerProfileRepository ownerProfileRepository;

    @MockitoBean
    private MessRepository messRepository;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestMongoConfig {
        @org.springframework.context.annotation.Bean(name = "mongoMappingContext")
        public MongoMappingContext mongoMappingContext() {
            return new MongoMappingContext();
        }
    }

    private User activeUser;
    private User activeOwner;
    private User activeAdmin;
    private User blockedUser;
    private User inactiveUser;

    private String activeUserToken;
    private String activeOwnerToken;
    private String activeAdminToken;
    private String blockedUserToken;
    private String inactiveUserToken;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id("u-101")
                .name("Shivam Kumar")
                .email("shivam@messloc.com")
                .phone("9876543210")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .profileImage(null)
                .build();

        activeOwner = User.builder()
                .id("o-201")
                .name("Rahul Sharma")
                .email("rahul@messloc.com")
                .phone("9876543211")
                .role(Role.OWNER)
                .status(AccountStatus.ACTIVE)
                .build();

        activeAdmin = User.builder()
                .id("a-301")
                .name("MessLoc Administrator")
                .email("admin@messloc.com")
                .phone("9999999999")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        blockedUser = User.builder()
                .id("u-999")
                .name("Blocked User")
                .email("blocked@messloc.com")
                .role(Role.USER)
                .status(AccountStatus.BLOCKED)
                .build();

        inactiveUser = User.builder()
                .id("u-888")
                .name("Inactive User")
                .email("inactive@messloc.com")
                .role(Role.USER)
                .status(AccountStatus.INACTIVE)
                .build();

        when(userDetailsService.loadUserByUsername("shivam@messloc.com"))
                .thenReturn(new CustomUserDetails(activeUser));
        when(userDetailsService.loadUserByUsername("rahul@messloc.com"))
                .thenReturn(new CustomUserDetails(activeOwner));
        when(userDetailsService.loadUserByUsername("admin@messloc.com"))
                .thenReturn(new CustomUserDetails(activeAdmin));
        when(userDetailsService.loadUserByUsername("blocked@messloc.com"))
                .thenReturn(new CustomUserDetails(blockedUser));
        when(userDetailsService.loadUserByUsername("inactive@messloc.com"))
                .thenReturn(new CustomUserDetails(inactiveUser));

        activeUserToken = "Bearer " + jwtService.generateToken(activeUser);
        activeOwnerToken = "Bearer " + jwtService.generateToken(activeOwner);
        activeAdminToken = "Bearer " + jwtService.generateToken(activeAdmin);
        blockedUserToken = "Bearer " + jwtService.generateToken(blockedUser);
        inactiveUserToken = "Bearer " + jwtService.generateToken(inactiveUser);
    }

    // ── 1. Authenticated USER can retrieve their own profile ──

    @Test
    void testGetMyProfile_AuthenticatedUser_Returns200() throws Exception {
        when(userService.getAuthenticatedUserProfile("shivam@messloc.com"))
                .thenReturn(UserProfileResponse.fromEntity(activeUser));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", activeUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("u-101"))
                .andExpect(jsonPath("$.data.name").value("Shivam Kumar"))
                .andExpect(jsonPath("$.data.email").value("shivam@messloc.com"))
                .andExpect(jsonPath("$.data.phone").value("9876543210"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
    }

    // ── 2. Authenticated OWNER can retrieve their own profile ──

    @Test
    void testGetMyProfile_AuthenticatedOwner_Returns200() throws Exception {
        when(userService.getAuthenticatedUserProfile("rahul@messloc.com"))
                .thenReturn(UserProfileResponse.fromEntity(activeOwner));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", activeOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("o-201"))
                .andExpect(jsonPath("$.data.name").value("Rahul Sharma"))
                .andExpect(jsonPath("$.data.role").value("OWNER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
    }

    // ── 3. Authenticated ADMIN can retrieve their own profile ──

    @Test
    void testGetMyProfile_AuthenticatedAdmin_Returns200() throws Exception {
        when(userService.getAuthenticatedUserProfile("admin@messloc.com"))
                .thenReturn(UserProfileResponse.fromEntity(activeAdmin));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", activeAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("a-301"))
                .andExpect(jsonPath("$.data.name").value("MessLoc Administrator"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
    }

    // ── 4. Missing JWT is rejected ──

    @Test
    void testGetMyProfile_NoToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── 5. Invalid JWT is rejected ──

    @Test
    void testGetMyProfile_InvalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer thisIsNotAValidJwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── 6. Expired JWT is rejected ──

    @Test
    void testGetMyProfile_ExpiredToken_Returns401() throws Exception {
        JwtService expiredJwtService = new JwtService();
        org.springframework.test.util.ReflectionTestUtils.setField(
                expiredJwtService, "secretKey",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        org.springframework.test.util.ReflectionTestUtils.setField(
                expiredJwtService, "jwtExpirationMs", -60000L);
        org.springframework.test.util.ReflectionTestUtils.setField(
                expiredJwtService, "jwtIssuer", "MessLoc");

        String expiredToken = "Bearer " + expiredJwtService.generateToken(activeUser);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── 7. Blocked account cannot access ──

    @Test
    void testGetMyProfile_BlockedAccount_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", blockedUserToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── 7b. Inactive account cannot access ──

    @Test
    void testGetMyProfile_InactiveAccount_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", inactiveUserToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── 8. Cannot retrieve another user's profile via request parameters ──

    @Test
    void testGetMyProfile_IgnoresQueryParameterUserId_ReturnsOwnProfile() throws Exception {
        when(userService.getAuthenticatedUserProfile("shivam@messloc.com"))
                .thenReturn(UserProfileResponse.fromEntity(activeUser));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", activeUserToken)
                        .param("userId", "a-301")
                        .param("email", "admin@messloc.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("u-101"))
                .andExpect(jsonPath("$.data.email").value("shivam@messloc.com"));
    }

    // ── 9. Response does not contain sensitive fields ──

    @Test
    void testGetMyProfile_NeverExposesSensitiveData() throws Exception {
        when(userService.getAuthenticatedUserProfile("shivam@messloc.com"))
                .thenReturn(UserProfileResponse.fromEntity(activeUser));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", activeUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.secret").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ── 10. Tampered JWT is rejected ──

    @Test
    void testGetMyProfile_TamperedToken_Returns401() throws Exception {
        String validToken = jwtService.generateToken(activeUser);
        String[] parts = validToken.split("\\.");
        String tamperedToken = "Bearer " + parts[0] + "." + parts[1] + ".tamperedInvalidSignature";

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── 11. User no longer exists in database returns 404 ──

    @Test
    void testGetMyProfile_UserDeletedFromDatabase_Returns404() throws Exception {
        when(userService.getAuthenticatedUserProfile("shivam@messloc.com"))
                .thenThrow(new ResourceNotFoundException("User profile not found for email: shivam@messloc.com"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", activeUserToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404));
    }
}
