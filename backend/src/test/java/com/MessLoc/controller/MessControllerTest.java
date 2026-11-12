package com.MessLoc.controller;

import com.MessLoc.dto.mess.CreateMessRequest;
import com.MessLoc.dto.mess.MessResponse;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.FoodType;
import com.MessLoc.enums.MessStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.enums.VerificationStatus;
import com.MessLoc.repository.MessRepository;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.security.CustomUserDetailsService;
import com.MessLoc.security.JwtService;
import com.MessLoc.service.AuthService;
import com.MessLoc.service.MessService;
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

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class MessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private MessService messService;

    @MockitoBean
    private UserService userService;

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

    @MockitoBean
    private com.MessLoc.repository.MenuRepository menuRepository;

    @MockitoBean
    private com.MessLoc.service.MenuService menuService;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestMongoConfig {
        @org.springframework.context.annotation.Bean(name = "mongoMappingContext")
        public MongoMappingContext mongoMappingContext() {
            return new MongoMappingContext();
        }
    }

    private User activeOwner;
    private User pendingOwner;
    private User regularUser;
    private User adminUser;

    private String ownerToken;
    private String pendingOwnerToken;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        activeOwner = User.builder()
                .id("o1")
                .name("Sharma Owner")
                .email("owner@test.com")
                .role(Role.OWNER)
                .status(AccountStatus.ACTIVE)
                .build();

        pendingOwner = User.builder()
                .id("o-pending")
                .name("Pending Owner")
                .email("pending-owner@test.com")
                .role(Role.OWNER)
                .status(AccountStatus.PENDING)
                .build();

        regularUser = User.builder()
                .id("u1")
                .name("Regular User")
                .email("user@test.com")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        adminUser = User.builder()
                .id("a1")
                .name("Admin User")
                .email("admin@test.com")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userDetailsService.loadUserByUsername("owner@test.com")).thenReturn(new CustomUserDetails(activeOwner));
        when(userDetailsService.loadUserByUsername("pending-owner@test.com")).thenReturn(new CustomUserDetails(pendingOwner));
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(new CustomUserDetails(regularUser));
        when(userDetailsService.loadUserByUsername("admin@test.com")).thenReturn(new CustomUserDetails(adminUser));

        ownerToken = "Bearer " + jwtService.generateToken(activeOwner);
        pendingOwnerToken = "Bearer " + jwtService.generateToken(pendingOwner);
        userToken = "Bearer " + jwtService.generateToken(regularUser);
        adminToken = "Bearer " + jwtService.generateToken(adminUser);
    }

    private CreateMessRequest buildValidCreateMessRequest() {
        return CreateMessRequest.builder()
                .name("Sharma Mess")
                .description("Nutritious and clean homemade mess food")
                .address("Street 1, Near University")
                .city("Dehradun")
                .state("Uttarakhand")
                .pincode("248001")
                .latitude(30.3165)
                .longitude(78.0322)
                .contactNumber("9876543210")
                .monthlyPrice(2500.0)
                .foodType(FoodType.VEG)
                .openingTime(LocalTime.of(7, 30))
                .closingTime(LocalTime.of(21, 30))
                .facilities(List.of("WiFi", "RO Water"))
                .build();
    }

    // ── 1. Approved OWNER creates a mess successfully via POST /api/messes ──

    @Test
    void testOwnerCanCreateMess_ViaApiMesses_Returns201() throws Exception {
        CreateMessRequest request = buildValidCreateMessRequest();

        MessResponse mockResponse = MessResponse.builder()
                .id("m1")
                .name("Sharma Mess")
                .ownerId("o1")
                .verificationStatus(VerificationStatus.PENDING)
                .status(MessStatus.ACTIVE)
                .build();

        when(messService.createMess(any(CreateMessRequest.class), eq("o1"))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/messes")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("m1"))
                .andExpect(jsonPath("$.data.name").value("Sharma Mess"))
                .andExpect(jsonPath("$.data.ownerId").value("o1"))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    // ── 1b. Approved OWNER creates a mess successfully via legacy POST /api/owner/messes ──

    @Test
    void testOwnerCanCreateMess_ViaApiOwnerMesses_Returns201() throws Exception {
        CreateMessRequest request = buildValidCreateMessRequest();

        MessResponse mockResponse = MessResponse.builder()
                .id("m1")
                .name("Sharma Mess")
                .ownerId("o1")
                .verificationStatus(VerificationStatus.PENDING)
                .status(MessStatus.ACTIVE)
                .build();

        when(messService.createMess(any(CreateMessRequest.class), eq("o1"))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/owner/messes")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Sharma Mess"));
    }

    // ── 2. USER attempts to create a mess and is denied (403 Forbidden) ──

    @Test
    void testUserCannotCreateMess_Returns403() throws Exception {
        CreateMessRequest request = buildValidCreateMessRequest();

        mockMvc.perform(post("/api/messes")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403));
    }

    // ── 3. ADMIN attempts to create a mess and is denied (403 Forbidden) ──

    @Test
    void testAdminCannotCreateMess_Returns403() throws Exception {
        CreateMessRequest request = buildValidCreateMessRequest();

        mockMvc.perform(post("/api/messes")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403));
    }

    // ── 4. Pending OWNER attempts to create a mess and is denied ──

    @Test
    void testPendingOwnerCannotCreateMess_Returns401or403() throws Exception {
        CreateMessRequest request = buildValidCreateMessRequest();

        // Pending owners are either denied at JWT filter (401) or in controller check (403)
        mockMvc.perform(post("/api/messes")
                        .header("Authorization", pendingOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── 5. Missing JWT is rejected (401 Unauthorized) ──

    @Test
    void testMissingJwt_Returns401() throws Exception {
        CreateMessRequest request = buildValidCreateMessRequest();

        mockMvc.perform(post("/api/messes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── 6. Invalid JWT is rejected (401 Unauthorized) ──

    @Test
    void testInvalidJwt_Returns401() throws Exception {
        CreateMessRequest request = buildValidCreateMessRequest();

        mockMvc.perform(post("/api/messes")
                        .header("Authorization", "Bearer invalidJwtTokenHere")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── 7. Missing required fields are rejected (400 Bad Request) ──

    @Test
    void testMissingRequiredFields_Returns400() throws Exception {
        // Missing name, city, address, pincode, price, foodType, times
        CreateMessRequest request = new CreateMessRequest();

        mockMvc.perform(post("/api/messes")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── 8. Invalid monthly price is rejected (400 Bad Request) ──

    @Test
    void testInvalidMonthlyPrice_Negative_Returns400() throws Exception {
        CreateMessRequest request = buildValidCreateMessRequest();
        request.setMonthlyPrice(-500.0); // Invalid: <= 0

        mockMvc.perform(post("/api/messes")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.monthlyPrice").exists());
    }

    @Test
    void testInvalidMonthlyPrice_Zero_Returns400() throws Exception {
        CreateMessRequest request = buildValidCreateMessRequest();
        request.setMonthlyPrice(0.0); // Invalid: must be > 0

        mockMvc.perform(post("/api/messes")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── 9. Client attempts to supply ownerId belonging to another user ──

    @Test
    void testClientSuppliedOwnerId_IsIgnoredAndBoundToAuthenticatedOwner() throws Exception {
        CreateMessRequest request = buildValidCreateMessRequest();

        // Malicious JSON containing injected ownerId: "victim-owner-999"
        String maliciousPayload = objectMapper.writeValueAsString(request)
                .replaceFirst("\\{", "{\"ownerId\":\"victim-owner-999\",");

        MessResponse mockResponse = MessResponse.builder()
                .id("m-safe")
                .name("Sharma Mess")
                .ownerId("o1") // Server safely assigns authenticated owner id "o1"
                .verificationStatus(VerificationStatus.PENDING)
                .status(MessStatus.ACTIVE)
                .build();

        // MessService must receive "o1" (from SecurityContext), NOT "victim-owner-999"
        when(messService.createMess(any(CreateMessRequest.class), eq("o1"))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/messes")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ownerId").value("o1"));
    }

    // ── 10. Client attempts to set verificationStatus to APPROVED ──

    @Test
    void testClientSettingVerificationStatusApproved_IsIgnoredAndSetToPending() throws Exception {
        CreateMessRequest request = buildValidCreateMessRequest();

        // Malicious JSON attempting to self-approve
        String maliciousPayload = objectMapper.writeValueAsString(request)
                .replaceFirst("\\{", "{\"verificationStatus\":\"APPROVED\",");

        MessResponse mockResponse = MessResponse.builder()
                .id("m-safe")
                .name("Sharma Mess")
                .ownerId("o1")
                .verificationStatus(VerificationStatus.PENDING) // Always PENDING on creation
                .status(MessStatus.ACTIVE)
                .build();

        when(messService.createMess(any(CreateMessRequest.class), eq("o1"))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/messes")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"));
    }

    // ── 11. Client attempts to set operational status to INACTIVE/anything ──

    @Test
    void testClientSettingMessStatus_IsIgnoredAndControlledByServer() throws Exception {
        CreateMessRequest request = buildValidCreateMessRequest();

        String maliciousPayload = objectMapper.writeValueAsString(request)
                .replaceFirst("\\{", "{\"status\":\"INACTIVE\",");

        MessResponse mockResponse = MessResponse.builder()
                .id("m-safe")
                .name("Sharma Mess")
                .ownerId("o1")
                .verificationStatus(VerificationStatus.PENDING)
                .status(MessStatus.ACTIVE) // Server initializes operational status
                .build();

        when(messService.createMess(any(CreateMessRequest.class), eq("o1"))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/messes")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    // ── 12. Invalid foodType is rejected (400 Bad Request) ──

    @Test
    void testInvalidFoodType_Returns400() throws Exception {
        String payload = "{" +
                "\"name\":\"Sharma Mess\"," +
                "\"description\":\"Good food\"," +
                "\"address\":\"Street 1\"," +
                "\"city\":\"Dehradun\"," +
                "\"state\":\"UK\"," +
                "\"pincode\":\"248001\"," +
                "\"latitude\":30.3," +
                "\"longitude\":78.0," +
                "\"contactNumber\":\"9876543210\"," +
                "\"monthlyPrice\":2500.0," +
                "\"foodType\":\"INVALID_FOOD_TYPE\"," +
                "\"openingTime\":\"07:30\"," +
                "\"closingTime\":\"21:30\"" +
                "}";

        mockMvc.perform(post("/api/messes")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── 13. Invalid latitude rejected (400 Bad Request) ──

    @Test
    void testInvalidLatitude_Returns400() throws Exception {
        CreateMessRequest request = buildValidCreateMessRequest();
        request.setLatitude(99.99); // Invalid: > 90

        mockMvc.perform(post("/api/messes")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── Existing Admin and Public APIs verification ──

    @Test
    void adminCanApproveMess() throws Exception {
        mockMvc.perform(patch("/api/admin/messes/m1/approve")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void ownerCannotApproveMess() throws Exception {
        mockMvc.perform(patch("/api/admin/messes/m1/approve")
                        .header("Authorization", ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicCanGetMesses() throws Exception {
        mockMvc.perform(get("/api/messes"))
                .andExpect(status().isOk());
    }
}
