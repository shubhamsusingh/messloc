package com.MessLoc.service;

import com.MessLoc.dto.request.CreateAdminRequest;
import com.MessLoc.dto.response.UserResponse;
import com.MessLoc.entity.OwnerProfile;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.exception.AccountBlockedException;
import com.MessLoc.exception.AccountInactiveException;
import com.MessLoc.exception.EmailAlreadyExistsException;
import com.MessLoc.exception.ResourceNotFoundException;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OwnerProfileRepository ownerProfileRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @InjectMocks
    private AdminServiceImpl adminService;

    private User activeAdminUser;

    @BeforeEach
    void setUp() {
        activeAdminUser = User.builder()
                .id("admin-1")
                .name("Super Admin")
                .email("admin@messloc.com")
                .password(passwordEncoder.encode("AdminPass123"))
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(activeAdminUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Create Admin - Success: Active admin creates new admin with encrypted password, ADMIN role and ACTIVE status")
    void testCreateAdmin_Success() {
        CreateAdminRequest request = CreateAdminRequest.builder()
                .name("Jane Admin")
                .email("  Jane.Admin@MessLoc.com  ")
                .password("SecureAdminPass123")
                .phone("9876543210")
                .build();

        when(userRepository.existsByEmail("jane.admin@messloc.com")).thenReturn(false);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setId("saved-admin-id");
            toSave.setCreatedAt(LocalDateTime.now());
            return toSave;
        });

        UserResponse response = adminService.createAdmin(request);

        assertNotNull(response);
        assertEquals("saved-admin-id", response.getId());
        assertEquals("Jane Admin", response.getName());
        assertEquals("jane.admin@messloc.com", response.getEmail());
        assertEquals("9876543210", response.getPhone());
        assertEquals(Role.ADMIN, response.getRole());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());

        // Verify entity state saved to repository
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals(Role.ADMIN, savedUser.getRole());
        assertEquals(AccountStatus.ACTIVE, savedUser.getStatus());
        assertEquals("jane.admin@messloc.com", savedUser.getEmail());
        assertNotEquals("SecureAdminPass123", savedUser.getPassword());
        assertTrue(passwordEncoder.matches("SecureAdminPass123", savedUser.getPassword()),
                "Saved password must be a valid BCrypt hash matching raw password");
    }

    @Test
    @DisplayName("Create Admin - Duplicate Email: Throws EmailAlreadyExistsException (409)")
    void testCreateAdmin_DuplicateEmail_Throws409() {
        CreateAdminRequest request = CreateAdminRequest.builder()
                .name("Duplicate Admin")
                .email("admin@messloc.com")
                .password("SecureAdminPass123")
                .phone("9876543210")
                .build();

        when(userRepository.existsByEmail("admin@messloc.com")).thenReturn(true);

        EmailAlreadyExistsException ex = assertThrows(EmailAlreadyExistsException.class,
                () -> adminService.createAdmin(request));

        assertTrue(ex.getMessage().contains("admin@messloc.com"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Admin - Blocked Admin Caller: Throws AccountBlockedException (403)")
    void testCreateAdmin_BlockedAdminCaller_Throws403() {
        User blockedAdmin = User.builder()
                .id("blocked-admin")
                .email("blocked@messloc.com")
                .role(Role.ADMIN)
                .status(AccountStatus.BLOCKED)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(blockedAdmin);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        CreateAdminRequest request = CreateAdminRequest.builder()
                .name("New Admin")
                .email("new@messloc.com")
                .password("Password123")
                .phone("9876543210")
                .build();

        assertThrows(AccountBlockedException.class, () -> adminService.createAdmin(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Admin - Inactive Admin Caller: Throws AccountInactiveException (403)")
    void testCreateAdmin_InactiveAdminCaller_Throws403() {
        User inactiveAdmin = User.builder()
                .id("inactive-admin")
                .email("inactive@messloc.com")
                .role(Role.ADMIN)
                .status(AccountStatus.INACTIVE)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(inactiveAdmin);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        CreateAdminRequest request = CreateAdminRequest.builder()
                .name("New Admin")
                .email("new@messloc.com")
                .password("Password123")
                .phone("9876543210")
                .build();

        assertThrows(AccountInactiveException.class, () -> adminService.createAdmin(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Admin - Non-Admin Caller: Throws AccessDeniedException (403)")
    void testCreateAdmin_NonAdminCaller_ThrowsAccessDenied() {
        User regularUser = User.builder()
                .id("user-1")
                .email("user@messloc.com")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(regularUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        CreateAdminRequest request = CreateAdminRequest.builder()
                .name("New Admin")
                .email("new@messloc.com")
                .password("Password123")
                .phone("9876543210")
                .build();

        assertThrows(AccessDeniedException.class, () -> adminService.createAdmin(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get Pending Owners - Success: Returns mapped UserResponse list")
    void testGetPendingOwners_Success() {
        User pendingOwner = User.builder()
                .id("owner-1")
                .name("Owner One")
                .email("owner1@messloc.com")
                .phone("9876543210")
                .role(Role.OWNER)
                .status(AccountStatus.PENDING)
                .build();

        when(userRepository.findByRoleAndStatus(Role.OWNER, AccountStatus.PENDING))
                .thenReturn(List.of(pendingOwner));

        List<UserResponse> list = adminService.getPendingOwners();
        assertEquals(1, list.size());
        assertEquals("owner1@messloc.com", list.get(0).getEmail());
    }

    @Test
    @DisplayName("Verify Owner - Success: Updates user to ACTIVE and marks profile verified")
    void testVerifyOwner_Success() {
        User user = User.builder()
                .id("owner-1")
                .status(AccountStatus.PENDING)
                .role(Role.OWNER)
                .build();

        OwnerProfile profile = OwnerProfile.builder()
                .id("prof-1")
                .userId("owner-1")
                .verified(false)
                .build();

        when(userRepository.findById("owner-1")).thenReturn(Optional.of(user));
        when(ownerProfileRepository.findByUserId("owner-1")).thenReturn(Optional.of(profile));

        adminService.verifyOwner("owner-1");

        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        assertTrue(profile.isVerified());
        verify(userRepository).save(user);
        verify(ownerProfileRepository).save(profile);
    }

    @Test
    @DisplayName("Verify Owner - User Not Found: Throws ResourceNotFoundException")
    void testVerifyOwner_NotFound_Throws404() {
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.verifyOwner("non-existent"));
    }

    @Test
    @DisplayName("Block Owner - Success: Updates user to BLOCKED")
    void testBlockOwner_Success() {
        User user = User.builder()
                .id("owner-1")
                .status(AccountStatus.ACTIVE)
                .role(Role.OWNER)
                .build();

        when(userRepository.findById("owner-1")).thenReturn(Optional.of(user));

        adminService.blockOwner("owner-1");

        assertEquals(AccountStatus.BLOCKED, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Block Owner - User Not Found: Throws ResourceNotFoundException")
    void testBlockOwner_NotFound_Throws404() {
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.blockOwner("non-existent"));
    }
}

