package com.MessLoc.service;

import com.MessLoc.dto.request.UpdateUserProfileRequest;
import com.MessLoc.dto.response.UserProfileResponse;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.exception.ResourceNotFoundException;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id("u-101")
                .name("Shivam Kumar")
                .email("shivam@messloc.com")
                .password("$2a$12$SecureHashedPassword")
                .phone("9876543210")
                .city("Dehradun")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .profileImage("https://example.com/avatar.jpg")
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    @Test
    void testGetAuthenticatedUserProfile_Success() {
        when(userRepository.findByEmail("shivam@messloc.com")).thenReturn(Optional.of(existingUser));

        UserProfileResponse response = userService.getAuthenticatedUserProfile("shivam@messloc.com");

        assertNotNull(response);
        assertEquals("u-101", response.getId());
        assertEquals("Shivam Kumar", response.getName());
        assertEquals("shivam@messloc.com", response.getEmail());
        assertEquals("9876543210", response.getPhone());
        assertEquals("Dehradun", response.getCity());
        assertEquals(Role.USER, response.getRole());
        assertEquals(AccountStatus.ACTIVE, response.getAccountStatus());
        assertEquals("https://example.com/avatar.jpg", response.getProfileImage());
    }

    @Test
    void testGetAuthenticatedUserProfile_NotFound_ThrowsException() {
        when(userRepository.findByEmail("unknown@messloc.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                userService.getAuthenticatedUserProfile("unknown@messloc.com"));
    }

    @Test
    void testUpdateAuthenticatedUserProfile_AllFields_Success() {
        when(userRepository.findByEmail("shivam@messloc.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("Shivam Kumar Updated")
                .phone("9876543211")
                .city("Rishikesh")
                .profileImage("https://example.com/new-avatar.jpg")
                .build();

        UserProfileResponse response = userService.updateAuthenticatedUserProfile("shivam@messloc.com", request);

        assertNotNull(response);
        assertEquals("Shivam Kumar Updated", response.getName());
        assertEquals("9876543211", response.getPhone());
        assertEquals("Rishikesh", response.getCity());
        assertEquals("https://example.com/new-avatar.jpg", response.getProfileImage());
        // Verify immutable fields are strictly preserved
        assertEquals("u-101", response.getId());
        assertEquals("shivam@messloc.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());
        assertEquals(AccountStatus.ACTIVE, response.getAccountStatus());
        assertEquals("$2a$12$SecureHashedPassword", existingUser.getPassword());

        verify(userRepository).save(existingUser);
    }

    @Test
    void testUpdateAuthenticatedUserProfile_PartialUpdate_PreservesUnsuppliedFields() {
        when(userRepository.findByEmail("shivam@messloc.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Only updating name — phone, city, profileImage are null
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("Only Name Changed")
                .build();

        UserProfileResponse response = userService.updateAuthenticatedUserProfile("shivam@messloc.com", request);

        assertNotNull(response);
        assertEquals("Only Name Changed", response.getName());
        // Existing unsupplied fields must NOT be overwritten with null
        assertEquals("9876543210", response.getPhone());
        assertEquals("Dehradun", response.getCity());
        assertEquals("https://example.com/avatar.jpg", response.getProfileImage());
        assertEquals("shivam@messloc.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());

        verify(userRepository).save(existingUser);
    }

    @Test
    void testUpdateAuthenticatedUserProfile_BlankName_ThrowsIllegalArgumentException() {
        when(userRepository.findByEmail("shivam@messloc.com")).thenReturn(Optional.of(existingUser));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("   ")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                userService.updateAuthenticatedUserProfile("shivam@messloc.com", request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdateAuthenticatedUserProfile_BlankPhone_ThrowsIllegalArgumentException() {
        when(userRepository.findByEmail("shivam@messloc.com")).thenReturn(Optional.of(existingUser));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .phone("   ")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                userService.updateAuthenticatedUserProfile("shivam@messloc.com", request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdateAuthenticatedUserProfile_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("unknown@messloc.com")).thenReturn(Optional.empty());

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("New Name")
                .build();

        assertThrows(ResourceNotFoundException.class, () ->
                userService.updateAuthenticatedUserProfile("unknown@messloc.com", request));

        verify(userRepository, never()).save(any());
    }
}

