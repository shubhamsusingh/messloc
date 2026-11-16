package com.MessLoc.service;

import com.MessLoc.dto.subscription.CreateSubscriptionRequest;
import com.MessLoc.dto.subscription.SubscriptionResponse;
import com.MessLoc.entity.Mess;
import com.MessLoc.entity.Subscription;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.MessStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.enums.SubscriptionStatus;
import com.MessLoc.enums.VerificationStatus;
import com.MessLoc.exception.AccountBlockedException;
import com.MessLoc.exception.AccountInactiveException;
import com.MessLoc.exception.DuplicateSubscriptionException;
import com.MessLoc.exception.MessNotFoundException;
import com.MessLoc.exception.ResourceNotFoundException;
import com.MessLoc.exception.SubscriptionNotFoundException;
import com.MessLoc.exception.UnauthorizedMessAccessException;
import com.MessLoc.repository.MessRepository;
import com.MessLoc.repository.SubscriptionRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private MessRepository messRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private User activeUser;
    private User blockedUser;
    private User inactiveUser;
    private User messOwner;
    private Mess approvedActiveMess;
    private Mess pendingMess;
    private Mess rejectedMess;
    private Mess inactiveMess;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id("user-101")
                .name("Aman Verma")
                .email("aman@example.com")
                .phone("9876543210")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        blockedUser = User.builder()
                .id("user-102")
                .name("Blocked User")
                .email("blocked@example.com")
                .role(Role.USER)
                .status(AccountStatus.BLOCKED)
                .build();

        inactiveUser = User.builder()
                .id("user-103")
                .name("Inactive User")
                .email("inactive@example.com")
                .role(Role.USER)
                .status(AccountStatus.INACTIVE)
                .build();

        messOwner = User.builder()
                .id("owner-201")
                .name("Ramesh Kumar")
                .email("ramesh@example.com")
                .phone("9123456780")
                .role(Role.OWNER)
                .status(AccountStatus.ACTIVE)
                .build();

        approvedActiveMess = Mess.builder()
                .id("mess-301")
                .ownerId("owner-201")
                .name("Annapurna Mess")
                .monthlyPrice(3000.0)
                .verificationStatus(VerificationStatus.APPROVED)
                .status(MessStatus.ACTIVE)
                .build();

        pendingMess = Mess.builder()
                .id("mess-302")
                .ownerId("owner-201")
                .name("Pending Mess")
                .monthlyPrice(2500.0)
                .verificationStatus(VerificationStatus.PENDING)
                .status(MessStatus.ACTIVE)
                .build();

        rejectedMess = Mess.builder()
                .id("mess-303")
                .ownerId("owner-201")
                .name("Rejected Mess")
                .monthlyPrice(2500.0)
                .verificationStatus(VerificationStatus.REJECTED)
                .status(MessStatus.ACTIVE)
                .build();

        inactiveMess = Mess.builder()
                .id("mess-304")
                .ownerId("owner-201")
                .name("Inactive Mess")
                .monthlyPrice(2500.0)
                .verificationStatus(VerificationStatus.APPROVED)
                .status(MessStatus.INACTIVE)
                .build();
    }

    @Nested
    @DisplayName("Create Subscription Tests")
    class CreateSubscriptionTests {

        @Test
        @DisplayName("Should successfully create default monthly subscription")
        void testCreateSubscription_Success_DefaultMonthly() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .messId("mess-301")
                    .build();

            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));
            when(messRepository.findById("mess-301")).thenReturn(Optional.of(approvedActiveMess));
            when(subscriptionRepository.findByUserIdAndMessIdAndStatus("user-101", "mess-301", SubscriptionStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
                Subscription sub = invocation.getArgument(0);
                sub.setId("sub-501");
                return sub;
            });

            SubscriptionResponse response = subscriptionService.createSubscription(request, "user-101");

            assertNotNull(response);
            assertEquals("sub-501", response.getId());
            assertEquals("user-101", response.getUserId());
            assertEquals("Aman Verma", response.getUserName());
            assertEquals("mess-301", response.getMessId());
            assertEquals("Annapurna Mess", response.getMessName());
            assertEquals("monthly", response.getPlanId());
            assertEquals("Monthly Plan", response.getPlanName());
            assertEquals(30, response.getDurationDays());
            assertEquals(3000.0, response.getPrice());
            assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
            assertEquals(LocalDate.now(), response.getStartDate());
            assertEquals(LocalDate.now().plusDays(30), response.getEndDate());
            verify(subscriptionRepository, times(1)).save(any(Subscription.class));
        }

        @Test
        @DisplayName("Should successfully create quarterly plan subscription")
        void testCreateSubscription_Success_QuarterlyPlan() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .messId("mess-301")
                    .planId("quarterly")
                    .build();

            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));
            when(messRepository.findById("mess-301")).thenReturn(Optional.of(approvedActiveMess));
            when(subscriptionRepository.findByUserIdAndMessIdAndStatus("user-101", "mess-301", SubscriptionStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
                Subscription sub = invocation.getArgument(0);
                sub.setId("sub-502");
                return sub;
            });

            SubscriptionResponse response = subscriptionService.createSubscription(request, "user-101");

            assertNotNull(response);
            assertEquals("quarterly", response.getPlanId());
            assertEquals("Quarterly Plan", response.getPlanName());
            assertEquals(90, response.getDurationDays());
            assertEquals(9000.0, response.getPrice());
            assertEquals(LocalDate.now().plusDays(90), response.getEndDate());
        }

        @Test
        @DisplayName("Should successfully create yearly plan subscription")
        void testCreateSubscription_Success_YearlyPlan() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .messId("mess-301")
                    .planId("yearly")
                    .build();

            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));
            when(messRepository.findById("mess-301")).thenReturn(Optional.of(approvedActiveMess));
            when(subscriptionRepository.findByUserIdAndMessIdAndStatus("user-101", "mess-301", SubscriptionStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
                Subscription sub = invocation.getArgument(0);
                sub.setId("sub-503");
                return sub;
            });

            SubscriptionResponse response = subscriptionService.createSubscription(request, "user-101");

            assertNotNull(response);
            assertEquals("yearly", response.getPlanId());
            assertEquals("Yearly Plan", response.getPlanName());
            assertEquals(365, response.getDurationDays());
            assertEquals(36000.0, response.getPrice());
            assertEquals(LocalDate.now().plusDays(365), response.getEndDate());
        }

        @Test
        @DisplayName("Should reject subscription when user is not found")
        void testCreateSubscription_UserNotFound_ThrowsException() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder().messId("mess-301").build();
            when(userRepository.findById("unknown-user")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    subscriptionService.createSubscription(request, "unknown-user"));
        }

        @Test
        @DisplayName("Should reject subscription when user is blocked")
        void testCreateSubscription_UserBlocked_ThrowsException() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder().messId("mess-301").build();
            when(userRepository.findById("user-102")).thenReturn(Optional.of(blockedUser));

            assertThrows(AccountBlockedException.class, () ->
                    subscriptionService.createSubscription(request, "user-102"));
        }

        @Test
        @DisplayName("Should reject subscription when user is inactive")
        void testCreateSubscription_UserInactive_ThrowsException() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder().messId("mess-301").build();
            when(userRepository.findById("user-103")).thenReturn(Optional.of(inactiveUser));

            assertThrows(AccountInactiveException.class, () ->
                    subscriptionService.createSubscription(request, "user-103"));
        }

        @Test
        @DisplayName("Should reject subscription when mess is not found")
        void testCreateSubscription_MessNotFound_ThrowsException() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder().messId("non-existent").build();
            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));
            when(messRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(MessNotFoundException.class, () ->
                    subscriptionService.createSubscription(request, "user-101"));
        }

        @Test
        @DisplayName("Should reject subscription when mess verification is PENDING")
        void testCreateSubscription_MessPending_ThrowsException() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder().messId("mess-302").build();
            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));
            when(messRepository.findById("mess-302")).thenReturn(Optional.of(pendingMess));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    subscriptionService.createSubscription(request, "user-101"));
            assertTrue(ex.getMessage().contains("PENDING"));
        }

        @Test
        @DisplayName("Should reject subscription when mess verification is REJECTED")
        void testCreateSubscription_MessRejected_ThrowsException() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder().messId("mess-303").build();
            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));
            when(messRepository.findById("mess-303")).thenReturn(Optional.of(rejectedMess));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    subscriptionService.createSubscription(request, "user-101"));
            assertTrue(ex.getMessage().contains("REJECTED"));
        }

        @Test
        @DisplayName("Should reject subscription when mess operational status is INACTIVE")
        void testCreateSubscription_MessInactive_ThrowsException() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder().messId("mess-304").build();
            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));
            when(messRepository.findById("mess-304")).thenReturn(Optional.of(inactiveMess));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    subscriptionService.createSubscription(request, "user-101"));
            assertTrue(ex.getMessage().contains("INACTIVE"));
        }

        @Test
        @DisplayName("Should reject duplicate ACTIVE subscription to the same mess")
        void testCreateSubscription_DuplicateActive_ThrowsException() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder().messId("mess-301").build();
            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));
            when(messRepository.findById("mess-301")).thenReturn(Optional.of(approvedActiveMess));

            Subscription existing = Subscription.builder()
                    .id("existing-sub")
                    .userId("user-101")
                    .messId("mess-301")
                    .status(SubscriptionStatus.ACTIVE)
                    .startDate(LocalDate.now().minusDays(5))
                    .endDate(LocalDate.now().plusDays(25))
                    .build();

            when(subscriptionRepository.findByUserIdAndMessIdAndStatus("user-101", "mess-301", SubscriptionStatus.ACTIVE))
                    .thenReturn(Optional.of(existing));

            DuplicateSubscriptionException ex = assertThrows(DuplicateSubscriptionException.class, () ->
                    subscriptionService.createSubscription(request, "user-101"));
            assertTrue(ex.getMessage().contains("already have an active subscription"));
        }

        @Test
        @DisplayName("Should allow new subscription if previous active subscription has expired")
        void testCreateSubscription_PriorExpiredSubscription_AllowsNew() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder().messId("mess-301").build();
            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));
            when(messRepository.findById("mess-301")).thenReturn(Optional.of(approvedActiveMess));

            Subscription expiredSub = Subscription.builder()
                    .id("expired-sub")
                    .userId("user-101")
                    .messId("mess-301")
                    .status(SubscriptionStatus.ACTIVE)
                    .startDate(LocalDate.now().minusDays(35))
                    .endDate(LocalDate.now().minusDays(5)) // past end date
                    .build();

            when(subscriptionRepository.findByUserIdAndMessIdAndStatus("user-101", "mess-301", SubscriptionStatus.ACTIVE))
                    .thenReturn(Optional.of(expiredSub));

            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
                Subscription sub = invocation.getArgument(0);
                if (sub.getId() == null) {
                    sub.setId("new-sub-505");
                }
                return sub;
            });

            SubscriptionResponse response = subscriptionService.createSubscription(request, "user-101");
            assertNotNull(response);
            assertEquals("new-sub-505", response.getId());
            assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
            assertEquals(SubscriptionStatus.EXPIRED, expiredSub.getStatus());
        }

        @Test
        @DisplayName("Should reject subscription when duration is non-positive")
        void testCreateSubscription_InvalidDuration_ThrowsException() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .messId("mess-301")
                    .durationDays(-10)
                    .build();

            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));
            when(messRepository.findById("mess-301")).thenReturn(Optional.of(approvedActiveMess));
            when(subscriptionRepository.findByUserIdAndMessIdAndStatus("user-101", "mess-301", SubscriptionStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    subscriptionService.createSubscription(request, "user-101"));
            assertTrue(ex.getMessage().contains("duration must be greater than 0"));
        }
    }

    @Nested
    @DisplayName("Get User Subscriptions Tests")
    class GetUserSubscriptionsTests {

        @Test
        @DisplayName("Should return all user subscriptions")
        void testGetUserSubscriptions_All_Success() {
            Subscription s1 = Subscription.builder()
                    .id("sub-1")
                    .userId("user-101")
                    .messId("mess-301")
                    .status(SubscriptionStatus.ACTIVE)
                    .endDate(LocalDate.now().plusDays(20))
                    .build();
            Subscription s2 = Subscription.builder()
                    .id("sub-2")
                    .userId("user-101")
                    .messId("mess-302")
                    .status(SubscriptionStatus.CANCELLED)
                    .build();

            when(subscriptionRepository.findByUserId("user-101")).thenReturn(List.of(s1, s2));

            List<SubscriptionResponse> results = subscriptionService.getUserSubscriptions("user-101", null);

            assertEquals(2, results.size());
            assertEquals("sub-1", results.get(0).getId());
            assertEquals("sub-2", results.get(1).getId());
        }

        @Test
        @DisplayName("Should filter user subscriptions by status")
        void testGetUserSubscriptions_WithStatusFilter_Success() {
            Subscription s1 = Subscription.builder()
                    .id("sub-1")
                    .userId("user-101")
                    .messId("mess-301")
                    .status(SubscriptionStatus.ACTIVE)
                    .endDate(LocalDate.now().plusDays(20))
                    .build();

            when(subscriptionRepository.findByUserIdAndStatus("user-101", SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(s1));

            List<SubscriptionResponse> results = subscriptionService.getUserSubscriptions("user-101", SubscriptionStatus.ACTIVE);

            assertEquals(1, results.size());
            assertEquals(SubscriptionStatus.ACTIVE, results.get(0).getStatus());
        }
    }

    @Nested
    @DisplayName("Get Subscription By ID Tests")
    class GetSubscriptionByIdTests {

        @Test
        @DisplayName("Subscriber can view their own subscription")
        void testGetSubscriptionById_Subscriber_Success() {
            Subscription sub = Subscription.builder()
                    .id("sub-1")
                    .userId("user-101")
                    .messId("mess-301")
                    .status(SubscriptionStatus.ACTIVE)
                    .endDate(LocalDate.now().plusDays(10))
                    .build();

            when(subscriptionRepository.findById("sub-1")).thenReturn(Optional.of(sub));
            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));

            SubscriptionResponse response = subscriptionService.getSubscriptionById("sub-1", "user-101", false);

            assertNotNull(response);
            assertEquals("sub-1", response.getId());
            assertEquals("user-101", response.getUserId());
        }

        @Test
        @DisplayName("Admin can view any user's subscription")
        void testGetSubscriptionById_Admin_Success() {
            Subscription sub = Subscription.builder()
                    .id("sub-1")
                    .userId("user-101")
                    .messId("mess-301")
                    .status(SubscriptionStatus.ACTIVE)
                    .endDate(LocalDate.now().plusDays(10))
                    .build();

            when(subscriptionRepository.findById("sub-1")).thenReturn(Optional.of(sub));
            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));

            SubscriptionResponse response = subscriptionService.getSubscriptionById("sub-1", "admin-999", true);

            assertNotNull(response);
            assertEquals("sub-1", response.getId());
        }

        @Test
        @DisplayName("Non-admin user cannot view another user's subscription (IDOR prevention)")
        void testGetSubscriptionById_OtherUser_ThrowsAccessDenied() {
            Subscription sub = Subscription.builder()
                    .id("sub-1")
                    .userId("user-101")
                    .messId("mess-301")
                    .status(SubscriptionStatus.ACTIVE)
                    .build();

            when(subscriptionRepository.findById("sub-1")).thenReturn(Optional.of(sub));

            assertThrows(AccessDeniedException.class, () ->
                    subscriptionService.getSubscriptionById("sub-1", "other-user-999", false));
        }

        @Test
        @DisplayName("Throws exception when subscription not found")
        void testGetSubscriptionById_NotFound_ThrowsException() {
            when(subscriptionRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(SubscriptionNotFoundException.class, () ->
                    subscriptionService.getSubscriptionById("non-existent", "user-101", false));
        }
    }

    @Nested
    @DisplayName("Cancel Subscription Tests")
    class CancelSubscriptionTests {

        @Test
        @DisplayName("Subscriber can successfully cancel their active subscription")
        void testCancelSubscription_Success() {
            Subscription sub = Subscription.builder()
                    .id("sub-1")
                    .userId("user-101")
                    .messId("mess-301")
                    .status(SubscriptionStatus.ACTIVE)
                    .endDate(LocalDate.now().plusDays(15))
                    .build();

            when(subscriptionRepository.findById("sub-1")).thenReturn(Optional.of(sub));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));
            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));

            SubscriptionResponse response = subscriptionService.cancelSubscription("sub-1", "user-101");

            assertNotNull(response);
            assertEquals(SubscriptionStatus.CANCELLED, response.getStatus());
            assertNotNull(response.getCancelledAt());
            verify(subscriptionRepository, times(1)).save(sub);
        }

        @Test
        @DisplayName("User cannot cancel another user's subscription")
        void testCancelSubscription_OtherUser_ThrowsAccessDenied() {
            Subscription sub = Subscription.builder()
                    .id("sub-1")
                    .userId("user-101")
                    .status(SubscriptionStatus.ACTIVE)
                    .build();

            when(subscriptionRepository.findById("sub-1")).thenReturn(Optional.of(sub));

            assertThrows(AccessDeniedException.class, () ->
                    subscriptionService.cancelSubscription("sub-1", "other-user"));
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cannot cancel already cancelled subscription")
        void testCancelSubscription_AlreadyCancelled_ThrowsIllegalState() {
            Subscription sub = Subscription.builder()
                    .id("sub-1")
                    .userId("user-101")
                    .status(SubscriptionStatus.CANCELLED)
                    .build();

            when(subscriptionRepository.findById("sub-1")).thenReturn(Optional.of(sub));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    subscriptionService.cancelSubscription("sub-1", "user-101"));
            assertTrue(ex.getMessage().contains("already cancelled"));
        }

        @Test
        @DisplayName("Cannot cancel already expired subscription")
        void testCancelSubscription_AlreadyExpired_ThrowsIllegalState() {
            Subscription sub = Subscription.builder()
                    .id("sub-1")
                    .userId("user-101")
                    .status(SubscriptionStatus.EXPIRED)
                    .endDate(LocalDate.now().minusDays(10))
                    .build();

            when(subscriptionRepository.findById("sub-1")).thenReturn(Optional.of(sub));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    subscriptionService.cancelSubscription("sub-1", "user-101"));
            assertTrue(ex.getMessage().contains("expired"));
        }

        @Test
        @DisplayName("Cancel non-existent subscription throws SubscriptionNotFoundException")
        void testCancelSubscription_NotFound_ThrowsException() {
            when(subscriptionRepository.findById("missing-id")).thenReturn(Optional.empty());

            assertThrows(SubscriptionNotFoundException.class, () ->
                    subscriptionService.cancelSubscription("missing-id", "user-101"));
        }
    }

    @Nested
    @DisplayName("Owner Mess Subscriptions Tests")
    class OwnerMessSubscriptionsTests {

        @Test
        @DisplayName("Owner can view subscriptions of their own mess with subscriber details")
        void testGetOwnerMessSubscriptions_Success() {
            Subscription sub = Subscription.builder()
                    .id("sub-1")
                    .userId("user-101")
                    .messId("mess-301")
                    .status(SubscriptionStatus.ACTIVE)
                    .endDate(LocalDate.now().plusDays(20))
                    .build();

            when(messRepository.findById("mess-301")).thenReturn(Optional.of(approvedActiveMess));
            when(subscriptionRepository.findByMessId("mess-301")).thenReturn(List.of(sub));
            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));

            List<SubscriptionResponse> results = subscriptionService.getOwnerMessSubscriptions("mess-301", "owner-201", null);

            assertEquals(1, results.size());
            SubscriptionResponse res = results.get(0);
            assertEquals("sub-1", res.getId());
            assertEquals("Aman Verma", res.getUserName());
            assertEquals("aman@example.com", res.getUserEmail());
            assertEquals("9876543210", res.getUserPhone());
        }

        @Test
        @DisplayName("Owner cannot view subscriptions of another owner's mess")
        void testGetOwnerMessSubscriptions_OtherOwner_ThrowsUnauthorized() {
            when(messRepository.findById("mess-301")).thenReturn(Optional.of(approvedActiveMess));

            assertThrows(UnauthorizedMessAccessException.class, () ->
                    subscriptionService.getOwnerMessSubscriptions("mess-301", "different-owner", null));
        }

        @Test
        @DisplayName("Owner fetching subscriptions for non-existent mess throws MessNotFoundException")
        void testGetOwnerMessSubscriptions_MessNotFound_ThrowsException() {
            when(messRepository.findById("missing-mess")).thenReturn(Optional.empty());

            assertThrows(MessNotFoundException.class, () ->
                    subscriptionService.getOwnerMessSubscriptions("missing-mess", "owner-201", null));
        }
    }

    @Nested
    @DisplayName("Admin Subscriptions Tests")
    class AdminSubscriptionsTests {

        @Test
        @DisplayName("Admin can view all platform subscriptions")
        void testGetAllSubscriptions_Success() {
            Subscription s1 = Subscription.builder()
                    .id("sub-1")
                    .userId("user-101")
                    .messId("mess-301")
                    .status(SubscriptionStatus.ACTIVE)
                    .endDate(LocalDate.now().plusDays(10))
                    .build();

            when(subscriptionRepository.findAll()).thenReturn(List.of(s1));
            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));

            List<SubscriptionResponse> results = subscriptionService.getAllSubscriptions(null);

            assertEquals(1, results.size());
            assertEquals("sub-1", results.get(0).getId());
            assertEquals("Aman Verma", results.get(0).getUserName());
        }

        @Test
        @DisplayName("Admin can filter platform subscriptions by status")
        void testGetAllSubscriptions_WithStatusFilter_Success() {
            Subscription s1 = Subscription.builder()
                    .id("sub-1")
                    .userId("user-101")
                    .messId("mess-301")
                    .status(SubscriptionStatus.ACTIVE)
                    .endDate(LocalDate.now().plusDays(10))
                    .build();

            when(subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)).thenReturn(List.of(s1));
            when(userRepository.findById("user-101")).thenReturn(Optional.of(activeUser));

            List<SubscriptionResponse> results = subscriptionService.getAllSubscriptions(SubscriptionStatus.ACTIVE);

            assertEquals(1, results.size());
            assertEquals(SubscriptionStatus.ACTIVE, results.get(0).getStatus());
        }
    }
}

