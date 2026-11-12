package com.MessLoc.controller;

import com.MessLoc.dto.mess.MessDetailsResponse;
import com.MessLoc.dto.mess.PublicMessResponse;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.FoodType;
import com.MessLoc.enums.Role;
import com.MessLoc.exception.MessNotFoundException;
import com.MessLoc.repository.MessRepository;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.security.CustomUserDetailsService;
import com.MessLoc.security.JwtService;
import com.MessLoc.service.AuthService;
import com.MessLoc.service.MessService;
import com.MessLoc.service.UserService;
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

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class MessDiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    private String userToken;
    private PublicMessResponse samplePublicMess;
    private MessDetailsResponse sampleMessDetails;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id("u1")
                .name("Test User")
                .email("user@test.com")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(new CustomUserDetails(user));
        userToken = "Bearer " + jwtService.generateToken(user);

        samplePublicMess = PublicMessResponse.builder()
                .id("m-1")
                .name("Annapurna Mess")
                .description("Tasty nutritious meals")
                .address("Rajpur Road")
                .city("Dehradun")
                .state("Uttarakhand")
                .pincode("248001")
                .latitude(30.3255)
                .longitude(78.0436)
                .contactNumber("9876543210")
                .monthlyPrice(3200.0)
                .foodType(FoodType.VEG)
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(22, 0))
                .facilities(List.of("WiFi", "RO Water"))
                .build();

        sampleMessDetails = MessDetailsResponse.builder()
                .id("m-1")
                .name("Annapurna Mess")
                .description("Tasty nutritious meals")
                .address("Rajpur Road")
                .city("Dehradun")
                .state("Uttarakhand")
                .pincode("248001")
                .latitude(30.3255)
                .longitude(78.0436)
                .contactNumber("9876543210")
                .monthlyPrice(3200.0)
                .foodType(FoodType.VEG)
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(22, 0))
                .facilities(List.of("WiFi", "RO Water"))
                .build();
    }

    // ==========================================
    // GET /api/messes (Feature 1, 2, 3, 4)
    // ==========================================

    @Test
    @DisplayName("Public user can browse all messes without authentication")
    void testGetPublicMesses_Unauthenticated_Returns200() throws Exception {
        when(messService.getPublicMesses(null, null)).thenReturn(List.of(samplePublicMess));

        mockMvc.perform(get("/api/messes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("m-1"))
                .andExpect(jsonPath("$.data[0].name").value("Annapurna Mess"))
                .andExpect(jsonPath("$.data[0].ownerId").doesNotExist()) // Verify private ownerId is never exposed
                .andExpect(jsonPath("$.data[0].verificationStatus").doesNotExist());
    }

    @Test
    @DisplayName("Authenticated user can also browse messes")
    void testGetPublicMesses_Authenticated_Returns200() throws Exception {
        when(messService.getPublicMesses(null, null)).thenReturn(List.of(samplePublicMess));

        mockMvc.perform(get("/api/messes")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Annapurna Mess"));
    }

    @Test
    @DisplayName("Search messes by city parameter")
    void testGetPublicMesses_ByCity() throws Exception {
        when(messService.getPublicMesses("Dehradun", null)).thenReturn(List.of(samplePublicMess));

        mockMvc.perform(get("/api/messes?city=Dehradun"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].city").value("Dehradun"));
    }

    @Test
    @DisplayName("Filter messes by case-insensitive foodType: veg")
    void testGetPublicMesses_ByFoodTypeVeg() throws Exception {
        when(messService.getPublicMesses(null, FoodType.VEG)).thenReturn(List.of(samplePublicMess));

        mockMvc.perform(get("/api/messes?foodType=veg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].foodType").value("VEG"));
    }

    @Test
    @DisplayName("Filter messes by foodType: non_veg")
    void testGetPublicMesses_ByFoodTypeNonVeg() throws Exception {
        when(messService.getPublicMesses(null, FoodType.NON_VEG)).thenReturn(List.of(samplePublicMess));

        mockMvc.perform(get("/api/messes?foodType=NON_VEG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Combined search: city and foodType together")
    void testGetPublicMesses_CityAndFoodTypeCombined() throws Exception {
        when(messService.getPublicMesses("Dehradun", FoodType.VEG)).thenReturn(List.of(samplePublicMess));

        mockMvc.perform(get("/api/messes?city=Dehradun&foodType=VEG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Annapurna Mess"));
    }

    @Test
    @DisplayName("Invalid foodType returns 400 Bad Request")
    void testGetPublicMesses_InvalidFoodType_Returns400() throws Exception {
        mockMvc.perform(get("/api/messes?foodType=JUNK_FOOD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid foodType: 'JUNK_FOOD'. Allowed values: VEG, NON_VEG, BOTH"));
    }

    @Test
    @DisplayName("Blank city parameter returns 400 Bad Request")
    void testGetPublicMesses_BlankCity_Returns400() throws Exception {
        when(messService.getPublicMesses("   ", null))
                .thenThrow(new IllegalArgumentException("City search parameter cannot be blank"));

        mockMvc.perform(get("/api/messes?city=   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));
    }

    // ==========================================
    // GET /api/messes/{id} (Feature 5)
    // ==========================================

    @Test
    @DisplayName("Get mess details by ID returns 200 with MessDetailsResponse")
    void testGetPublicMess_ValidId_Returns200() throws Exception {
        when(messService.getPublicMess("m-1")).thenReturn(sampleMessDetails);

        mockMvc.perform(get("/api/messes/m-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("m-1"))
                .andExpect(jsonPath("$.data.name").value("Annapurna Mess"))
                .andExpect(jsonPath("$.data.monthlyPrice").value(3200.0))
                .andExpect(jsonPath("$.data.ownerId").doesNotExist());
    }

    @Test
    @DisplayName("Non-existent mess ID returns 404 Not Found")
    void testGetPublicMess_NotFound_Returns404() throws Exception {
        when(messService.getPublicMess("non-existent-id"))
                .thenThrow(new MessNotFoundException("Mess not found with id: non-existent-id"));

        mockMvc.perform(get("/api/messes/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Unapproved mess returns 404 without leaking internal state")
    void testGetPublicMess_Unapproved_Returns404() throws Exception {
        when(messService.getPublicMess("pending-mess-id"))
                .thenThrow(new MessNotFoundException("Mess is not publicly available or pending verification"));

        mockMvc.perform(get("/api/messes/pending-mess-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404));
    }

    // ==========================================
    // GET /api/messes/nearby (Feature 6)
    // ==========================================

    @Test
    @DisplayName("Nearby discovery with valid coordinates returns 200 and distanceInKm")
    void testGetNearbyMesses_ValidCoordinates_Returns200() throws Exception {
        PublicMessResponse nearbyMess = PublicMessResponse.builder()
                .id("m-nearby")
                .name("Nearby Bhojanalaya")
                .city("Dehradun")
                .distanceInKm(1.25)
                .build();

        when(messService.getNearbyMesses(30.3165, 78.0322, 5.0, null))
                .thenReturn(List.of(nearbyMess));

        mockMvc.perform(get("/api/messes/nearby?latitude=30.3165&longitude=78.0322&radius=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("m-nearby"))
                .andExpect(jsonPath("$.data[0].distanceInKm").value(1.25));
    }

    @Test
    @DisplayName("Nearby discovery with foodType filter")
    void testGetNearbyMesses_WithFoodTypeFilter() throws Exception {
        when(messService.getNearbyMesses(30.3165, 78.0322, 5.0, FoodType.VEG))
                .thenReturn(List.of(samplePublicMess));

        mockMvc.perform(get("/api/messes/nearby?latitude=30.3165&longitude=78.0322&radius=5&foodType=VEG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Nearby discovery with invalid latitude returns 400 Bad Request")
    void testGetNearbyMesses_InvalidLatitude_Returns400() throws Exception {
        when(messService.getNearbyMesses(95.0, 78.0322, 5.0, null))
                .thenThrow(new IllegalArgumentException("Latitude must be between -90 and 90"));

        mockMvc.perform(get("/api/messes/nearby?latitude=95.0&longitude=78.0322"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Latitude must be between -90 and 90"));
    }

    @Test
    @DisplayName("Nearby discovery with invalid longitude returns 400 Bad Request")
    void testGetNearbyMesses_InvalidLongitude_Returns400() throws Exception {
        when(messService.getNearbyMesses(30.3165, 190.0, 5.0, null))
                .thenThrow(new IllegalArgumentException("Longitude must be between -180 and 180"));

        mockMvc.perform(get("/api/messes/nearby?latitude=30.3165&longitude=190.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Nearby discovery with missing latitude returns 400 Bad Request")
    void testGetNearbyMesses_MissingLatitude_Returns400() throws Exception {
        mockMvc.perform(get("/api/messes/nearby?longitude=78.0322"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Required parameter 'latitude' is missing"));
    }

    @Test
    @DisplayName("Nearby discovery with invalid coordinate string returns 400 Bad Request")
    void testGetNearbyMesses_TypeMismatch_Returns400() throws Exception {
        mockMvc.perform(get("/api/messes/nearby?latitude=not-a-number&longitude=78.0322"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));
    }

    // ==========================================
    // SECURITY BOUNDARIES
    // ==========================================

    @Test
    @DisplayName("POST /api/messes remains strictly protected (401 without auth)")
    void testPostMess_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/messes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/messes forbidden for regular USER role (403)")
    void testPostMess_UserRole_Returns403() throws Exception {
        mockMvc.perform(post("/api/messes")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}

