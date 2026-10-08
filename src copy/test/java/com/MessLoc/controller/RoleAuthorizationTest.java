package com.MessLoc.controller;

import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.security.CustomUserDetailsService;
import com.MessLoc.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class RoleAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OwnerProfileRepository ownerProfileRepository;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestMongoConfig {
        @org.springframework.context.annotation.Bean(name = "mongoMappingContext")
        public org.springframework.data.mongodb.core.mapping.MongoMappingContext mongoMappingContext() {
            return new org.springframework.data.mongodb.core.mapping.MongoMappingContext();
        }
    }

    private String userToken;
    private String ownerToken;
    private String adminToken;

    @BeforeEach
    void setup() {
        User user = User.builder()
                .id("u1")
                .name("Test User")
                .email("user@test.com")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        User owner = User.builder()
                .id("o1")
                .name("Test Owner")
                .email("owner@test.com")
                .role(Role.OWNER)
                .status(AccountStatus.ACTIVE)
                .build();

        User admin = User.builder()
                .id("a1")
                .name("Test Admin")
                .email("admin@test.com")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(new CustomUserDetails(user));
        when(userDetailsService.loadUserByUsername("owner@test.com")).thenReturn(new CustomUserDetails(owner));
        when(userDetailsService.loadUserByUsername("admin@test.com")).thenReturn(new CustomUserDetails(admin));

        userToken = "Bearer " + jwtService.generateToken(user);
        ownerToken = "Bearer " + jwtService.generateToken(owner);
        adminToken = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void testUnauthenticated_AccessDenied_401() throws Exception {
        mockMvc.perform(get("/api/user/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testUserEndpoint_WithUserToken_200() throws Exception {
        mockMvc.perform(get("/api/user/test").header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void testUserEndpoint_WithOwnerToken_403() throws Exception {
        mockMvc.perform(get("/api/user/test").header("Authorization", ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testOwnerEndpoint_WithOwnerToken_200() throws Exception {
        mockMvc.perform(get("/api/owner/test").header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("OWNER"));
    }

    @Test
    void testOwnerEndpoint_WithUserToken_403() throws Exception {
        mockMvc.perform(get("/api/owner/test").header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testAdminEndpoint_WithAdminToken_200() throws Exception {
        mockMvc.perform(get("/api/admin/test").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void testAdminEndpoint_WithUserToken_403() throws Exception {
        mockMvc.perform(get("/api/admin/test").header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403));
    }
}
