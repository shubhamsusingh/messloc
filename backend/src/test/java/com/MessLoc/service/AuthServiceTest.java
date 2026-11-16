package com.MessLoc.service;

import com.MessLoc.dto.request.ChangePasswordRequest;
import com.MessLoc.dto.request.ForgotPasswordRequest;
import com.MessLoc.dto.request.OwnerSignupRequest;
import com.MessLoc.dto.request.ResetPasswordRequest;
import com.MessLoc.dto.request.SigninRequest;
import com.MessLoc.dto.request.UserSignupRequest;
import com.MessLoc.dto.response.AuthResponse;
import com.MessLoc.entity.OwnerProfile;
import com.MessLoc.entity.PasswordResetToken;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.exception.AccountBlockedException;
import com.MessLoc.exception.AccountInactiveException;
import com.MessLoc.exception.EmailAlreadyExistsException;
import com.MessLoc.exception.InvalidCredentialsException;
import com.MessLoc.exception.InvalidResetTokenException;
import com.MessLoc.exception.OwnerPendingException;
import com.MessLoc.exception.RateLimitExceededException;
import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.PasswordResetTokenRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.security.JwtService;
import com.MessLoc.service.EmailService;
import com.MessLoc.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

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
    void testRegisterOwner_Success_PendingStatus_NoTokenIssued() {
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

        AuthResponse response = authService.registerOwner(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNull(response.getToken(), "Pending owners must not be issued a JWT token upon registration");
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

        Authentication mockAuth = mock(Authentication.class);
        CustomUserDetails userDetails = new CustomUserDetails(sampleUser);
        when(mockAuth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);
        when(jwtService.generateToken(sampleUser)).thenReturn("mock_jwt_token");
        when(jwtService.getJwtExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.signin(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("mock_jwt_token", response.getToken());
        assertEquals("shivam@example.com", response.getUser().getEmail());
    }

    @Test
    void testSignin_InvalidPassword_ThrowsInvalidCredentialsException() {
        SigninRequest request = SigninRequest.builder()
                .email("shivam@example.com")
                .password("WrongPassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.signin(request));
    }

    @Test
    void testSignin_UnknownEmail_ThrowsInvalidCredentialsException() {
        SigninRequest request = SigninRequest.builder()
                .email("unknown@example.com")
                .password("Password123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.signin(request));
    }

    @Test
    void testSignin_BlockedAccount_ThrowsAccountBlockedException() {
        SigninRequest request = SigninRequest.builder()
                .email("shivam@example.com")
                .password("Password123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException("User account is locked"));

        assertThrows(AccountBlockedException.class, () -> authService.signin(request));
    }

    @Test
    void testSignin_InactiveAccount_ThrowsAccountInactiveException() {
        sampleUser.setStatus(AccountStatus.INACTIVE);

        SigninRequest request = SigninRequest.builder()
                .email("shivam@example.com")
                .password("Password123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));
        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));

        assertThrows(AccountInactiveException.class, () -> authService.signin(request));
    }

    @Test
    void testSignin_PendingAccount_ThrowsOwnerPendingException() {
        sampleUser.setStatus(AccountStatus.PENDING);

        SigninRequest request = SigninRequest.builder()
                .email("shivam@example.com")
                .password("Password123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));
        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));

        assertThrows(OwnerPendingException.class, () -> authService.signin(request));
    }

    @Test
    void testRegisterUser_PasswordIsSecurelyHashedBeforeStorage() {
        UserSignupRequest request = UserSignupRequest.builder()
                .name("Shivam Kumar")
                .email("shivam@example.com")
                .password("PlainSecretPassword123")
                .phone("9876543210")
                .build();

        when(userRepository.existsByEmail("shivam@example.com")).thenReturn(false);
        when(passwordEncoder.encode("PlainSecretPassword123")).thenReturn("$2a$12$e8FvMockedBCryptHashStringHere12345");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("mock_token");
        when(jwtService.getJwtExpirationMs()).thenReturn(86400000L);

        authService.registerUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNotEquals("PlainSecretPassword123", savedUser.getPassword(), "Plaintext password must NEVER be stored in the entity");
        assertTrue(savedUser.getPassword().startsWith("$2a$12$"), "Password must be BCrypt hashed");
        assertEquals(Role.USER, savedUser.getRole());
        assertEquals(AccountStatus.ACTIVE, savedUser.getStatus());
    }

    @Test
    void testRegisterUser_MongoDuplicateKeyException_TranslatesToEmailAlreadyExistsException() {
        UserSignupRequest request = UserSignupRequest.builder()
                .name("Shivam Kumar")
                .email("duplicate@example.com")
                .password("Password123")
                .phone("9876543210")
                .build();

        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("E11000 duplicate key error collection: users index: email_1 dup key"));

        assertThrows(EmailAlreadyExistsException.class, () -> authService.registerUser(request));
    }

    @Test
    void testGetCurrentUser_Success() {
        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));

        var response = authService.getCurrentUser("shivam@example.com");

        assertNotNull(response);
        assertEquals("shivam@example.com", response.getEmail());
        assertEquals("Shivam Kumar", response.getName());
        assertEquals(Role.USER, response.getRole());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
    }

    @Test
    void testGetCurrentUser_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(com.MessLoc.exception.ResourceNotFoundException.class,
                () -> authService.getCurrentUser("nonexistent@example.com"));
    }

    // ==========================================
    // CHANGE PASSWORD TESTS
    // ==========================================

    @Test
    void testChangePassword_Success() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPass123@")
                .newPassword("NewPass456@")
                .confirmPassword("NewPass456@")
                .build();

        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("OldPass123@", sampleUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("NewPass456@", sampleUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("NewPass456@")).thenReturn("encoded_new_pass");

        authService.changePassword(request, "shivam@example.com");

        assertEquals("encoded_new_pass", sampleUser.getPassword());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void testChangePassword_IncorrectCurrentPassword_ThrowsInvalidCredentialsException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPass123@")
                .newPassword("NewPass456@")
                .confirmPassword("NewPass456@")
                .build();

        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPass123@", sampleUser.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () ->
                authService.changePassword(request, "shivam@example.com"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testChangePassword_PasswordMismatch_ThrowsIllegalArgumentException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPass123@")
                .newPassword("NewPass456@")
                .confirmPassword("DifferentPass789@")
                .build();

        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("OldPass123@", sampleUser.getPassword())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                authService.changePassword(request, "shivam@example.com"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testChangePassword_SamePassword_ThrowsIllegalArgumentException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPass123@")
                .newPassword("OldPass123@")
                .confirmPassword("OldPass123@")
                .build();

        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("OldPass123@", sampleUser.getPassword())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                authService.changePassword(request, "shivam@example.com"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testChangePassword_WeakPassword_ThrowsIllegalArgumentException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPass123@")
                .newPassword("weak")
                .confirmPassword("weak")
                .build();

        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("OldPass123@", sampleUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("weak", sampleUser.getPassword())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                authService.changePassword(request, "shivam@example.com"));
        verify(userRepository, never()).save(any());
    }

    // ==========================================
    // FORGOT PASSWORD TESTS
    // ==========================================

    @Test
    void testForgotPassword_RegisteredEmail_GeneratesTokenAndCallsEmailService() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("shivam@example.com")
                .build();

        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordResetTokenRepository.countByUserIdAndCreatedAtAfter(eq("user-1"), any())).thenReturn(0);
        when(passwordResetTokenRepository.findByUserIdAndUsedFalse("user-1")).thenReturn(List.of());

        authService.forgotPassword(request);

        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(eq("shivam@example.com"), eq("Shivam Kumar"), anyString());
    }

    @Test
    void testForgotPassword_UnregisteredEmail_ReturnsSilentlyWithoutSendingEmail() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("unknown@example.com")
                .build();

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.forgotPassword(request));
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void testForgotPassword_RateLimitExceeded_ThrowsRateLimitExceededException() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("shivam@example.com")
                .build();

        when(userRepository.findByEmail("shivam@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordResetTokenRepository.countByUserIdAndCreatedAtAfter(eq("user-1"), any())).thenReturn(3);

        assertThrows(RateLimitExceededException.class, () -> authService.forgotPassword(request));
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    // ==========================================
    // RESET PASSWORD TESTS
    // ==========================================

    @Test
    void testResetPassword_Success() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("rawTokenHex1234567890abcdef")
                .newPassword("FreshPassword123@")
                .confirmPassword("FreshPassword123@")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id("token-1")
                .userId("user-1")
                .tokenHash("someHash")
                .expiryDate(Instant.now().plusSeconds(600))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(anyString())).thenReturn(Optional.of(resetToken));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("FreshPassword123@", sampleUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("FreshPassword123@")).thenReturn("new_encoded_password");

        authService.resetPassword(request);

        assertTrue(resetToken.isUsed());
        assertNotNull(resetToken.getUsedAt());
        assertEquals("new_encoded_password", sampleUser.getPassword());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void testResetPassword_InvalidToken_ThrowsInvalidResetTokenException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("invalidOrConsumedToken")
                .newPassword("FreshPassword123@")
                .confirmPassword("FreshPassword123@")
                .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidResetTokenException.class, () -> authService.resetPassword(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testResetPassword_ExpiredToken_ThrowsInvalidResetTokenException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("expiredToken12345")
                .newPassword("FreshPassword123@")
                .confirmPassword("FreshPassword123@")
                .build();

        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .id("token-exp")
                .userId("user-1")
                .tokenHash("someHash")
                .expiryDate(Instant.now().minusSeconds(60))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(anyString())).thenReturn(Optional.of(expiredToken));

        assertThrows(InvalidResetTokenException.class, () -> authService.resetPassword(request));
        assertTrue(expiredToken.isUsed());
        verify(passwordResetTokenRepository, times(1)).save(expiredToken);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testResetPassword_PasswordMismatch_ThrowsIllegalArgumentException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("anyToken")
                .newPassword("FreshPassword123@")
                .confirmPassword("MismatchedPassword123@")
                .build();

        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
        verify(passwordResetTokenRepository, never()).findByTokenHashAndUsedFalse(any());
    }
}
