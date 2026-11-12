package com.MessLoc.service.impl;

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
import com.MessLoc.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final MessRepository messRepository;
    private final UserRepository userRepository;

    // ==========================================
    // OWNER OPERATIONS
    // ==========================================

    @Override
    public MenuResponse createMenu(CreateMenuRequest request, String ownerId) {
        Mess mess = getMessOwnedByOwner(request.getMessId(), ownerId);

        // Pre-check for existing menu on this date
        menuRepository.findByMessIdAndMenuDate(mess.getId(), request.getMenuDate())
                .ifPresent(existing -> {
                    throw new MenuAlreadyExistsException("A daily menu already exists for mess '" + mess.getName() + "' on date " + request.getMenuDate());
                });

        List<MenuItem> items = new ArrayList<>();
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            validateNoDuplicateFoodItems(request.getItems());
            for (MenuItemRequest itemReq : request.getItems()) {
                items.add(MenuItem.builder()
                        .id(UUID.randomUUID().toString())
                        .mealType(itemReq.getMealType())
                        .foodName(itemReq.getFoodName().trim())
                        .description(itemReq.getDescription() != null ? itemReq.getDescription().trim() : null)
                        .build());
            }
        }

        Instant now = Instant.now();
        Menu menu = Menu.builder()
                .messId(mess.getId())
                .menuDate(request.getMenuDate())
                .items(items)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            Menu savedMenu = menuRepository.save(menu);
            return mapToMenuResponse(savedMenu);
        } catch (DuplicateKeyException ex) {
            throw new MenuAlreadyExistsException("A daily menu already exists for this mess on date " + request.getMenuDate());
        }
    }

    @Override
    public MenuResponse updateMenu(String menuId, UpdateMenuRequest request, String ownerId) {
        Menu menu = getMenuOwnedByOwner(menuId, ownerId);

        if (request.getMenuDate() != null && !request.getMenuDate().equals(menu.getMenuDate())) {
            menuRepository.findByMessIdAndMenuDate(menu.getMessId(), request.getMenuDate())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(menu.getId())) {
                            throw new MenuAlreadyExistsException("A daily menu already exists for this mess on date " + request.getMenuDate());
                        }
                    });
            menu.setMenuDate(request.getMenuDate());
        }

        if (request.getItems() != null) {
            validateNoDuplicateFoodItems(request.getItems());
            List<MenuItem> updatedItems = new ArrayList<>();
            for (MenuItemRequest itemReq : request.getItems()) {
                updatedItems.add(MenuItem.builder()
                        .id(UUID.randomUUID().toString())
                        .mealType(itemReq.getMealType())
                        .foodName(itemReq.getFoodName().trim())
                        .description(itemReq.getDescription() != null ? itemReq.getDescription().trim() : null)
                        .build());
            }
            menu.setItems(updatedItems);
        }

        menu.setUpdatedAt(Instant.now());
        try {
            Menu updatedMenu = menuRepository.save(menu);
            return mapToMenuResponse(updatedMenu);
        } catch (DuplicateKeyException ex) {
            throw new MenuAlreadyExistsException("A daily menu already exists for this mess on date " + menu.getMenuDate());
        }
    }

    @Override
    public MenuResponse addMenuItem(String menuId, MenuItemRequest request, String ownerId) {
        Menu menu = getMenuOwnedByOwner(menuId, ownerId);

        // Check duplicate within the same meal category
        String foodNameTrimmed = request.getFoodName().trim();
        boolean alreadyExists = menu.getItems().stream().anyMatch(i ->
                i.getMealType() == request.getMealType() &&
                i.getFoodName().equalsIgnoreCase(foodNameTrimmed));

        if (alreadyExists) {
            throw new IllegalArgumentException("Food item '" + foodNameTrimmed + "' already exists under " + request.getMealType());
        }

        MenuItem newItem = MenuItem.builder()
                .id(UUID.randomUUID().toString())
                .mealType(request.getMealType())
                .foodName(foodNameTrimmed)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .build();

        menu.getItems().add(newItem);
        menu.setUpdatedAt(Instant.now());

        Menu updatedMenu = menuRepository.save(menu);
        return mapToMenuResponse(updatedMenu);
    }

    @Override
    public MenuResponse updateMenuItem(String menuId, String itemId, MenuItemRequest request, String ownerId) {
        Menu menu = getMenuOwnedByOwner(menuId, ownerId);

        MenuItem targetItem = menu.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new MenuItemNotFoundException("Menu item not found with id: " + itemId));

        String foodNameTrimmed = request.getFoodName().trim();
        boolean duplicateConflict = menu.getItems().stream().anyMatch(i ->
                !i.getId().equals(itemId) &&
                i.getMealType() == request.getMealType() &&
                i.getFoodName().equalsIgnoreCase(foodNameTrimmed));

        if (duplicateConflict) {
            throw new IllegalArgumentException("Food item '" + foodNameTrimmed + "' already exists under " + request.getMealType());
        }

        targetItem.setMealType(request.getMealType());
        targetItem.setFoodName(foodNameTrimmed);
        targetItem.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        menu.setUpdatedAt(Instant.now());
        Menu updatedMenu = menuRepository.save(menu);
        return mapToMenuResponse(updatedMenu);
    }

    @Override
    public MenuResponse deleteMenuItem(String menuId, String itemId, String ownerId) {
        Menu menu = getMenuOwnedByOwner(menuId, ownerId);

        boolean removed = menu.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new MenuItemNotFoundException("Menu item not found with id: " + itemId);
        }

        menu.setUpdatedAt(Instant.now());
        Menu updatedMenu = menuRepository.save(menu);
        return mapToMenuResponse(updatedMenu);
    }

    @Override
    public void deleteMenu(String menuId, String ownerId) {
        Menu menu = getMenuOwnedByOwner(menuId, ownerId);
        menuRepository.delete(menu);
    }

    @Override
    public WeeklyMenuResponse getOwnerWeeklyMenu(String messId, LocalDate startDate, String ownerId) {
        Mess mess = getMessOwnedByOwner(messId, ownerId);
        return buildWeeklyMenuResponse(mess, startDate);
    }

    // ==========================================
    // PUBLIC / USER OPERATIONS
    // ==========================================

    @Override
    public DailyMenuResponse getPublicTodayMenu(String messId) {
        Mess mess = getPubliclyVisibleMess(messId);
        LocalDate today = LocalDate.now();

        return menuRepository.findByMessIdAndMenuDate(mess.getId(), today)
                .map(this::mapToDailyMenuResponse)
                .orElseGet(() -> DailyMenuResponse.builder()
                        .messId(mess.getId())
                        .menuDate(today)
                        .isUploaded(false)
                        .breakfast(new ArrayList<>())
                        .lunch(new ArrayList<>())
                        .dinner(new ArrayList<>())
                        .build());
    }

    @Override
    public WeeklyMenuResponse getPublicWeeklyMenu(String messId, LocalDate startDate) {
        Mess mess = getPubliclyVisibleMess(messId);
        return buildWeeklyMenuResponse(mess, startDate);
    }

    // ==========================================
    // HELPER & VALIDATION METHODS
    // ==========================================

    private void validateOwnerAccount(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new UnauthorizedMessAccessException("Authentication required: owner is not authenticated");
        }
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new UnauthorizedMessAccessException("Owner account not found with id: " + ownerId));

        if (owner.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER accounts can manage menus");
        }
        if (owner.getStatus() == AccountStatus.PENDING) {
            throw new OwnerPendingException("Your account is pending admin approval. Please wait for verification.");
        }
        if (owner.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Your account is currently inactive. Please contact MessLoc support.");
        }
    }

    private Mess getMessOwnedByOwner(String messId, String ownerId) {
        validateOwnerAccount(ownerId);
        Mess mess = messRepository.findById(messId)
                .orElseThrow(() -> new MessNotFoundException("Mess not found with id: " + messId));

        if (!mess.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedMessAccessException("You are not authorized to manage menus for this mess");
        }
        return mess;
    }

    private Menu getMenuOwnedByOwner(String menuId, String ownerId) {
        validateOwnerAccount(ownerId);
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuNotFoundException("Menu not found with id: " + menuId));

        Mess mess = messRepository.findById(menu.getMessId())
                .orElseThrow(() -> new MessNotFoundException("Mess not found with id: " + menu.getMessId()));

        if (!mess.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedMessAccessException("You are not authorized to manage this menu");
        }
        return menu;
    }

    private Mess getPubliclyVisibleMess(String messId) {
        Mess mess = messRepository.findById(messId)
                .orElseThrow(() -> new MessNotFoundException("Mess not found with id: " + messId));

        if (mess.getVerificationStatus() != VerificationStatus.APPROVED || mess.getStatus() != MessStatus.ACTIVE) {
            throw new MessNotFoundException("Mess is not publicly available or pending verification");
        }
        return mess;
    }

    private void validateNoDuplicateFoodItems(List<MenuItemRequest> items) {
        Set<String> seen = new HashSet<>();
        for (MenuItemRequest item : items) {
            String key = item.getMealType().name() + ":" + item.getFoodName().trim().toLowerCase();
            if (!seen.add(key)) {
                throw new IllegalArgumentException("Duplicate food item '" + item.getFoodName().trim() + "' specified under " + item.getMealType());
            }
        }
    }

    private WeeklyMenuResponse buildWeeklyMenuResponse(Mess mess, LocalDate requestedStartDate) {
        LocalDate start = (requestedStartDate != null) ? requestedStartDate : LocalDate.now();
        LocalDate end = start.plusDays(6);

        List<Menu> existingMenus = menuRepository.findByMessIdAndMenuDateBetweenOrderByMenuDateAsc(mess.getId(), start, end);
        Map<LocalDate, Menu> menuByDate = existingMenus.stream()
                .collect(Collectors.toMap(Menu::getMenuDate, Function.identity()));

        List<DailyMenuResponse> dailyList = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            Menu dailyMenu = menuByDate.get(date);
            if (dailyMenu != null) {
                dailyList.add(mapToDailyMenuResponse(dailyMenu));
            } else {
                dailyList.add(DailyMenuResponse.builder()
                        .messId(mess.getId())
                        .menuDate(date)
                        .isUploaded(false)
                        .breakfast(new ArrayList<>())
                        .lunch(new ArrayList<>())
                        .dinner(new ArrayList<>())
                        .build());
            }
        }

        return WeeklyMenuResponse.builder()
                .messId(mess.getId())
                .messName(mess.getName())
                .startDate(start)
                .endDate(end)
                .menus(dailyList)
                .build();
    }

    private MenuResponse mapToMenuResponse(Menu menu) {
        List<MenuItemResponse> itemResponses = menu.getItems().stream()
                .map(this::mapToMenuItemResponse)
                .collect(Collectors.toList());

        return MenuResponse.builder()
                .id(menu.getId())
                .messId(menu.getMessId())
                .menuDate(menu.getMenuDate())
                .items(itemResponses)
                .createdAt(menu.getCreatedAt())
                .updatedAt(menu.getUpdatedAt())
                .build();
    }

    private MenuItemResponse mapToMenuItemResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .mealType(item.getMealType())
                .foodName(item.getFoodName())
                .description(item.getDescription())
                .build();
    }

    private DailyMenuResponse mapToDailyMenuResponse(Menu menu) {
        List<MenuItemResponse> breakfast = new ArrayList<>();
        List<MenuItemResponse> lunch = new ArrayList<>();
        List<MenuItemResponse> dinner = new ArrayList<>();

        for (MenuItem item : menu.getItems()) {
            MenuItemResponse resp = mapToMenuItemResponse(item);
            if (item.getMealType() == MealType.BREAKFAST) {
                breakfast.add(resp);
            } else if (item.getMealType() == MealType.LUNCH) {
                lunch.add(resp);
            } else if (item.getMealType() == MealType.DINNER) {
                dinner.add(resp);
            }
        }

        return DailyMenuResponse.builder()
                .menuId(menu.getId())
                .messId(menu.getMessId())
                .menuDate(menu.getMenuDate())
                .isUploaded(true)
                .breakfast(breakfast)
                .lunch(lunch)
                .dinner(dinner)
                .build();
    }
}

