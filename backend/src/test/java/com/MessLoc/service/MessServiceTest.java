package com.MessLoc.service;

import com.MessLoc.dto.mess.CreateMessRequest;
import com.MessLoc.dto.mess.MessResponse;
import com.MessLoc.dto.mess.PublicMessResponse;
import com.MessLoc.entity.Mess;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.FoodType;
import com.MessLoc.enums.MessStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.enums.VerificationStatus;
import com.MessLoc.exception.AccountInactiveException;
import com.MessLoc.exception.MessNotFoundException;
import com.MessLoc.exception.OwnerPendingException;
import com.MessLoc.repository.MessRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.service.impl.MessServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessServiceTest {

    @Mock
    private MessRepository messRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessServiceImpl messService;

    private User activeOwner;
    private User pendingOwner;
    private User inactiveOwner;
    private User regularUser;
    private CreateMessRequest validRequest;

    @BeforeEach
    void setUp() {
        activeOwner = User.builder()
                .id("o-101")
                .name("Sharma Owner")
                .email("owner@messloc.com")
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
                .id("u-user")
                .name("Regular User")
                .email("user@messloc.com")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        validRequest = CreateMessRequest.builder()
                .name("Annapurna Mess")
                .description("Pure vegetarian homely meals")
                .address("Rajpur Road, Near Clock Tower")
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
                .facilities(List.of("WiFi", "RO Water", "Parking"))
                .build();
    }

    @Test
    void testCreateMess_ValidRequest_SavesWithPendingVerificationAndCorrectOwnerId() {
        when(userRepository.findById("o-101")).thenReturn(Optional.of(activeOwner));
        when(messRepository.save(any(Mess.class))).thenAnswer(invocation -> {
            Mess m = invocation.getArgument(0);
            m.setId("mess-generated-id");
            return m;
        });

        MessResponse response = messService.createMess(validRequest, "o-101");

        assertNotNull(response);
        assertEquals("mess-generated-id", response.getId());
        assertEquals("Annapurna Mess", response.getName());
        assertEquals("o-101", response.getOwnerId());
        assertEquals(VerificationStatus.PENDING, response.getVerificationStatus());
        assertEquals(MessStatus.ACTIVE, response.getStatus());

        ArgumentCaptor<Mess> messCaptor = ArgumentCaptor.forClass(Mess.class);
        verify(messRepository).save(messCaptor.capture());

        Mess capturedMess = messCaptor.getValue();
        assertEquals("o-101", capturedMess.getOwnerId());
        assertEquals(VerificationStatus.PENDING, capturedMess.getVerificationStatus());
        assertEquals(MessStatus.ACTIVE, capturedMess.getStatus());
        assertNotNull(capturedMess.getCreatedAt());
        assertNotNull(capturedMess.getUpdatedAt());
    }

    @Test
    void testCreateMess_PendingOwner_ThrowsOwnerPendingException() {
        when(userRepository.findById("o-pending")).thenReturn(Optional.of(pendingOwner));

        assertThrows(OwnerPendingException.class, () ->
                messService.createMess(validRequest, "o-pending"));

        verify(messRepository, never()).save(any());
    }

    @Test
    void testCreateMess_InactiveOwner_ThrowsAccountInactiveException() {
        when(userRepository.findById("o-inactive")).thenReturn(Optional.of(inactiveOwner));

        assertThrows(AccountInactiveException.class, () ->
                messService.createMess(validRequest, "o-inactive"));

        verify(messRepository, never()).save(any());
    }

    @Test
    void testCreateMess_NonOwnerRole_ThrowsAccessDeniedException() {
        when(userRepository.findById("u-user")).thenReturn(Optional.of(regularUser));

        assertThrows(AccessDeniedException.class, () ->
                messService.createMess(validRequest, "u-user"));

        verify(messRepository, never()).save(any());
    }

    @Test
    void testCreateMess_InvalidLatitude_ThrowsIllegalArgumentException() {
        when(userRepository.findById("o-101")).thenReturn(Optional.of(activeOwner));
        validRequest.setLatitude(95.0); // Invalid: > 90

        assertThrows(IllegalArgumentException.class, () ->
                messService.createMess(validRequest, "o-101"));

        verify(messRepository, never()).save(any());
    }

    @Test
    void testCreateMess_InvalidLongitude_ThrowsIllegalArgumentException() {
        when(userRepository.findById("o-101")).thenReturn(Optional.of(activeOwner));
        validRequest.setLongitude(190.0); // Invalid: > 180

        assertThrows(IllegalArgumentException.class, () ->
                messService.createMess(validRequest, "o-101"));

        verify(messRepository, never()).save(any());
    }

    @Test
    void testCreateMess_InvalidTimeOrder_ThrowsIllegalArgumentException() {
        when(userRepository.findById("o-101")).thenReturn(Optional.of(activeOwner));
        validRequest.setOpeningTime(LocalTime.of(22, 0));
        validRequest.setClosingTime(LocalTime.of(8, 0)); // Invalid: opening after closing

        assertThrows(IllegalArgumentException.class, () ->
                messService.createMess(validRequest, "o-101"));

        verify(messRepository, never()).save(any());
    }

    @Test
    void testGetPublicMesses_OnlyReturnsApprovedAndActive() {
        Mess approvedMess = Mess.builder()
                .id("m-approved")
                .name("Approved Mess")
                .verificationStatus(VerificationStatus.APPROVED)
                .status(MessStatus.ACTIVE)
                .build();

        when(messRepository.findByVerificationStatusAndStatus(VerificationStatus.APPROVED, MessStatus.ACTIVE))
                .thenReturn(List.of(approvedMess));

        List<PublicMessResponse> publicMesses = messService.getPublicMesses();

        assertEquals(1, publicMesses.size());
        assertEquals("Approved Mess", publicMesses.get(0).getName());
    }

    @Test
    void testGetPublicMess_UnapprovedMess_ThrowsMessNotFoundException() {
        Mess pendingMess = Mess.builder()
                .id("m-pending")
                .name("Pending Mess")
                .verificationStatus(VerificationStatus.PENDING)
                .status(MessStatus.ACTIVE)
                .build();

        when(messRepository.findById("m-pending")).thenReturn(Optional.of(pendingMess));

        assertThrows(MessNotFoundException.class, () ->
                messService.getPublicMess("m-pending"));
    }
}

