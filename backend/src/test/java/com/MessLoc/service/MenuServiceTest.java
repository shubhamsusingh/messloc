package com.MessLoc.service;

import com.MessLoc.dto.menu.CreateMenuRequest;
import com.MessLoc.dto.menu.DailyMenuResponse;
import com.MessLoc.dto.menu.MenuItemRequest;
import com.MessLoc.dto.menu.MenuItemResponse;
import com.MessLoc.dto.menu.MenuResponse;
import com.MessLoc.dto.menu.UpdateMenuRequest;
import com.MessLoc.dto.menu.WeeklyMenuResponse;
import com.MessLoc.entity.Menu;
import com.MessLoc.entity.MenuItem;
import com.MessLoc.entity.Mess;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.MealType;
import com.MessLoc.enums.MessStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.enums.VerificationStatus;
import com.MessLoc.exception.AccountInactiveException;
import com.MessLoc.exception.MenuAlreadyExistsException;
import com.MessLoc.exception.MenuItemNotFoundException;
import com.MessLoc.exception.MenuNotFoundException;
import com.MessLoc.exception.MessNotFoundException;
import com.MessLoc.exception.OwnerPendingException;
import com.MessLoc.exception.UnauthorizedMessAccessException;
import com.MessLoc.repository.MenuRepository;
import com.MessLoc.repository.MessRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.service.impl.MenuServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MessRepository messRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MenuServiceImpl menuService;

    private User activeOwner;
    private User otherOwner;
    private User pendingOwner;
    private User inactiveOwner;
    private User regularUser;
    private Mess approvedActiveMess;
    private Mess pendingMess;
    private Mess inactiveMess;
    private Mess rejectedMess;
    private Menu sampleMenu;

    private final LocalDate testDate = LocalDate.of(2026, 11, 10);

    @BeforeEach
    void setUp() {
        activeOwner = User.builder()
                .id("o-1")
                .name("Sharma Owner")
                .email("owner1@messloc.com")
                .role(Role.OWNER)
                .status(AccountStatus.ACTIVE)
                .build();

        otherOwner = User.builder()
                .id("o-2")
                .name("Other Owner")
                .email("owner2@messloc.com")
                .role(Role.OWNER)
                .status(AccountStatus.ACTIVE)
                .build();

        pendingOwner = User.builder()
                .id("o-pending")
                .name("Pending Owner")
                .email("pending@messloc.com")
                .role(Role.OWNER)
                .status(AccountStatus.PENDING)
                .build();

        inactiveOwner = User.builder()
                .id("o-inactive")
                .name("Inactive Owner")
                .email("inactive@messloc.com")
                .role(Role.OWNER)
                .status(AccountStatus.INACTIVE)
                .build();

        regularUser = User.builder()
                .id("u-1")
                .name("Regular User")
                .email("user@messloc.com")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        approvedActiveMess = Mess.builder()
                .id("m-1")
                .ownerId("o-1")
                .name("Annapurna Mess")
                .verificationStatus(VerificationStatus.APPROVED)
                .status(MessStatus.ACTIVE)
                .build();

        pendingMess = Mess.builder()
                .id("m-pending")
                .ownerId("o-1")
                .name("Pending Mess")
                .verificationStatus(VerificationStatus.PENDING)
                .status(MessStatus.ACTIVE)
                .build();

        inactiveMess = Mess.builder()
                .id("m-inactive")
                .ownerId("o-1")
                .name("Inactive Mess")
                .verificationStatus(VerificationStatus.APPROVED)
                .status(MessStatus.INACTIVE)
                .build();

        rejectedMess = Mess.builder()
                .id("m-rejected")
                .ownerId("o-1")
                .name("Rejected Mess")
                .verificationStatus(VerificationStatus.REJECTED)
                .status(MessStatus.ACTIVE)
                .build();

        MenuItem item1 = MenuItem.builder()
                .id("item-1")
                .mealType(MealType.BREAKFAST)
                .foodName("Poha")
                .description("Fresh vegetable poha")
                .build();

        MenuItem item2 = MenuItem.builder()
                .id("item-2")
                .mealType(MealType.LUNCH)
                .foodName("Dal Rice")
                .description("Yellow dal with steamed rice")
                .build();

        sampleMenu = Menu.builder()
                .id("menu-1")
                .messId("m-1")
                .menuDate(testDate)
                .items(new ArrayList<>(List.of(item1, item2)))
                .build();
    }

    // ==========================================
    // CREATE MENU TESTS
    // ==========================================

    @Test
    @DisplayName("Owner creates daily menu successfully")
    void testCreateMenu_Success() {
        when(userRepository.findById("o-1")).thenReturn(Optional.of(activeOwner));
        when(messRepository.findById("m-1")).thenReturn(Optional.of(approvedActiveMess));
        when(menuRepository.findByMessIdAndMenuDate("m-1", testDate)).thenReturn(Optional.empty());
        when(menuRepository.save(any(Menu.class))).thenAnswer(i -> {
            Menu m = i.getArgument(0);
            m.setId("menu-generated-id");
            return m;
        });

        CreateMenuRequest request = CreateMenuRequest.builder()
                .messId("m-1")
                .menuDate(testDate)
                .items(List.of(
                        MenuItemRequest.builder().mealType(MealType.BREAKFAST).foodName("Idli").build(),
                        MenuItemRequest.builder().mealType(MealType.LUNCH).foodName("Thali").build()
                ))
                .build();

        MenuResponse response = menuService.createMenu(request, "o-1");

        assertNotNull(response);
        assertEquals("menu-generated-id", response.getId());
        assertEquals("m-1", response.getMessId());
        assertEquals(testDate, response.getMenuDate());
        assertEquals(2, response.getItems().size());
        assertNotNull(response.getItems().get(0).getId());
    }

    @Test
    @DisplayName("Creating daily menu for duplicate date throws MenuAlreadyExistsException (409)")
    void testCreateMenu_DuplicateDate_Throws409() {
        when(userRepository.findById("o-1")).thenReturn(Optional.of(activeOwner));
        when(messRepository.findById("m-1")).thenReturn(Optional.of(approvedActiveMess));
        when(menuRepository.findByMessIdAndMenuDate("m-1", testDate)).thenReturn(Optional.of(sampleMenu));

        CreateMenuRequest request = CreateMenuRequest.builder()
                .messId("m-1")
                .menuDate(testDate)
                .build();

        assertThrows(MenuAlreadyExistsException.class, () -> menuService.createMenu(request, "o-1"));
        verify(menuRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creating menu with duplicate food names in same meal category throws IllegalArgumentException (400)")
    void testCreateMenu_DuplicateFoodInSameMealType_Throws400() {
        when(userRepository.findById("o-1")).thenReturn(Optional.of(activeOwner));
        when(messRepository.findById("m-1")).thenReturn(Optional.of(approvedActiveMess));
        when(menuRepository.findByMessIdAndMenuDate("m-1", testDate)).thenReturn(Optional.empty());

        CreateMenuRequest request = CreateMenuRequest.builder()
                .messId("m-1")
                .menuDate(testDate)
                .items(List.of(
                        MenuItemRequest.builder().mealType(MealType.BREAKFAST).foodName("Poha").build(),
                        MenuItemRequest.builder().mealType(MealType.BREAKFAST).foodName("poha").build()
                ))
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                menuService.createMenu(request, "o-1"));

        assertTrue(ex.getMessage().contains("Duplicate food item 'poha'"));
    }

    @Test
    @DisplayName("Owner cannot create menu for another owner's mess (403 Forbidden)")
    void testCreateMenu_NotOwnerOfMess_Throws403() {
        when(userRepository.findById("o-2")).thenReturn(Optional.of(otherOwner));
        when(messRepository.findById("m-1")).thenReturn(Optional.of(approvedActiveMess)); // owned by o-1

        CreateMenuRequest request = CreateMenuRequest.builder()
                .messId("m-1")
                .menuDate(testDate)
                .build();

        assertThrows(UnauthorizedMessAccessException.class, () -> menuService.createMenu(request, "o-2"));
    }

    @Test
    @DisplayName("Pending owner cannot create menu (403 Forbidden)")
    void testCreateMenu_PendingOwner_Throws403() {
        when(userRepository.findById("o-pending")).thenReturn(Optional.of(pendingOwner));

        CreateMenuRequest request = CreateMenuRequest.builder()
                .messId("m-1")
                .menuDate(testDate)
                .build();

        assertThrows(OwnerPendingException.class, () -> menuService.createMenu(request, "o-pending"));
    }

    @Test
    @DisplayName("Non-owner USER role cannot create menu (403 Forbidden)")
    void testCreateMenu_UserRole_ThrowsAccessDenied() {
        when(userRepository.findById("u-1")).thenReturn(Optional.of(regularUser));

        CreateMenuRequest request = CreateMenuRequest.builder()
                .messId("m-1")
                .menuDate(testDate)
                .build();

        assertThrows(AccessDeniedException.class, () -> menuService.createMenu(request, "u-1"));
    }

    // ==========================================
    // ITEM MANAGEMENT TESTS
    // ==========================================

    @Test
    @DisplayName("Add food item to existing daily menu")
    void testAddMenuItem_Success() {
        when(userRepository.findById("o-1")).thenReturn(Optional.of(activeOwner));
        when(menuRepository.findById("menu-1")).thenReturn(Optional.of(sampleMenu));
        when(messRepository.findById("m-1")).thenReturn(Optional.of(approvedActiveMess));
        when(menuRepository.save(any(Menu.class))).thenAnswer(i -> i.getArgument(0));

        MenuItemRequest request = MenuItemRequest.builder()
                .mealType(MealType.DINNER)
                .foodName("Paneer Butter Masala")
                .description("Rich creamy gravy")
                .build();

        MenuResponse response = menuService.addMenuItem("menu-1", request, "o-1");

        assertEquals(3, response.getItems().size());
        assertTrue(response.getItems().stream().anyMatch(i -> i.getFoodName().equals("Paneer Butter Masala")));
    }

    @Test
    @DisplayName("Adding duplicate food item in same meal category throws IllegalArgumentException (400)")
    void testAddMenuItem_DuplicateItemInSameMealType_Throws400() {
        when(userRepository.findById("o-1")).thenReturn(Optional.of(activeOwner));
        when(menuRepository.findById("menu-1")).thenReturn(Optional.of(sampleMenu)); // already has BREAKFAST: Poha
        when(messRepository.findById("m-1")).thenReturn(Optional.of(approvedActiveMess));

        MenuItemRequest request = MenuItemRequest.builder()
                .mealType(MealType.BREAKFAST)
                .foodName("poha") // duplicate case-insensitive
                .build();

        assertThrows(IllegalArgumentException.class, () -> menuService.addMenuItem("menu-1", request, "o-1"));
    }

    @Test
    @DisplayName("Update specific food item in menu")
    void testUpdateMenuItem_Success() {
        when(userRepository.findById("o-1")).thenReturn(Optional.of(activeOwner));
        when(menuRepository.findById("menu-1")).thenReturn(Optional.of(sampleMenu));
        when(messRepository.findById("m-1")).thenReturn(Optional.of(approvedActiveMess));
        when(menuRepository.save(any(Menu.class))).thenAnswer(i -> i.getArgument(0));

        MenuItemRequest updateRequest = MenuItemRequest.builder()
                .mealType(MealType.BREAKFAST)
                .foodName("Kanda Poha")
                .description("Special onion poha")
                .build();

        MenuResponse response = menuService.updateMenuItem("menu-1", "item-1", updateRequest, "o-1");

        MenuItemResponse updated = response.getItems().stream()
                .filter(i -> i.getId().equals("item-1"))
                .findFirst()
                .orElse(null);

        assertNotNull(updated);
        assertEquals("Kanda Poha", updated.getFoodName());
    }

    @Test
    @DisplayName("Updating non-existent food item throws MenuItemNotFoundException (404)")
    void testUpdateMenuItem_NotFound_Throws404() {
        when(userRepository.findById("o-1")).thenReturn(Optional.of(activeOwner));
        when(menuRepository.findById("menu-1")).thenReturn(Optional.of(sampleMenu));
        when(messRepository.findById("m-1")).thenReturn(Optional.of(approvedActiveMess));

        MenuItemRequest updateRequest = MenuItemRequest.builder()
                .mealType(MealType.BREAKFAST)
                .foodName("Upma")
                .build();

        assertThrows(MenuItemNotFoundException.class, () ->
                menuService.updateMenuItem("menu-1", "non-existent-item", updateRequest, "o-1"));
    }

    @Test
    @DisplayName("Delete food item from menu")
    void testDeleteMenuItem_Success() {
        when(userRepository.findById("o-1")).thenReturn(Optional.of(activeOwner));
        when(menuRepository.findById("menu-1")).thenReturn(Optional.of(sampleMenu));
        when(messRepository.findById("m-1")).thenReturn(Optional.of(approvedActiveMess));
        when(menuRepository.save(any(Menu.class))).thenAnswer(i -> i.getArgument(0));

        MenuResponse response = menuService.deleteMenuItem("menu-1", "item-1", "o-1");

        assertEquals(1, response.getItems().size());
        assertEquals("item-2", response.getItems().get(0).getId());
    }

    @Test
    @DisplayName("Delete daily menu completely")
    void testDeleteMenu_Success() {
        when(userRepository.findById("o-1")).thenReturn(Optional.of(activeOwner));
        when(menuRepository.findById("menu-1")).thenReturn(Optional.of(sampleMenu));
        when(messRepository.findById("m-1")).thenReturn(Optional.of(approvedActiveMess));

        menuService.deleteMenu("menu-1", "o-1");

        verify(menuRepository).delete(sampleMenu);
    }

    // ==========================================
    // WEEKLY MENU TESTS
    // ==========================================

    @Test
    @DisplayName("Owner views weekly menu: returns 7 consecutive dates with meal grouping")
    void testGetOwnerWeeklyMenu_Returns7DaysGrouped() {
        when(userRepository.findById("o-1")).thenReturn(Optional.of(activeOwner));
        when(messRepository.findById("m-1")).thenReturn(Optional.of(approvedActiveMess));

        LocalDate start = LocalDate.of(2026, 11, 10);
        when(menuRepository.findByMessIdAndMenuDateBetweenOrderByMenuDateAsc("m-1", start, start.plusDays(6)))
                .thenReturn(List.of(sampleMenu));

        WeeklyMenuResponse response = menuService.getOwnerWeeklyMenu("m-1", start, "o-1");

        assertNotNull(response);
        assertEquals("m-1", response.getMessId());
        assertEquals("Annapurna Mess", response.getMessName());
        assertEquals(7, response.getMenus().size());

        // First day has uploaded menu
        DailyMenuResponse day1 = response.getMenus().get(0);
        assertEquals(start, day1.getMenuDate());
        assertTrue(day1.isUploaded());
        assertEquals(1, day1.getBreakfast().size());
        assertEquals(1, day1.getLunch().size());
        assertEquals(0, day1.getDinner().size());

        // Second day has no menu
        DailyMenuResponse day2 = response.getMenus().get(1);
        assertEquals(start.plusDays(1), day2.getMenuDate());
        assertFalse(day2.isUploaded());
        assertTrue(day2.getBreakfast().isEmpty());
    }

    // ==========================================
    // PUBLIC VIEWING TESTS
    // ==========================================

    @Test
    @DisplayName("Public user views today's menu for approved and active mess")
    void testGetPublicTodayMenu_ApprovedActiveMess_Success() {
        when(messRepository.findById("m-1")).thenReturn(Optional.of(approvedActiveMess));
        LocalDate today = LocalDate.now();
        Menu todayMenu = Menu.builder()
                .id("menu-today")
                .messId("m-1")
                .menuDate(today)
                .items(List.of(
                        MenuItem.builder().id("i1").mealType(MealType.BREAKFAST).foodName("Paratha").build(),
                        MenuItem.builder().id("i2").mealType(MealType.LUNCH).foodName("Rajma Chawal").build()
                ))
                .build();

        when(menuRepository.findByMessIdAndMenuDate("m-1", today)).thenReturn(Optional.of(todayMenu));

        DailyMenuResponse response = menuService.getPublicTodayMenu("m-1");

        assertNotNull(response);
        assertTrue(response.isUploaded());
        assertEquals(1, response.getBreakfast().size());
        assertEquals(1, response.getLunch().size());
    }

    @Test
    @DisplayName("Public user gets clear response when today's menu is not uploaded")
    void testGetPublicTodayMenu_NotUploaded_ReturnsEmptyGrouped() {
        when(messRepository.findById("m-1")).thenReturn(Optional.of(approvedActiveMess));
        LocalDate today = LocalDate.now();
        when(menuRepository.findByMessIdAndMenuDate("m-1", today)).thenReturn(Optional.empty());

        DailyMenuResponse response = menuService.getPublicTodayMenu("m-1");

        assertNotNull(response);
        assertFalse(response.isUploaded());
        assertTrue(response.getBreakfast().isEmpty());
        assertTrue(response.getLunch().isEmpty());
        assertTrue(response.getDinner().isEmpty());
    }

    @Test
    @DisplayName("Public user cannot view menu for pending mess (404 Not Found)")
    void testGetPublicMenu_PendingMess_Throws404() {
        when(messRepository.findById("m-pending")).thenReturn(Optional.of(pendingMess));

        assertThrows(MessNotFoundException.class, () -> menuService.getPublicTodayMenu("m-pending"));
        assertThrows(MessNotFoundException.class, () -> menuService.getPublicWeeklyMenu("m-pending", testDate));
    }

    @Test
    @DisplayName("Public user cannot view menu for inactive mess (404 Not Found)")
    void testGetPublicMenu_InactiveMess_Throws404() {
        when(messRepository.findById("m-inactive")).thenReturn(Optional.of(inactiveMess));

        assertThrows(MessNotFoundException.class, () -> menuService.getPublicTodayMenu("m-inactive"));
        assertThrows(MessNotFoundException.class, () -> menuService.getPublicWeeklyMenu("m-inactive", testDate));
    }

    @Test
    @DisplayName("Public user cannot view menu for rejected mess (404 Not Found)")
    void testGetPublicMenu_RejectedMess_Throws404() {
        when(messRepository.findById("m-rejected")).thenReturn(Optional.of(rejectedMess));

        assertThrows(MessNotFoundException.class, () -> menuService.getPublicTodayMenu("m-rejected"));
    }
}
