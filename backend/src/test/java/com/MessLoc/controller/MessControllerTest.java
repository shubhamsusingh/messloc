package com.MessLoc.controller;

import com.MessLoc.dto.mess.CreateMessRequest;
import com.MessLoc.dto.mess.MessResponse;
import com.MessLoc.dto.mess.UpdateMessRequest;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.FoodType;
import com.MessLoc.enums.MessStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.enums.VerificationStatus;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.security.CustomUserDetailsService;
import com.MessLoc.security.JwtService;
import com.MessLoc.service.MessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class MessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private MessService messService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OwnerProfileRepository ownerProfileRepository;

    @MockitoBean
    private com.MessLoc.repository.MessRepository messRepository;

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
        User user = User.builder().id("u1").email("user@test.com").role(Role.USER).status(AccountStatus.ACTIVE).build();
        User owner = User.builder().id("o1").email("owner@test.com").role(Role.OWNER).status(AccountStatus.ACTIVE).build();
        User admin = User.builder().id("a1").email("admin@test.com").role(Role.ADMIN).status(AccountStatus.ACTIVE).build();

        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(new CustomUserDetails(user));
        when(userDetailsService.loadUserByUsername("owner@test.com")).thenReturn(new CustomUserDetails(owner));
        when(userDetailsService.loadUserByUsername("admin@test.com")).thenReturn(new CustomUserDetails(admin));

        userToken = "Bearer " + jwtService.generateToken(user);
        ownerToken = "Bearer " + jwtService.generateToken(owner);
        adminToken = "Bearer " + jwtService.generateToken(admin);
    }

    private CreateMessRequest createSampleRequest() {
        CreateMessRequest request = new CreateMessRequest();
        request.setName("Sharma Mess");
        request.setDescription("Good food");
        request.setAddress("Street 1");
        request.setCity("Dehradun");
        request.setState("UK");
        request.setPincode("248001");
        request.setLatitude(30.3);
        request.setLongitude(78.0);
        request.setContactNumber("9876543210");
        request.setMonthlyPrice(2500.0);
        request.setFoodType(FoodType.VEG);
        request.setOpeningTime(LocalTime.of(7, 30));
        request.setClosingTime(LocalTime.of(21, 30));
        request.setFacilities(List.of("WiFi"));
        return request;
    }

    @Test
    void ownerCanCreateMess() throws Exception {
        CreateMessRequest request = createSampleRequest();

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

    @Test
    void ownerCanCreateMessViaMessesEndpoint() throws Exception {
        CreateMessRequest request = createSampleRequest();

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
                .andExpect(jsonPath("$.data.name").value("Sharma Mess"));
    }

    @Test
    void userCannotCreateMess() throws Exception {
        CreateMessRequest request = new CreateMessRequest();
        mockMvc.perform(post("/api/owner/messes")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanGetOwnerMesses() throws Exception {
        MessResponse mockResponse = MessResponse.builder().id("m1").name("Sharma Mess").build();
        when(messService.getOwnerMesses("o1")).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/owner/messes")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("m1"));
    }

    @Test
    void ownerCanGetOwnerMessById() throws Exception {
        MessResponse mockResponse = MessResponse.builder().id("m1").name("Sharma Mess").build();
        when(messService.getOwnerMess("m1", "o1")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/owner/messes/m1")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("m1"));
    }

    @Test
    void ownerCanUpdateMess() throws Exception {
        UpdateMessRequest request = new UpdateMessRequest();
        request.setName("Sharma Mess Updated");
        request.setDescription("Great food");
        request.setAddress("Street 2");
        request.setCity("Dehradun");
        request.setState("UK");
        request.setPincode("248001");
        request.setLatitude(30.3);
        request.setLongitude(78.0);
        request.setContactNumber("9876543210");
        request.setMonthlyPrice(2800.0);
        request.setFoodType(FoodType.VEG);
        request.setOpeningTime(LocalTime.of(7, 30));
        request.setClosingTime(LocalTime.of(22, 0));

        MessResponse mockResponse = MessResponse.builder().id("m1").name("Sharma Mess Updated").build();
        when(messService.updateMess(eq("m1"), any(UpdateMessRequest.class), eq("o1"))).thenReturn(mockResponse);

        mockMvc.perform(put("/api/owner/messes/m1")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Sharma Mess Updated"));
    }

    @Test
    void ownerCanDeleteMess() throws Exception {
        mockMvc.perform(delete("/api/owner/messes/m1")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void ownerCanActivateAndDeactivateMess() throws Exception {
        mockMvc.perform(patch("/api/owner/messes/m1/activate")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(patch("/api/owner/messes/m1/deactivate")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void publicCanGetMesses() throws Exception {
        mockMvc.perform(get("/api/messes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void publicCanGetPublicMessById() throws Exception {
        com.MessLoc.dto.mess.PublicMessResponse mockResponse = com.MessLoc.dto.mess.PublicMessResponse.builder()
                .id("m1")
                .name("Sharma Mess")
                .build();
        when(messService.getPublicMess("m1")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/messes/m1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Sharma Mess"));
    }

    @Test
    void adminCanGetAllMessesAndPendingMesses() throws Exception {
        mockMvc.perform(get("/api/admin/messes")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/messes/pending")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void adminCanApproveMess() throws Exception {
        mockMvc.perform(patch("/api/admin/messes/m1/approve")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanRejectMess() throws Exception {
        mockMvc.perform(patch("/api/admin/messes/m1/reject")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void ownerCannotApproveMess() throws Exception {
        mockMvc.perform(patch("/api/admin/messes/m1/approve")
                        .header("Authorization", ownerToken))
                .andExpect(status().isForbidden());
    }
}
