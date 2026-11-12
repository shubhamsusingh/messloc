package com.MessLoc.controller;

import com.MessLoc.dto.menu.CreateMenuRequest;
import com.MessLoc.dto.menu.DailyMenuResponse;
import com.MessLoc.dto.menu.MenuItemRequest;
import com.MessLoc.dto.menu.MenuItemResponse;
import com.MessLoc.dto.menu.MenuResponse;
import com.MessLoc.dto.menu.UpdateMenuRequest;
import com.MessLoc.dto.menu.WeeklyMenuResponse;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.MealType;
import com.MessLoc.enums.Role;
import com.MessLoc.exception.MenuAlreadyExistsException;
import com.MessLoc.exception.MenuItemNotFoundException;
import com.MessLoc.exception.MenuNotFoundException;
import com.MessLoc.exception.MessNotFoundException;
import com.MessLoc.repository.MenuRepository;
import com.MessLoc.repository.MessRepository;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.security.CustomUserDetailsService;
import com.MessLoc.security.JwtService;
import com.MessLoc.service.AuthService;
import com.MessLoc.service.MenuService;
import com.MessLoc.service.MessService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private JwtService jwtService;

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

    private String ownerToken;
    private String pendingOwnerToken;
    private String userToken;

    private final LocalDate testDate = LocalDate.of(2026, 11, 10);

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

        when(userDetailsService.loadUserByUsername("owner@test.com")).thenReturn(new CustomUserDetails(activeOwner));
        when(userDetailsService.loadUserByUsername("pending-owner@test.com")).thenReturn(new CustomUserDetails(pendingOwner));
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(new CustomUserDetails(regularUser));

        ownerToken = "Bearer " + jwtService.generateToken(activeOwner);
        pendingOwnerToken = "Bearer " + jwtService.generateToken(pendingOwner);
        userToken = "Bearer " + jwtService.generateToken(regularUser);
    }

    // ==========================================
    // OWNER APIS: CREATE, UPDATE, DELETE
    // ==========================================

    @Test
    @DisplayName("Owner creates daily menu successfully (201 Created)")
    void testCreateMenu_Success() throws Exception {
        CreateMenuRequest request = CreateMenuRequest.builder()
                .messId("m1")
                .menuDate(testDate)
                .items(List.of(
                        MenuItemRequest.builder().mealType(MealType.BREAKFAST).foodName("Poha").build()
                ))
                .build();

        MenuResponse mockResp = MenuResponse.builder()
                .id("menu-101")
                .messId("m1")
                .menuDate(testDate)
                .items(List.of(
                        MenuItemResponse.builder().id("i1").mealType(MealType.BREAKFAST).foodName("Poha").build()
                ))
                .build();

        when(menuService.createMenu(any(CreateMenuRequest.class), eq("o1"))).thenReturn(mockResp);

        mockMvc.perform(post("/api/menus")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("menu-101"))
                .andExpect(jsonPath("$.data.items[0].foodName").value("Poha"));
    }

    @Test
    @DisplayName("Create daily menu duplicate date returns 409 Conflict")
    void testCreateMenu_DuplicateDate_Returns409() throws Exception {
        CreateMenuRequest request = CreateMenuRequest.builder()
                .messId("m1")
                .menuDate(testDate)
                .build();

        when(menuService.createMenu(any(CreateMenuRequest.class), eq("o1")))
                .thenThrow(new MenuAlreadyExistsException("A daily menu already exists for this mess on date 2026-11-10"));

        mockMvc.perform(post("/api/menus")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/menus unauthenticated returns 401 Unauthorized")
    void testCreateMenu_Unauthenticated_Returns401() throws Exception {
        CreateMenuRequest request = CreateMenuRequest.builder()
                .messId("m1")
                .menuDate(testDate)
                .build();

        mockMvc.perform(post("/api/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/menus with USER token returns 403 Forbidden")
    void testCreateMenu_UserRole_Returns403() throws Exception {
        CreateMenuRequest request = CreateMenuRequest.builder()
                .messId("m1")
                .menuDate(testDate)
                .build();

        mockMvc.perform(post("/api/menus")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/menus with pending owner is denied (401 or 403)")
    void testCreateMenu_PendingOwner_Denied() throws Exception {
        CreateMenuRequest request = CreateMenuRequest.builder()
                .messId("m1")
                .menuDate(testDate)
                .build();

        mockMvc.perform(post("/api/menus")
                        .header("Authorization", pendingOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Owner updates daily menu (200 OK)")
    void testUpdateMenu_Success() throws Exception {
        UpdateMenuRequest request = UpdateMenuRequest.builder()
                .items(List.of(
                        MenuItemRequest.builder().mealType(MealType.LUNCH).foodName("Thali").build()
                ))
                .build();

        MenuResponse mockResp = MenuResponse.builder()
                .id("menu-101")
                .messId("m1")
                .menuDate(testDate)
                .items(List.of(
                        MenuItemResponse.builder().id("i2").mealType(MealType.LUNCH).foodName("Thali").build()
                ))
                .build();

        when(menuService.updateMenu(eq("menu-101"), any(UpdateMenuRequest.class), eq("o1"))).thenReturn(mockResp);

        mockMvc.perform(put("/api/menus/menu-101")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].foodName").value("Thali"));
    }

    @Test
    @DisplayName("Owner adds food item to daily menu (201 Created)")
    void testAddMenuItem_Success() throws Exception {
        MenuItemRequest request = MenuItemRequest.builder()
                .mealType(MealType.DINNER)
                .foodName("Paneer Roti")
                .description("Fresh paneer with 4 rotis")
                .build();

        MenuResponse mockResp = MenuResponse.builder()
                .id("menu-101")
                .items(List.of(
                        MenuItemResponse.builder().id("item-new").mealType(MealType.DINNER).foodName("Paneer Roti").build()
                ))
                .build();

        when(menuService.addMenuItem(eq("menu-101"), any(MenuItemRequest.class), eq("o1"))).thenReturn(mockResp);

        mockMvc.perform(post("/api/menus/menu-101/items")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].foodName").value("Paneer Roti"));
    }

    @Test
    @DisplayName("Owner updates specific food item (200 OK)")
    void testUpdateMenuItem_Success() throws Exception {
        MenuItemRequest request = MenuItemRequest.builder()
                .mealType(MealType.BREAKFAST)
                .foodName("Masala Poha")
                .build();

        MenuResponse mockResp = MenuResponse.builder()
                .id("menu-101")
                .items(List.of(
                        MenuItemResponse.builder().id("item-1").mealType(MealType.BREAKFAST).foodName("Masala Poha").build()
                ))
                .build();

        when(menuService.updateMenuItem(eq("menu-101"), eq("item-1"), any(MenuItemRequest.class), eq("o1")))
                .thenReturn(mockResp);

        mockMvc.perform(put("/api/menus/menu-101/items/item-1")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].foodName").value("Masala Poha"));
    }

    @Test
    @DisplayName("Owner deletes food item (200 OK)")
    void testDeleteMenuItem_Success() throws Exception {
        MenuResponse mockResp = MenuResponse.builder().id("menu-101").items(List.of()).build();
        when(menuService.deleteMenuItem("menu-101", "item-1", "o1")).thenReturn(mockResp);

        mockMvc.perform(delete("/api/menus/menu-101/items/item-1")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Owner deletes daily menu completely (200 OK)")
    void testDeleteMenu_Success() throws Exception {
        mockMvc.perform(delete("/api/menus/menu-101")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Owner views weekly menu (200 OK)")
    void testGetOwnerWeeklyMenu_Success() throws Exception {
        WeeklyMenuResponse mockResp = WeeklyMenuResponse.builder()
                .messId("m1")
                .messName("Sharma Mess")
                .startDate(testDate)
                .endDate(testDate.plusDays(6))
                .menus(List.of())
                .build();

        when(menuService.getOwnerWeeklyMenu(eq("m1"), any(), eq("o1"))).thenReturn(mockResp);

        mockMvc.perform(get("/api/menus/owner/mess/m1/weekly")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messName").value("Sharma Mess"));
    }

    // ==========================================
    // PUBLIC APIS: TODAY & WEEKLY MENU
    // ==========================================

    @Test
    @DisplayName("Public user can view today's menu without authentication (200 OK)")
    void testGetPublicTodayMenu_Unauthenticated_Returns200() throws Exception {
        DailyMenuResponse mockResp = DailyMenuResponse.builder()
                .messId("m1")
                .menuDate(LocalDate.now())
                .isUploaded(true)
                .breakfast(List.of(
                        MenuItemResponse.builder().id("i1").mealType(MealType.BREAKFAST).foodName("Aloo Paratha").build()
                ))
                .build();

        when(menuService.getPublicTodayMenu("m1")).thenReturn(mockResp);

        mockMvc.perform(get("/api/menus/mess/m1/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.breakfast[0].foodName").value("Aloo Paratha"));
    }

    @Test
    @DisplayName("Public user can view weekly menu without authentication (200 OK)")
    void testGetPublicWeeklyMenu_Unauthenticated_Returns200() throws Exception {
        WeeklyMenuResponse mockResp = WeeklyMenuResponse.builder()
                .messId("m1")
                .messName("Sharma Mess")
                .startDate(testDate)
                .endDate(testDate.plusDays(6))
                .menus(List.of())
                .build();

        when(menuService.getPublicWeeklyMenu(eq("m1"), any())).thenReturn(mockResp);

        mockMvc.perform(get("/api/menus/mess/m1/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messName").value("Sharma Mess"));
    }

    @Test
    @DisplayName("Public menu request for pending mess returns 404 Not Found")
    void testGetPublicMenu_PendingMess_Returns404() throws Exception {
        when(menuService.getPublicTodayMenu("m-pending"))
                .thenThrow(new MessNotFoundException("Mess is not publicly available or pending verification"));

        mockMvc.perform(get("/api/menus/mess/m-pending/today"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404));
    }

    // ==========================================
    // VALIDATION TESTS
    // ==========================================

    @Test
    @DisplayName("Create menu with missing messId returns 400 Bad Request")
    void testCreateMenu_MissingMessId_Returns400() throws Exception {
        CreateMenuRequest request = CreateMenuRequest.builder()
                .menuDate(testDate)
                .build();

        mockMvc.perform(post("/api/menus")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Create menu with missing menuDate returns 400 Bad Request")
    void testCreateMenu_MissingMenuDate_Returns400() throws Exception {
        CreateMenuRequest request = CreateMenuRequest.builder()
                .messId("m1")
                .build();

        mockMvc.perform(post("/api/menus")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Add food item with blank foodName returns 400 Bad Request")
    void testAddMenuItem_BlankFoodName_Returns400() throws Exception {
        MenuItemRequest request = MenuItemRequest.builder()
                .mealType(MealType.BREAKFAST)
                .foodName("   ")
                .build();

        mockMvc.perform(post("/api/menus/menu-101/items")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Add food item with invalid mealType returns 400 Bad Request")
    void testAddMenuItem_InvalidMealType_Returns400() throws Exception {
        String payload = "{\"mealType\":\"MIDNIGHT_SNACK\",\"foodName\":\"Noodles\"}";

        mockMvc.perform(post("/api/menus/menu-101/items")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));
    }
}

