package com.MessLoc.controller;

import com.MessLoc.dto.request.OwnerSignupRequest;
import com.MessLoc.dto.request.SigninRequest;
import com.MessLoc.dto.request.UserSignupRequest;
import com.MessLoc.dto.response.AuthResponse;
import com.MessLoc.dto.response.UserResponse;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.exception.AccountBlockedException;
import com.MessLoc.exception.AccountInactiveException;
import com.MessLoc.exception.EmailAlreadyExistsException;
import com.MessLoc.exception.InvalidCredentialsException;
import com.MessLoc.exception.OwnerPendingException;
import com.MessLoc.repository.MessRepository;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.security.CustomUserDetailsService;
import com.MessLoc.security.JwtService;
import com.MessLoc.service.AuthService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private JwtService jwtService;

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
    private User blockedUser;
    private User inactiveUser;
    private String activeUserToken;
    private String blockedUserToken;
    private String inactiveUserToken;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id("u-101")
                .name("Shivam Kumar")
                .email("shivam@messloc.com")
                .role(Role.USER)
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
        when(userDetailsService.loadUserByUsername("blocked@messloc.com"))
                .thenReturn(new CustomUserDetails(blockedUser));
        when(userDetailsService.loadUserByUsername("inactive@messloc.com"))
                .thenReturn(new CustomUserDetails(inactiveUser));

        activeUserToken = "Bearer " + jwtService.generateToken(activeUser);
        blockedUserToken = "Bearer " + jwtService.generateToken(blockedUser);
        inactiveUserToken = "Bearer " + jwtService.generateToken(inactiveUser);
    }

    @Test
    void testUserSignup_Success_Returns201() throws Exception {
        UserSignupRequest request = UserSignupRequest.builder()
                .name("Shivam Kumar")
                .email("shivam@messloc.com")
                .password("StrongPass123")
                .phone("9876543210")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .success(true)
                .message("User registered successfully")
                .token("mock_jwt")
                .tokenType("Bearer")
                .expiresInMs(86400000L)
                .user(UserResponse.fromEntity(activeUser))
                .build();

        when(authService.registerUser(any(UserSignupRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/signup/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").value("mock_jwt"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));
    }

    @Test
    void testUserSignup_DuplicateEmail_Returns409Conflict() throws Exception {
        UserSignupRequest request = UserSignupRequest.builder()
                .name("Shivam Kumar")
                .email("duplicate@messloc.com")
                .password("StrongPass123")
                .phone("9876543210")
                .build();

        when(authService.registerUser(any(UserSignupRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("An account is already registered with email: duplicate@messloc.com"));

        mockMvc.perform(post("/api/auth/signup/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testUserSignup_ValidationFailure_InvalidEmail_Returns400() throws Exception {
        UserSignupRequest request = UserSignupRequest.builder()
                .name("Shivam Kumar")
                .email("invalid-email-format")
                .password("StrongPass123")
                .phone("9876543210")
                .build();

        mockMvc.perform(post("/api/auth/signup/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.email").exists());
    }

    @Test
    void testUserSignup_ValidationFailure_ShortPassword_Returns400() throws Exception {
        UserSignupRequest request = UserSignupRequest.builder()
                .name("Shivam Kumar")
                .email("test@messloc.com")
                .password("short")
                .phone("9876543210")
                .build();

        mockMvc.perform(post("/api/auth/signup/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    void testOwnerSignup_Success_Returns201_PendingVerification() throws Exception {
        OwnerSignupRequest request = OwnerSignupRequest.builder()
                .name("Rahul Sharma")
                .email("rahul@messloc.com")
                .password("OwnerPass123")
                .phone("9876543210")
                .businessName("Sharma Caterers")
                .businessDescription("Healthy meal plans")
                .build();

        User ownerUser = User.builder()
                .id("o-101")
                .name("Rahul Sharma")
                .email("rahul@messloc.com")
                .role(Role.OWNER)
                .status(AccountStatus.PENDING)
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .success(true)
                .message("Owner registered successfully. Account is pending admin verification.")
                .token(null)
                .user(UserResponse.fromEntity(ownerUser))
                .build();

        when(authService.registerOwner(any(OwnerSignupRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/signup/owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.user.role").value("OWNER"))
                .andExpect(jsonPath("$.user.status").value("PENDING"));
    }

    @Test
    void testSignin_Success_Returns200() throws Exception {
        SigninRequest request = SigninRequest.builder()
                .email("shivam@messloc.com")
                .password("StrongPass123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .success(true)
                .message("Login successful")
                .token("mock_valid_jwt")
                .tokenType("Bearer")
                .expiresInMs(86400000L)
                .user(UserResponse.fromEntity(activeUser))
                .build();

        when(authService.signin(any(SigninRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").value("mock_valid_jwt"));
    }

    @Test
    void testSignin_InvalidCredentials_Returns401() throws Exception {
        SigninRequest request = SigninRequest.builder()
                .email("shivam@messloc.com")
                .password("WrongPassword")
                .build();

        when(authService.signin(any(SigninRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testSignin_BlockedAccount_Returns403() throws Exception {
        SigninRequest request = SigninRequest.builder()
                .email("blocked@messloc.com")
                .password("StrongPass123")
                .build();

        when(authService.signin(any(SigninRequest.class)))
                .thenThrow(new AccountBlockedException("Your account has been blocked. Please contact MessLoc support."));

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testSignin_InactiveAccount_Returns403() throws Exception {
        SigninRequest request = SigninRequest.builder()
                .email("inactive@messloc.com")
                .password("StrongPass123")
                .build();

        when(authService.signin(any(SigninRequest.class)))
                .thenThrow(new AccountInactiveException("Your account is currently inactive. Please contact MessLoc support."));

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testSignin_PendingAccount_Returns403() throws Exception {
        SigninRequest request = SigninRequest.builder()
                .email("pending@messloc.com")
                .password("StrongPass123")
                .build();

        when(authService.signin(any(SigninRequest.class)))
                .thenThrow(new OwnerPendingException("Your account is pending admin approval. Please wait for verification."));

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testGetCurrentUser_Authenticated_Returns200() throws Exception {
        when(authService.getCurrentUser("shivam@messloc.com"))
                .thenReturn(UserResponse.fromEntity(activeUser));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", activeUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("shivam@messloc.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
    }

    @Test
    void testGetCurrentUser_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testGetCurrentUser_WithBlockedAccountToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", blockedUserToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testGetCurrentUser_MalformedToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer thisIsNotAValidJwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testUnsupportedMethod_Returns405() throws Exception {
        mockMvc.perform(put("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void testAttemptAdminSignup_DirectEndpoint_ReturnsUnauthorized() throws Exception {
        // Public registration must not provide any ADMIN signup endpoint
        mockMvc.perform(post("/api/auth/signup/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"fakeadmin@messloc.com\",\"password\":\"AdminPass123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAttemptAdminSignup_InjectedRoleInUserSignup_RoleRemainsUser() throws Exception {
        // Attempting to inject "role": "ADMIN" into UserSignupRequest must be ignored
        String maliciousPayload = "{" +
                "\"name\":\"Malicious User\"," +
                "\"email\":\"malicious@messloc.com\"," +
                "\"password\":\"StrongPass123\"," +
                "\"phone\":\"9876543210\"," +
                "\"role\":\"ADMIN\"" +
                "}";

        AuthResponse authResponse = AuthResponse.builder()
                .success(true)
                .message("User registered successfully")
                .token("mock_jwt")
                .user(UserResponse.fromEntity(activeUser))
                .build();

        when(authService.registerUser(any(UserSignupRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/signup/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void testUserSignup_ValidationFailure_InvalidPhoneNumber_Returns400() throws Exception {
        UserSignupRequest request = UserSignupRequest.builder()
                .name("Shivam Kumar")
                .email("shivam@messloc.com")
                .password("StrongPass123")
                .phone("12345") // Invalid: not 10 digits
                .build();

        mockMvc.perform(post("/api/auth/signup/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.phone").exists());
    }

    @Test
    void testUserSignup_ValidationFailure_BlankName_Returns400() throws Exception {
        UserSignupRequest request = UserSignupRequest.builder()
                .name("") // Invalid: blank
                .email("shivam@messloc.com")
                .password("StrongPass123")
                .phone("9876543210")
                .build();

        mockMvc.perform(post("/api/auth/signup/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    void testGetCurrentUser_WithInactiveAccountToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", inactiveUserToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testGetCurrentUser_WithExpiredToken_Returns401() throws Exception {
        JwtService expiredJwtService = new JwtService();
        org.springframework.test.util.ReflectionTestUtils.setField(expiredJwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        org.springframework.test.util.ReflectionTestUtils.setField(expiredJwtService, "jwtExpirationMs", -60000L);
        org.springframework.test.util.ReflectionTestUtils.setField(expiredJwtService, "jwtIssuer", "MessLoc");

        String expiredToken = "Bearer " + expiredJwtService.generateToken(activeUser);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testGetCurrentUser_WithTamperedToken_Returns401() throws Exception {
        String validToken = jwtService.generateToken(activeUser);
        String[] parts = validToken.split("\\.");
        String tamperedToken = "Bearer " + parts[0] + "." + parts[1] + ".tamperedInvalidSignature";

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testResponses_NeverExposeSensitiveDataOrPasswords() throws Exception {
        when(authService.getCurrentUser("shivam@messloc.com"))
                .thenReturn(UserResponse.fromEntity(activeUser));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", activeUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.secret").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
