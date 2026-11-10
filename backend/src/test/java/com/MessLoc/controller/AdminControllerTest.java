package com.MessLoc.controller;

import com.MessLoc.dto.request.UserSignupRequest;
import com.MessLoc.entity.OwnerProfile;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    private MessService messService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

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

    private String adminToken;
    private String userToken;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        User admin = User.builder()
                .id("admin-1")
                .name("Admin User")
                .email("admin@test.com")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        User user = User.builder()
                .id("user-1")
                .name("Regular User")
                .email("user@test.com")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        User owner = User.builder()
                .id("owner-1")
                .name("Owner User")
                .email("owner@test.com")
                .role(Role.OWNER)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userDetailsService.loadUserByUsername("admin@test.com")).thenReturn(new CustomUserDetails(admin));
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(new CustomUserDetails(user));
        when(userDetailsService.loadUserByUsername("owner@test.com")).thenReturn(new CustomUserDetails(owner));

        adminToken = "Bearer " + jwtService.generateToken(admin);
        userToken = "Bearer " + jwtService.generateToken(user);
        ownerToken = "Bearer " + jwtService.generateToken(owner);
    }

    @Test
    void testAdminAccess_Success() throws Exception {
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void testAdminAccess_ForbiddenForUser() throws Exception {
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testAdminAccess_UnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testCreateAdmin_Success() throws Exception {
        UserSignupRequest request = UserSignupRequest.builder()
                .name("New Admin")
                .email("newadmin@messloc.com")
                .password("Admin@1234")
                .phone("9876543210")
                .build();

        User savedAdmin = User.builder()
                .id("admin-2")
                .name("New Admin")
                .email("newadmin@messloc.com")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userRepository.existsByEmail("newadmin@messloc.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedAdmin);

        mockMvc.perform(post("/api/admin/create")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("newadmin@messloc.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void testCreateAdmin_DuplicateEmail_Conflict() throws Exception {
        UserSignupRequest request = UserSignupRequest.builder()
                .name("New Admin")
                .email("existing@messloc.com")
                .password("Admin@1234")
                .phone("9876543210")
                .build();

        when(userRepository.existsByEmail("existing@messloc.com")).thenReturn(true);

        mockMvc.perform(post("/api/admin/create")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testCreateAdmin_ForbiddenForOwner() throws Exception {
        UserSignupRequest request = UserSignupRequest.builder()
                .name("New Admin")
                .email("newadmin@messloc.com")
                .password("Admin@1234")
                .phone("9876543210")
                .build();

        mockMvc.perform(post("/api/admin/create")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testGetPendingOwners_Success() throws Exception {
        User pendingOwner = User.builder()
                .id("po-1")
                .name("Pending Owner")
                .email("po@messloc.com")
                .role(Role.OWNER)
                .status(AccountStatus.PENDING)
                .build();

        when(userRepository.findByRoleAndStatus(Role.OWNER, AccountStatus.PENDING))
                .thenReturn(List.of(pendingOwner));

        mockMvc.perform(get("/api/admin/owners/pending")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value("po@messloc.com"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void testGetPendingOwners_ForbiddenForUser() throws Exception {
        mockMvc.perform(get("/api/admin/owners/pending")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testVerifyOwner_Success() throws Exception {
        User pendingOwner = User.builder()
                .id("po-1")
                .name("Pending Owner")
                .email("po@messloc.com")
                .role(Role.OWNER)
                .status(AccountStatus.PENDING)
                .build();

        OwnerProfile profile = OwnerProfile.builder()
                .id("op-1")
                .userId("po-1")
                .verified(false)
                .build();

        when(userRepository.findById("po-1")).thenReturn(Optional.of(pendingOwner));
        when(ownerProfileRepository.findByUserId("po-1")).thenReturn(Optional.of(profile));

        mockMvc.perform(patch("/api/admin/owners/po-1/verify")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testVerifyOwner_NotFound() throws Exception {
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/admin/owners/non-existent/verify")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testBlockOwner_Success() throws Exception {
        User owner = User.builder()
                .id("o-1")
                .name("Owner")
                .email("o@messloc.com")
                .role(Role.OWNER)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userRepository.findById("o-1")).thenReturn(Optional.of(owner));

        mockMvc.perform(patch("/api/admin/owners/o-1/block")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
