package com.MessLoc.service;

import com.MessLoc.dto.request.OwnerSignupRequest;
import com.MessLoc.dto.request.SigninRequest;
import com.MessLoc.dto.request.UserSignupRequest;
import com.MessLoc.dto.response.AuthResponse;
import com.MessLoc.entity.OwnerProfile;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.exception.AccountBlockedException;
import com.MessLoc.exception.AccountInactiveException;
import com.MessLoc.exception.EmailAlreadyExistsException;
import com.MessLoc.exception.InvalidCredentialsException;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.JwtService;
import com.MessLoc.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OwnerProfileRepository ownerProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("user-1")
                .name("Shivam Kumar")
                .email("shivam@example.com")
                .password("encoded_pass")
                .phone("9876543210")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void testRegisterUser_Success() {
        UserSignupRequest request = UserSignupRequest.builder()
                .name("Shivam Kumar")
                .email("shivam@example.com")
                .password("Password123")
                .phone("9876543210")
                .build();

        when(userRepository.existsByEmail("shivam@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("mock_jwt_token");
        when(jwtService.getJwtExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.registerUser(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("mock_jwt_token", response.getToken());
        assertEquals(Role.USER, response.getUser().getRole());
        assertEquals(AccountStatus.ACTIVE, response.getUser().getStatus());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_DuplicateEmail_ThrowsEmailAlreadyExistsException() {
        UserSignupRequest request = UserSignupRequest.builder()
                .name("Shivam Kumar")
                .email("shivam@example.com")
                .password("Password123")
                .phone("9876543210")
                .build();

        when(userRepository.existsByEmail("shivam@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterOwner_Success_PendingStatus() {
        OwnerSignupRequest request = OwnerSignupRequest.builder()
                .name("Rahul Sharma")
                .email("rahul@example.com")
                .password("Password123")
                .phone("9876543210")
                .businessName("Sharma Mess")
                .businessDescription("Affordable home-cooked food")
                .build();

        User ownerUser = User.builder()
                .id("owner-1")
                .name("Rahul Sharma")
                .email("rahul@example.com")
                .password("encoded_pass")
                .phone("9876543210")
                .role(Role.OWNER)
                .status(AccountStatus.PENDING)
                .build();

        when(userRepository.existsByEmail("rahul@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(ownerUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("mock_jwt_token");
        when(jwtService.getJwtExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.registerOwner(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(Role.OWNER, response.getUser().getRole());
        assertEquals(AccountStatus.PENDING, response.getUser().getStatus());
        verify(ownerProfileRepository, times(1)).save(any(OwnerProfile.class));
    }

    @Test
    void testSignin_Success() {
        SigninRequest request = SigninRequest.builder()
                .email("shivam@example.com")
                .password("Password123")
                .build();

        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123", sampleUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken(sampleUser)).thenReturn("mock_jwt_token");
        when(jwtService.getJwtExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.signin(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("mock_jwt_token", response.getToken());
    }

    @Test
    void testSignin_InvalidPassword_ThrowsInvalidCredentialsException() {
        SigninRequest request = SigninRequest.builder()
                .email("shivam@example.com")
                .password("WrongPassword")
                .build();

        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword", sampleUser.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.signin(request));
    }

    @Test
    void testSignin_BlockedAccount_ThrowsAccountBlockedException() {
        sampleUser.setStatus(AccountStatus.BLOCKED);

        SigninRequest request = SigninRequest.builder()
                .email("shivam@example.com")
                .password("Password123")
                .build();

        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123", sampleUser.getPassword())).thenReturn(true);

        assertThrows(AccountBlockedException.class, () -> authService.signin(request));
    }

    @Test
    void testSignin_InactiveAccount_ThrowsAccountInactiveException() {
        sampleUser.setStatus(AccountStatus.INACTIVE);

        SigninRequest request = SigninRequest.builder()
                .email("shivam@example.com")
                .password("Password123")
                .build();

        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123", sampleUser.getPassword())).thenReturn(true);

        assertThrows(AccountInactiveException.class, () -> authService.signin(request));
    }
}
