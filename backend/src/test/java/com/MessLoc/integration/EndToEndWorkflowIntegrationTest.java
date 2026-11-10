package com.MessLoc.integration;

import com.MessLoc.dto.menu.CreateMenuRequest;
import com.MessLoc.dto.menu.MenuItemRequest;
import com.MessLoc.dto.menu.UpdateMenuRequest;
import com.MessLoc.dto.mess.CreateMessRequest;
import com.MessLoc.dto.mess.UpdateMessRequest;
import com.MessLoc.dto.request.OwnerSignupRequest;
import com.MessLoc.dto.request.SigninRequest;
import com.MessLoc.dto.request.UpdateUserProfileRequest;
import com.MessLoc.dto.request.UserSignupRequest;
import com.MessLoc.entity.Menu;
import com.MessLoc.entity.MenuItem;
import com.MessLoc.entity.Mess;
import com.MessLoc.entity.OwnerProfile;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.FoodType;
import com.MessLoc.enums.MealType;
import com.MessLoc.enums.MessStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.enums.VerificationStatus;
import com.MessLoc.repository.MessRepository;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.security.CustomUserDetailsService;
import com.MessLoc.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
public class EndToEndWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    @org.springframework.boot.test.context.TestConfiguration
    static class TestMongoConfig {
        @org.springframework.context.annotation.Bean(name = "mongoMappingContext")
        public MongoMappingContext mongoMappingContext() {
            return new MongoMappingContext();
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    // In-memory repositories for realistic stateful workflow progression
    private final Map<String, User> userDatabase = new ConcurrentHashMap<>();
    private final Map<String, OwnerProfile> ownerProfileDatabase = new ConcurrentHashMap<>();
    private final Map<String, Mess> messDatabase = new ConcurrentHashMap<>();
    private final Map<String, Menu> menuDatabase = new ConcurrentHashMap<>();

    private User adminUser;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userDatabase.clear();
        ownerProfileDatabase.clear();
        messDatabase.clear();
        menuDatabase.clear();

        // 1. Setup seed admin user
        adminUser = User.builder()
                .id("admin-id-1")
                .name("Admin Administrator")
                .email("admin@messloc.com")
                .password(passwordEncoder.encode("Admin@MessLoc123"))
                .phone("9999999999")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();
        userDatabase.put(adminUser.getEmail(), adminUser);
        adminToken = "Bearer " + jwtService.generateToken(adminUser);

        // 2. Mock UserRepository dynamic in-memory behavior
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            if (u.getId() == null) {
                u.setId("user-" + UUID.randomUUID().toString().substring(0, 8));
            }
            userDatabase.put(u.getEmail().toLowerCase().trim(), u);
            return u;
        });

        when(userRepository.findByEmail(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            return Optional.ofNullable(userDatabase.get(email.toLowerCase().trim()));
        });

        when(userRepository.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return userDatabase.values().stream().filter(u -> u.getId().equals(id)).findFirst();
        });

        when(userRepository.existsByEmail(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            return userDatabase.containsKey(email.toLowerCase().trim());
        });

        when(userRepository.findByRoleAndStatus(any(Role.class), any(AccountStatus.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            AccountStatus status = invocation.getArgument(1);
            return userDatabase.values().stream()
                    .filter(u -> u.getRole() == role && u.getStatus() == status)
                    .toList();
        });

        // 3. Mock UserDetailsService
        when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            User user = userDatabase.get(email.toLowerCase().trim());
            if (user == null) {
                throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Not found: " + email);
            }
            return new CustomUserDetails(user);
        });

        // 4. Mock OwnerProfileRepository
        when(ownerProfileRepository.save(any(OwnerProfile.class))).thenAnswer(invocation -> {
            OwnerProfile p = invocation.getArgument(0);
            if (p.getId() == null) {
                p.setId("op-" + UUID.randomUUID().toString().substring(0, 8));
            }
            ownerProfileDatabase.put(p.getUserId(), p);
            return p;
        });

        when(ownerProfileRepository.findByUserId(anyString())).thenAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            return Optional.ofNullable(ownerProfileDatabase.get(userId));
        });

        // 5. Mock MessRepository
        when(messRepository.save(any(Mess.class))).thenAnswer(invocation -> {
            Mess m = invocation.getArgument(0);
            if (m.getId() == null) {
                m.setId("mess-" + UUID.randomUUID().toString().substring(0, 8));
            }
            messDatabase.put(m.getId(), m);
            return m;
        });

        when(messRepository.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Optional.ofNullable(messDatabase.get(id));
        });

        when(messRepository.findByOwnerId(anyString())).thenAnswer(invocation -> {
            String ownerId = invocation.getArgument(0);
            return messDatabase.values().stream().filter(m -> m.getOwnerId().equals(ownerId)).toList();
        });

        when(messRepository.findByVerificationStatus(any(VerificationStatus.class))).thenAnswer(invocation -> {
            VerificationStatus status = invocation.getArgument(0);
            return messDatabase.values().stream().filter(m -> m.getVerificationStatus() == status).toList();
        });

        when(messRepository.findByVerificationStatusAndStatus(any(VerificationStatus.class), any(MessStatus.class))).thenAnswer(invocation -> {
            VerificationStatus vStatus = invocation.getArgument(0);
            MessStatus mStatus = invocation.getArgument(1);
            return messDatabase.values().stream()
                    .filter(m -> m.getVerificationStatus() == vStatus && m.getStatus() == mStatus)
                    .toList();
        });

        when(messRepository.findByVerificationStatusAndStatusAndCityIgnoreCase(any(VerificationStatus.class), any(MessStatus.class), anyString())).thenAnswer(invocation -> {
            VerificationStatus vStatus = invocation.getArgument(0);
            MessStatus mStatus = invocation.getArgument(1);
            String city = invocation.getArgument(2);
            return messDatabase.values().stream()
                    .filter(m -> m.getVerificationStatus() == vStatus && m.getStatus() == mStatus && m.getCity().equalsIgnoreCase(city))
                    .toList();
        });

        // 5. Mock MenuRepository dynamic in-memory behavior
        when(menuRepository.save(any(Menu.class))).thenAnswer(invocation -> {
            Menu m = invocation.getArgument(0);
            if (m.getId() == null) {
                m.setId("menu-" + UUID.randomUUID().toString().substring(0, 8));
            }
            menuDatabase.put(m.getId(), m);
            return m;
        });

        when(menuRepository.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Optional.ofNullable(menuDatabase.get(id));
        });

        when(menuRepository.findByMessIdAndMenuDate(anyString(), any(LocalDate.class))).thenAnswer(invocation -> {
            String messId = invocation.getArgument(0);
            LocalDate date = invocation.getArgument(1);
            return menuDatabase.values().stream()
                    .filter(m -> m.getMessId().equals(messId) && m.getMenuDate().equals(date))
                    .findFirst();
        });

        when(menuRepository.findByMessIdAndMenuDateBetweenOrderByMenuDateAsc(anyString(), any(LocalDate.class), any(LocalDate.class))).thenAnswer(invocation -> {
            String messId = invocation.getArgument(0);
            LocalDate start = invocation.getArgument(1);
            LocalDate end = invocation.getArgument(2);
            return menuDatabase.values().stream()
                    .filter(m -> m.getMessId().equals(messId) && !m.getMenuDate().isBefore(start) && !m.getMenuDate().isAfter(end))
                    .sorted(Comparator.comparing(Menu::getMenuDate))
                    .toList();
        });

        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            menuDatabase.remove(id);
            return null;
        }).when(menuRepository).deleteById(anyString());
    }

    // =========================================================================
    // WORKFLOW A: USER LIFECYCLE
    // =========================================================================

    @Test
    @DisplayName("Workflow A: User Registration, Signin, Identity, Profile Get and Update")
    void testWorkflowA_UserLifecycle() throws Exception {
        // Step 1: Register USER
        UserSignupRequest signupReq = UserSignupRequest.builder()
                .name("Aman Verma")
                .email("aman.verma@example.com")
                .password("AmanPass123")
                .phone("9876543210")
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        // Step 2: Signin with credentials
        SigninRequest signinReq = SigninRequest.builder()
                .email("aman.verma@example.com")
                .password("AmanPass123")
                .build();

        MvcResult signinResult = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signinReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode signinJson = objectMapper.readTree(signinResult.getResponse().getContentAsString());
        String userToken = "Bearer " + signinJson.get("token").asText();

        // Step 3: Access /api/auth/me
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("aman.verma@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));

        // Step 4: Access /api/users/me
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Aman Verma"))
                .andExpect(jsonPath("$.data.phone").value("9876543210"));

        // Step 5: Update profile via PUT /api/users/me
        UpdateUserProfileRequest updateReq = UpdateUserProfileRequest.builder()
                .name("Aman V. Sharma")
                .phone("9876500000")
                .city("Dehradun")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Aman V. Sharma"))
                .andExpect(jsonPath("$.data.city").value("Dehradun"));

        // Step 6: Verify updated profile
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Aman V. Sharma"))
                .andExpect(jsonPath("$.data.city").value("Dehradun"))
                .andExpect(jsonPath("$.data.phone").value("9876500000"));
    }

    // =========================================================================
    // WORKFLOW B: OWNER LIFECYCLE
    // =========================================================================

    @Test
    @DisplayName("Workflow B: Owner Registration (PENDING), Signin Blocked, Admin Verification, Signin Success, Mess Creation & Management")
    void testWorkflowB_OwnerLifecycle() throws Exception {
        // Step 1: Register OWNER
        OwnerSignupRequest ownerSignupReq = OwnerSignupRequest.builder()
                .name("Karan Joshi")
                .email("karan.owner@example.com")
                .password("OwnerPass123")
                .phone("9812345678")
                .businessName("Karan Bhojnalaya")
                .businessDescription("Healthy homestyle thali and mess services")
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup/owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerSignupReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.role").value("OWNER"))
                .andExpect(jsonPath("$.user.status").value("PENDING"))
                .andExpect(jsonPath("$.token").isEmpty()) // No JWT on registration for PENDING owners
                .andReturn();

        JsonNode signupJson = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        String ownerUserId = signupJson.get("user").get("id").asText();

        // Step 2: Attempt Signin -> Expect 403 Forbidden because status is PENDING
        SigninRequest signinReq = SigninRequest.builder()
                .email("karan.owner@example.com")
                .password("OwnerPass123")
                .build();

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signinReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // Step 3: Admin inspects pending owners
        mockMvc.perform(get("/api/admin/owners/pending")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value("karan.owner@example.com"));

        // Step 4: Admin verifies owner account
        mockMvc.perform(patch("/api/admin/owners/" + ownerUserId + "/verify")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 5: Owner signs in successfully now that status is ACTIVE
        MvcResult ownerSigninResult = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signinReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String ownerToken = "Bearer " + objectMapper.readTree(ownerSigninResult.getResponse().getContentAsString()).get("token").asText();

        // Step 6: Owner creates a mess
        CreateMessRequest messReq = CreateMessRequest.builder()
                .name("Karan Mess & Tiffin")
                .description("Pure veg North Indian meals")
                .address("Plot 12, Clock Tower")
                .city("Dehradun")
                .state("Uttarakhand")
                .pincode("248001")
                .latitude(30.3165)
                .longitude(78.0322)
                .contactNumber("9812345678")
                .monthlyPrice(3200.0)
                .foodType(FoodType.VEG)
                .openingTime(LocalTime.of(7, 30))
                .closingTime(LocalTime.of(22, 0))
                .facilities(List.of("WiFi", "RO Water", "Parking"))
                .build();

        MvcResult messCreateResult = mockMvc.perform(post("/api/messes")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.name").value("Karan Mess & Tiffin"))
                .andReturn();

        String messId = objectMapper.readTree(messCreateResult.getResponse().getContentAsString()).get("data").get("id").asText();

        // Step 7: Owner views their mess
        mockMvc.perform(get("/api/owner/messes")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(messId));

        // Step 8: Owner updates their mess
        UpdateMessRequest updateMessReq = UpdateMessRequest.builder()
                .name("Karan Deluxe Mess")
                .description("Updated pure veg meals with special Sunday sweet")
                .address("Plot 12, Clock Tower")
                .city("Dehradun")
                .state("Uttarakhand")
                .pincode("248001")
                .latitude(30.3165)
                .longitude(78.0322)
                .contactNumber("9812345678")
                .monthlyPrice(3500.0)
                .foodType(FoodType.VEG)
                .openingTime(LocalTime.of(7, 0))
                .closingTime(LocalTime.of(22, 30))
                .facilities(List.of("WiFi", "RO Water", "Parking", "AC Dining"))
                .status(MessStatus.ACTIVE)
                .build();

        mockMvc.perform(put("/api/owner/messes/" + messId)
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateMessReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Karan Deluxe Mess"));

        // Step 9: Owner deactivates mess
        mockMvc.perform(patch("/api/owner/messes/" + messId + "/deactivate")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(MessStatus.INACTIVE, messDatabase.get(messId).getStatus());

        // Step 10: Owner activates mess again
        mockMvc.perform(patch("/api/owner/messes/" + messId + "/activate")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(MessStatus.ACTIVE, messDatabase.get(messId).getStatus());
    }

    // =========================================================================
    // WORKFLOW C: ADMIN MESS APPROVAL & PUBLIC DISCOVERY
    // =========================================================================

    @Test
    @DisplayName("Workflow C: Admin Mess Approval, Public Discovery Visibility, and Operational Status Toggling")
    void testWorkflowC_AdminMessApprovalAndDiscovery() throws Exception {
        // Setup: Active owner with an existing mess
        User owner = User.builder()
                .id("owner-seed-1")
                .name("Suresh Owner")
                .email("suresh@example.com")
                .password(passwordEncoder.encode("Pass123"))
                .role(Role.OWNER)
                .status(AccountStatus.ACTIVE)
                .build();
        userDatabase.put(owner.getEmail(), owner);
        String ownerToken = "Bearer " + jwtService.generateToken(owner);

        Mess mess = Mess.builder()
                .id("mess-seed-1")
                .ownerId(owner.getId())
                .name("Suresh Thali House")
                .description("Tasty food")
                .city("Dehradun")
                .address("Rajpur Road")
                .state("UK")
                .pincode("248001")
                .latitude(30.3)
                .longitude(78.0)
                .contactNumber("9876543210")
                .monthlyPrice(3000.0)
                .foodType(FoodType.BOTH)
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(21, 0))
                .verificationStatus(VerificationStatus.PENDING)
                .status(MessStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        messDatabase.put(mess.getId(), mess);

        // Step 1: Public discovery should NOT return pending mess
        mockMvc.perform(get("/api/messes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        // Step 2: Admin fetches pending messes
        mockMvc.perform(get("/api/admin/messes/pending")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("mess-seed-1"));

        // Step 3: Admin approves the mess
        mockMvc.perform(patch("/api/admin/messes/mess-seed-1/approve")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(VerificationStatus.APPROVED, messDatabase.get("mess-seed-1").getVerificationStatus());

        // Step 4: Public discovery now displays the mess
        mockMvc.perform(get("/api/messes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Suresh Thali House"));

        // Step 5: City search filters correctly
        mockMvc.perform(get("/api/messes?city=Dehradun"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Suresh Thali House"));

        mockMvc.perform(get("/api/messes?city=Mumbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        // Step 6: Owner deactivates mess
        mockMvc.perform(patch("/api/owner/messes/mess-seed-1/deactivate")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk());

        // Step 7: Public discovery no longer returns inactive mess
        mockMvc.perform(get("/api/messes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        // Step 8: Owner activates mess again
        mockMvc.perform(patch("/api/owner/messes/mess-seed-1/activate")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk());

        // Step 9: Public discovery returns it again
        mockMvc.perform(get("/api/messes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Suresh Thali House"));
    }

    // =========================================================================
    // WORKFLOW D: SECURITY, RBAC & AUTHORIZATION BOUNDARIES
    // =========================================================================

    @Test
    @DisplayName("Workflow D: Strict Role Authorization, Token Expiration, and Cross-Tenant Isolation")
    void testWorkflowD_SecurityBoundaries() throws Exception {
        // Setup: 1 User, 2 Owners
        User regularUser = User.builder().id("u1").email("user1@example.com").role(Role.USER).status(AccountStatus.ACTIVE).build();
        User owner1 = User.builder().id("o1").email("owner1@example.com").role(Role.OWNER).status(AccountStatus.ACTIVE).build();
        User owner2 = User.builder().id("o2").email("owner2@example.com").role(Role.OWNER).status(AccountStatus.ACTIVE).build();
        User blockedUser = User.builder().id("b1").email("blocked@example.com").password(passwordEncoder.encode("Pass")).role(Role.USER).status(AccountStatus.BLOCKED).build();

        userDatabase.put(regularUser.getEmail(), regularUser);
        userDatabase.put(owner1.getEmail(), owner1);
        userDatabase.put(owner2.getEmail(), owner2);
        userDatabase.put(blockedUser.getEmail(), blockedUser);

        String userToken = "Bearer " + jwtService.generateToken(regularUser);
        String owner1Token = "Bearer " + jwtService.generateToken(owner1);
        String owner2Token = "Bearer " + jwtService.generateToken(owner2);

        Mess owner1Mess = Mess.builder()
                .id("m1")
                .ownerId(owner1.getId())
                .name("Owner1 Mess")
                .verificationStatus(VerificationStatus.APPROVED)
                .status(MessStatus.ACTIVE)
                .build();
        messDatabase.put(owner1Mess.getId(), owner1Mess);

        // 1. USER attempts ADMIN API -> 403 Forbidden
        mockMvc.perform(get("/api/admin/test").header("Authorization", userToken))
                .andExpect(status().isForbidden());

        // 2. OWNER attempts ADMIN API -> 403 Forbidden
        mockMvc.perform(get("/api/admin/test").header("Authorization", owner1Token))
                .andExpect(status().isForbidden());

        // 3. Unauthenticated client attempts protected API -> 401 Unauthorized
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        // 4. Invalid JWT attempts protected API -> 401 Unauthorized
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());

        // 5. Blocked user signin -> 403 Forbidden
        SigninRequest blockedSignin = SigninRequest.builder().email("blocked@example.com").password("Pass").build();
        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockedSignin)))
                .andExpect(status().isForbidden());

        // 6. Owner 2 attempts to update Owner 1's mess -> 403 Forbidden
        UpdateMessRequest maliciousUpdate = UpdateMessRequest.builder()
                .name("Hacked Name")
                .description("Hacked")
                .address("Hacked")
                .city("Hacked")
                .state("Hacked")
                .pincode("248001")
                .latitude(30.0)
                .longitude(78.0)
                .contactNumber("9876543210")
                .monthlyPrice(100.0)
                .foodType(FoodType.VEG)
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(20, 0))
                .build();

        mockMvc.perform(put("/api/owner/messes/m1")
                        .header("Authorization", owner2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousUpdate)))
                .andExpect(status().isForbidden());

        // 7. Client attempts to escalate role during registration -> Role remains USER
        String maliciousSignupPayload = "{" +
                "\"name\":\"Attacker\"," +
                "\"email\":\"attacker@example.com\"," +
                "\"password\":\"Secret12345\"," +
                "\"phone\":\"9998887776\"," +
                "\"role\":\"ADMIN\"," +
                "\"status\":\"ACTIVE\"" +
                "}";

        mockMvc.perform(post("/api/auth/signup/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousSignupPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    // =========================================================================
    // WORKFLOW E: MENU MANAGEMENT LIFECYCLE
    // =========================================================================

    @Test
    @DisplayName("Workflow E: Owner Creates Menu, Adds Item, Updates Item, Fetches Weekly View, Public Reads Menu, Deletes Item/Menu")
    void testWorkflowE_MenuManagementLifecycle() throws Exception {
        // Setup: Active owner with an approved & active mess
        User owner = User.builder()
                .id("owner-menu-1")
                .name("Ramesh Chef")
                .email("ramesh.chef@example.com")
                .password(passwordEncoder.encode("ChefPass123"))
                .role(Role.OWNER)
                .status(AccountStatus.ACTIVE)
                .build();
        userDatabase.put(owner.getEmail(), owner);
        String ownerToken = "Bearer " + jwtService.generateToken(owner);

        Mess mess = Mess.builder()
                .id("mess-menu-1")
                .ownerId(owner.getId())
                .name("Ramesh Punjabi Dhaba")
                .city("Dehradun")
                .verificationStatus(VerificationStatus.APPROVED)
                .status(MessStatus.ACTIVE)
                .build();
        messDatabase.put(mess.getId(), mess);

        LocalDate today = LocalDate.now();

        // Step 1: Owner creates daily menu for today with Breakfast & Lunch
        CreateMenuRequest createReq = CreateMenuRequest.builder()
                .messId("mess-menu-1")
                .menuDate(today)
                .items(List.of(
                        MenuItemRequest.builder().mealType(MealType.BREAKFAST).foodName("Aloo Paratha").description("With fresh curd and butter").build(),
                        MenuItemRequest.builder().mealType(MealType.LUNCH).foodName("Dal Makhani").description("Slow-cooked black lentils").build()
                ))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/menus")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messId").value("mess-menu-1"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andReturn();

        String menuId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("id").asText();
        assertNotNull(menuId);

        // Step 2: Prevent duplicate menu for same mess and date -> 409 Conflict
        mockMvc.perform(post("/api/menus")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        // Step 3: Owner adds dinner item to the menu
        MenuItemRequest addItemReq = MenuItemRequest.builder()
                .mealType(MealType.DINNER)
                .foodName("Paneer Butter Masala")
                .description("Rich cottage cheese gravy")
                .build();

        MvcResult addItemResult = mockMvc.perform(post("/api/menus/" + menuId + "/items")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[2].foodName").value("Paneer Butter Masala"))
                .andExpect(jsonPath("$.data.items[2].mealType").value("DINNER"))
                .andReturn();

        JsonNode menuJson = objectMapper.readTree(addItemResult.getResponse().getContentAsString()).get("data");
        String addedItemId = menuJson.get("items").get(2).get("id").asText();
        assertNotNull(addedItemId);

        // Step 4: Owner views weekly menu (7-day view)
        mockMvc.perform(get("/api/menus/owner/mess/mess-menu-1/weekly")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.menus.length()").value(7));

        // Step 5: Public user views today's menu (Unauthenticated)
        mockMvc.perform(get("/api/menus/mess/mess-menu-1/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.breakfast[0].foodName").value("Aloo Paratha"))
                .andExpect(jsonPath("$.data.lunch[0].foodName").value("Dal Makhani"))
                .andExpect(jsonPath("$.data.dinner[0].foodName").value("Paneer Butter Masala"));

        // Step 6: Public user views weekly menu (Unauthenticated)
        mockMvc.perform(get("/api/menus/mess/mess-menu-1/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.menus.length()").value(7));

        // Step 7: Update item
        MenuItemRequest updateItemReq = MenuItemRequest.builder()
                .mealType(MealType.DINNER)
                .foodName("Shahi Paneer")
                .description("Creamy tomato gravy")
                .build();

        mockMvc.perform(put("/api/menus/" + menuId + "/items/" + addedItemId)
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateItemReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[2].foodName").value("Shahi Paneer"));

        // Step 8: Delete item
        mockMvc.perform(delete("/api/menus/" + menuId + "/items/" + addedItemId)
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 9: Delete menu
        mockMvc.perform(delete("/api/menus/" + menuId)
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
