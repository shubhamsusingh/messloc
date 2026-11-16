package com.MessLoc.controller;

import com.MessLoc.dto.subscription.CreateSubscriptionRequest;
import com.MessLoc.dto.subscription.SubscriptionResponse;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.enums.SubscriptionStatus;
import com.MessLoc.repository.MenuRepository;
import com.MessLoc.repository.MessRepository;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.SubscriptionRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.security.CustomUserDetailsService;
import com.MessLoc.security.JwtService;
import com.MessLoc.service.AuthService;
import com.MessLoc.service.MenuService;
import com.MessLoc.service.MessService;
import com.MessLoc.service.SubscriptionService;
import com.MessLoc.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
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
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private SubscriptionRepository subscriptionRepository;

    @MockitoBean
    private MenuService menuService;

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
    private MenuRepository menuRepository;

    @MockitoBean
    private com.MessLoc.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestMongoConfig {
        @org.springframework.context.annotation.Bean(name = "mongoMappingContext")
        public MongoMappingContext mongoMappingContext() {
            return new MongoMappingContext();
        }
    }

    private User regularUser;
    private User messOwner;
    private User adminUser;

    private String userToken;
    private String ownerToken;
    private String adminToken;

    private SubscriptionResponse sampleResponse;

    @BeforeEach
    void setUp() {
        regularUser = User.builder()
                .id("u-101")
                .name("Aman Verma")
                .email("aman.user@example.com")
                .phone("9876543210")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        messOwner = User.builder()
                .id("o-201")
                .name("Ramesh Owner")
                .email("ramesh.owner@example.com")
                .phone("9123456780")
                .role(Role.OWNER)
                .status(AccountStatus.ACTIVE)
                .build();

        adminUser = User.builder()
                .id("a-301")
                .name("Admin User")
                .email("admin@messloc.com")
                .phone("9999999999")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userDetailsService.loadUserByUsername("aman.user@example.com"))
                .thenReturn(new CustomUserDetails(regularUser));
        when(userDetailsService.loadUserByUsername("ramesh.owner@example.com"))
                .thenReturn(new CustomUserDetails(messOwner));
        when(userDetailsService.loadUserByUsername("admin@messloc.com"))
                .thenReturn(new CustomUserDetails(adminUser));

        when(userRepository.findByEmail("aman.user@example.com")).thenReturn(Optional.of(regularUser));
        when(userRepository.findByEmail("ramesh.owner@example.com")).thenReturn(Optional.of(messOwner));
        when(userRepository.findByEmail("admin@messloc.com")).thenReturn(Optional.of(adminUser));

        userToken = "Bearer " + jwtService.generateToken(regularUser);
        ownerToken = "Bearer " + jwtService.generateToken(messOwner);
        adminToken = "Bearer " + jwtService.generateToken(adminUser);

        sampleResponse = SubscriptionResponse.builder()
                .id("sub-999")
                .userId("u-101")
                .userName("Aman Verma")
                .userEmail("aman.user@example.com")
                .userPhone("9876543210")
                .messId("mess-111")
                .messName("Annapurna Mess")
                .planId("monthly")
                .planName("Monthly Plan")
                .durationDays(30)
                .price(3000.0)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .status(SubscriptionStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // POST /api/subscriptions
    // =========================================================================

    @Test
    @DisplayName("Authenticated USER can create a subscription -> 201 Created")
    void testCreateSubscription_AsUser_201() throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .messId("mess-111")
                .planId("monthly")
                .build();

        when(subscriptionService.createSubscription(any(CreateSubscriptionRequest.class), anyString()))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value("sub-999"))
                .andExpect(jsonPath("$.data.messName").value("Annapurna Mess"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("OWNER cannot create subscriptions -> 403 Forbidden")
    void testCreateSubscription_AsOwner_403() throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .messId("mess-111")
                .build();

        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("ADMIN cannot create subscriptions -> 403 Forbidden")
    void testCreateSubscription_AsAdmin_403() throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .messId("mess-111")
                .build();

        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Unauthenticated request is rejected -> 401 Unauthorized")
    void testCreateSubscription_Unauthenticated_401() throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .messId("mess-111")
                .build();

        mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Blank messId triggers validation error -> 400 Bad Request")
    void testCreateSubscription_BlankMessId_400() throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .messId("")
                .build();

        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));
    }

    // =========================================================================
    // GET /api/subscriptions/me
    // =========================================================================

    @Test
    @DisplayName("Authenticated USER can view their subscriptions -> 200 OK")
    void testGetMySubscriptions_AsUser_200() throws Exception {
        when(subscriptionService.getUserSubscriptions(anyString(), any()))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/subscriptions/me")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("sub-999"));
    }

    @Test
    @DisplayName("Supports status query filter -> 200 OK")
    void testGetMySubscriptions_WithStatusFilter_200() throws Exception {
        when(subscriptionService.getUserSubscriptions(anyString(), eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/subscriptions/me")
                        .param("status", "ACTIVE")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("OWNER cannot access /subscriptions/me -> 403 Forbidden")
    void testGetMySubscriptions_AsOwner_403() throws Exception {
        mockMvc.perform(get("/api/subscriptions/me")
                        .header("Authorization", ownerToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/subscriptions/{subscriptionId}
    // =========================================================================

    @Test
    @DisplayName("Subscriber USER can view details -> 200 OK")
    void testGetSubscriptionById_AsUser_200() throws Exception {
        when(subscriptionService.getSubscriptionById(eq("sub-999"), anyString(), anyBoolean()))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/subscriptions/sub-999")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("sub-999"));
    }

    @Test
    @DisplayName("ADMIN can view details -> 200 OK")
    void testGetSubscriptionById_AsAdmin_200() throws Exception {
        when(subscriptionService.getSubscriptionById(eq("sub-999"), anyString(), eq(true)))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/subscriptions/sub-999")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("sub-999"));
    }

    @Test
    @DisplayName("OWNER cannot view subscription details -> 403 Forbidden")
    void testGetSubscriptionById_AsOwner_403() throws Exception {
        mockMvc.perform(get("/api/subscriptions/sub-999")
                        .header("Authorization", ownerToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // PATCH /api/subscriptions/{subscriptionId}/cancel
    // =========================================================================

    @Test
    @DisplayName("Authenticated USER can cancel subscription -> 200 OK")
    void testCancelSubscription_AsUser_200() throws Exception {
        SubscriptionResponse cancelled = SubscriptionResponse.builder()
                .id("sub-999")
                .status(SubscriptionStatus.CANCELLED)
                .cancelledAt(Instant.now())
                .build();

        when(subscriptionService.cancelSubscription(eq("sub-999"), anyString()))
                .thenReturn(cancelled);

        mockMvc.perform(patch("/api/subscriptions/sub-999/cancel")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("OWNER cannot cancel subscription -> 403 Forbidden")
    void testCancelSubscription_AsOwner_403() throws Exception {
        mockMvc.perform(patch("/api/subscriptions/sub-999/cancel")
                        .header("Authorization", ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request is rejected -> 401 Unauthorized")
    void testCancelSubscription_Unauthenticated_401() throws Exception {
        mockMvc.perform(patch("/api/subscriptions/sub-999/cancel"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // GET /api/subscriptions/owner/mess/{messId}
    // =========================================================================

    @Test
    @DisplayName("Authenticated OWNER can view subscriptions for their mess -> 200 OK")
    void testGetOwnerMessSubscriptions_AsOwner_200() throws Exception {
        when(subscriptionService.getOwnerMessSubscriptions(eq("mess-111"), anyString(), any()))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/subscriptions/owner/mess/mess-111")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("sub-999"))
                .andExpect(jsonPath("$.data[0].userName").value("Aman Verma"));
    }

    @Test
    @DisplayName("USER cannot access owner mess subscriptions -> 403 Forbidden")
    void testGetOwnerMessSubscriptions_AsUser_403() throws Exception {
        mockMvc.perform(get("/api/subscriptions/owner/mess/mess-111")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN cannot access owner-specific mess subscriptions -> 403 Forbidden")
    void testGetOwnerMessSubscriptions_AsAdmin_403() throws Exception {
        mockMvc.perform(get("/api/subscriptions/owner/mess/mess-111")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/admin/subscriptions
    // =========================================================================

    @Test
    @DisplayName("ADMIN can view all subscriptions -> 200 OK")
    void testGetAllSubscriptions_AsAdmin_200() throws Exception {
        when(subscriptionService.getAllSubscriptions(any()))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/admin/subscriptions")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("sub-999"));
    }

    @Test
    @DisplayName("USER cannot access admin subscriptions -> 403 Forbidden")
    void testGetAllSubscriptions_AsUser_403() throws Exception {
        mockMvc.perform(get("/api/admin/subscriptions")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OWNER cannot access admin subscriptions -> 403 Forbidden")
    void testGetAllSubscriptions_AsOwner_403() throws Exception {
        mockMvc.perform(get("/api/admin/subscriptions")
                        .header("Authorization", ownerToken))
                .andExpect(status().isForbidden());
    }
}
