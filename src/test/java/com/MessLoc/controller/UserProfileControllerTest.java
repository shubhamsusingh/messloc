package com.MessLoc.controller;

import com.MessLoc.dto.request.UpdateUserProfileRequest;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

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
                .city("Dehradun")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .profileImage(null)
                .build();

        activeOwner = User.builder()
                .id("o-201")
                .name("Rahul Sharma")
                .email("rahul@messloc.com")
                .phone("9876543211")
                .city("Dehradun")
                .role(Role.OWNER)
                .status(AccountStatus.ACTIVE)
                .build();

        activeAdmin = User.builder()
                .id("a-301")
                .name("MessLoc Administrator")
                .email("admin@messloc.com")
                .phone("9999999999")
                .city("Dehradun")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        blockedUser = User.builder()
                .id("u-999")
                .name("Blocked User")
                .email("blocked@messloc.com")
                .phone("9876543299")
                .role(Role.USER)
                .status(AccountStatus.BLOCKED)
                .build();

        inactiveUser = User.builder()
                .id("u-888")
                .name("Inactive User")
                .email("inactive@messloc.com")
                .phone("9876543288")
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

    // =========================================================================
    // GET /api/users/me tests
    // =========================================================================

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
                .andExpect(jsonPath("$.data.city").value("Dehradun"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
    }

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

    @Test
    void testGetMyProfile_NoToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testGetMyProfile_InvalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer thisIsNotAValidJwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

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

    @Test
    void testGetMyProfile_BlockedAccount_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", blockedUserToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testGetMyProfile_InactiveAccount_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", inactiveUserToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

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

    // =========================================================================
    // PUT /api/users/me tests
    // =========================================================================

    @Test
    void testUpdateMyProfile_AuthenticatedUser_Success_Returns200() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("Shivam Kumar Updated")
                .phone("9876543211")
                .city("Rishikesh")
                .build();

        UserProfileResponse updatedProfile = UserProfileResponse.builder()
                .id("u-101")
                .name("Shivam Kumar Updated")
                .email("shivam@messloc.com")
                .phone("9876543211")
                .city("Rishikesh")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(userService.updateAuthenticatedUserProfile(eq("shivam@messloc.com"), any(UpdateUserProfileRequest.class)))
                .thenReturn(updatedProfile);

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", activeUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User profile updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Shivam Kumar Updated"))
                .andExpect(jsonPath("$.data.phone").value("9876543211"))
                .andExpect(jsonPath("$.data.city").value("Rishikesh"))
                .andExpect(jsonPath("$.data.email").value("shivam@messloc.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void testUpdateMyProfile_AuthenticatedOwner_Success_Returns200() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("Rahul Sharma Updated")
                .phone("9876543222")
                .city("Haridwar")
                .build();

        UserProfileResponse updatedProfile = UserProfileResponse.builder()
                .id("o-201")
                .name("Rahul Sharma Updated")
                .email("rahul@messloc.com")
                .phone("9876543222")
                .city("Haridwar")
                .role(Role.OWNER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(userService.updateAuthenticatedUserProfile(eq("rahul@messloc.com"), any(UpdateUserProfileRequest.class)))
                .thenReturn(updatedProfile);

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", activeOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Rahul Sharma Updated"))
                .andExpect(jsonPath("$.data.role").value("OWNER"));
    }

    @Test
    void testUpdateMyProfile_AuthenticatedAdmin_Success_Returns200() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("Admin Head Updated")
                .phone("9999999998")
                .city("Delhi")
                .build();

        UserProfileResponse updatedProfile = UserProfileResponse.builder()
                .id("a-301")
                .name("Admin Head Updated")
                .email("admin@messloc.com")
                .phone("9999999998")
                .city("Delhi")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(userService.updateAuthenticatedUserProfile(eq("admin@messloc.com"), any(UpdateUserProfileRequest.class)))
                .thenReturn(updatedProfile);

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", activeAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Admin Head Updated"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void testUpdateMyProfile_NoToken_Returns401() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("New Name")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testUpdateMyProfile_InvalidToken_Returns401() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("New Name")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer invalidJwtToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testUpdateMyProfile_ExpiredToken_Returns401() throws Exception {
        JwtService expiredJwtService = new JwtService();
        org.springframework.test.util.ReflectionTestUtils.setField(
                expiredJwtService, "secretKey",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        org.springframework.test.util.ReflectionTestUtils.setField(
                expiredJwtService, "jwtExpirationMs", -60000L);
        org.springframework.test.util.ReflectionTestUtils.setField(
                expiredJwtService, "jwtIssuer", "MessLoc");

        String expiredToken = "Bearer " + expiredJwtService.generateToken(activeUser);

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("New Name")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testUpdateMyProfile_BlockedAccount_Returns401() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("New Name")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", blockedUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testUpdateMyProfile_InactiveAccount_Returns401() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("New Name")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", inactiveUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testUpdateMyProfile_ValidationFailure_InvalidPhoneNumber_Returns400() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("Shivam Kumar")
                .phone("12345") // Invalid: not 10 digits
                .build();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", activeUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.phone").exists());
    }

    @Test
    void testUpdateMyProfile_ValidationFailure_ShortName_Returns400() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("A") // Invalid: < 2 chars
                .build();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", activeUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    void testUpdateMyProfile_IgnoresInjectedPrivilegedFields_ReturnsSafeProfile() throws Exception {
        // Attempting to send role, email, password, status in raw JSON
        String maliciousPayload = "{" +
                "\"name\":\"Updated Name\"," +
                "\"phone\":\"9876543210\"," +
                "\"role\":\"ADMIN\"," +
                "\"email\":\"hacked@messloc.com\"," +
                "\"password\":\"HackedPassword123\"," +
                "\"status\":\"BLOCKED\"" +
                "}";

        UserProfileResponse safeResponse = UserProfileResponse.builder()
                .id("u-101")
                .name("Updated Name")
                .email("shivam@messloc.com")
                .phone("9876543210")
                .role(Role.USER) // Role remains USER
                .accountStatus(AccountStatus.ACTIVE) // Status remains ACTIVE
                .build();

        when(userService.updateAuthenticatedUserProfile(eq("shivam@messloc.com"), any(UpdateUserProfileRequest.class)))
                .thenReturn(safeResponse);

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", activeUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.email").value("shivam@messloc.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void testUpdateMyProfile_UserDeletedFromDatabase_Returns404() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("New Name")
                .build();

        when(userService.updateAuthenticatedUserProfile(eq("shivam@messloc.com"), any(UpdateUserProfileRequest.class)))
                .thenThrow(new ResourceNotFoundException("User profile not found for email: shivam@messloc.com"));

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", activeUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404));
    }
}
