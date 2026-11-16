package com.MessLoc.service.impl;

import com.MessLoc.dto.subscription.CreateSubscriptionRequest;
import com.MessLoc.dto.subscription.SubscriptionResponse;
import com.MessLoc.entity.Mess;
import com.MessLoc.entity.Subscription;
import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.MessStatus;
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
import com.MessLoc.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    private final SubscriptionRepository subscriptionRepository;
    private final MessRepository messRepository;
    private final UserRepository userRepository;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository,
                                   MessRepository messRepository,
                                   UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.messRepository = messRepository;
        this.userRepository = userRepository;
    }

    @Override
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request, String userId) {
        log.info("Creating subscription for user ID: {} and mess ID: {}", userId, request.getMessId());

        // 1. Validate User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getStatus() == AccountStatus.BLOCKED) {
            log.warn("Blocked user '{}' attempted to create a subscription", userId);
            throw new AccountBlockedException("Your account is blocked. You cannot create subscriptions.");
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            log.warn("Inactive user '{}' attempted to create a subscription", userId);
            throw new AccountInactiveException("Your account is inactive. Please contact support.");
        }

        // 2. Validate Mess
        Mess mess = messRepository.findById(request.getMessId())
                .orElseThrow(() -> new MessNotFoundException("Mess not found with ID: " + request.getMessId()));

        if (mess.getVerificationStatus() != VerificationStatus.APPROVED) {
            log.warn("Subscription rejected: Mess '{}' verification status is {}", mess.getId(), mess.getVerificationStatus());
            throw new IllegalArgumentException("Cannot subscribe to mess. Mess verification status is " + mess.getVerificationStatus() + ". Only APPROVED messes can accept subscriptions.");
        }

        if (mess.getStatus() != MessStatus.ACTIVE) {
            log.warn("Subscription rejected: Mess '{}' operational status is {}", mess.getId(), mess.getStatus());
            throw new IllegalArgumentException("Cannot subscribe to mess. Mess operational status is " + mess.getStatus() + ". Only ACTIVE messes can accept subscriptions.");
        }

        // 3. Duplicate Active Subscription Check
        Optional<Subscription> existingActiveOpt = subscriptionRepository.findByUserIdAndMessIdAndStatus(
                userId, mess.getId(), SubscriptionStatus.ACTIVE);

        if (existingActiveOpt.isPresent()) {
            Subscription existing = existingActiveOpt.get();
            // Check if it has expired based on end date
            if (existing.getEndDate() != null && existing.getEndDate().isBefore(LocalDate.now())) {
                existing.setStatus(SubscriptionStatus.EXPIRED);
                existing.setUpdatedAt(Instant.now());
                subscriptionRepository.save(existing);
                log.info("Marked prior expired subscription '{}' as EXPIRED", existing.getId());
            } else {
                log.warn("Duplicate active subscription rejected for user '{}' on mess '{}'", userId, mess.getId());
                throw new DuplicateSubscriptionException("You already have an active subscription to mess: " + mess.getName());
            }
        }

        // 4. Resolve Plan Details & Duration
        int durationDays = 30;
        String planId = "monthly";
        String planName = "Monthly Plan";
        double baseMonthlyPrice = (mess.getMonthlyPrice() != null) ? mess.getMonthlyPrice() : 0.0;
        double price = baseMonthlyPrice;

        if (request.getPlanId() != null && !request.getPlanId().isBlank()) {
            String pId = request.getPlanId().trim().toLowerCase();
            if ("quarterly".equals(pId)) {
                planId = "quarterly";
                planName = "Quarterly Plan";
                durationDays = 90;
                price = baseMonthlyPrice * 3;
            } else if ("half-yearly".equals(pId) || "half_yearly".equals(pId)) {
                planId = "half-yearly";
                planName = "Half-Yearly Plan";
                durationDays = 180;
                price = baseMonthlyPrice * 6;
            } else if ("yearly".equals(pId) || "annual".equals(pId)) {
                planId = "yearly";
                planName = "Yearly Plan";
                durationDays = 365;
                price = baseMonthlyPrice * 12;
            } else {
                planId = pId;
                if (request.getPlanName() != null && !request.getPlanName().isBlank()) {
                    planName = request.getPlanName().trim();
                }
                if (request.getDurationDays() != null) {
                    durationDays = request.getDurationDays();
                }
            }
        } else {
            if (request.getPlanName() != null && !request.getPlanName().isBlank()) {
                planName = request.getPlanName().trim();
            }
            if (request.getDurationDays() != null) {
                durationDays = request.getDurationDays();
            }
        }

        if (durationDays <= 0) {
            throw new IllegalArgumentException("Subscription duration must be greater than 0 days");
        }

        // 5. Generate Dates (Backend-generated, never trusted from client)
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(durationDays);
        Instant now = Instant.now();

        // 6. Build and persist subscription
        Subscription subscription = Subscription.builder()
                .userId(userId)
                .messId(mess.getId())
                .messName(mess.getName())
                .planId(planId)
                .planName(planName)
                .durationDays(durationDays)
                .price(price)
                .startDate(startDate)
                .endDate(endDate)
                .status(SubscriptionStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription '{}' successfully created for user '{}' to mess '{}'", saved.getId(), userId, mess.getId());

        return SubscriptionResponse.fromEntityWithUser(saved, user);
    }

    @Override
    public List<SubscriptionResponse> getUserSubscriptions(String userId, SubscriptionStatus status) {
        log.info("Fetching subscriptions for user: {}, status filter: {}", userId, status);

        List<Subscription> subscriptions;
        if (status != null) {
            subscriptions = subscriptionRepository.findByUserIdAndStatus(userId, status);
        } else {
            subscriptions = subscriptionRepository.findByUserId(userId);
        }

        LocalDate today = LocalDate.now();
        List<SubscriptionResponse> responseList = new ArrayList<>();

        for (Subscription sub : subscriptions) {
            // Auto-expire active subscriptions whose end date is past
            if (sub.getStatus() == SubscriptionStatus.ACTIVE && sub.getEndDate() != null && sub.getEndDate().isBefore(today)) {
                sub.setStatus(SubscriptionStatus.EXPIRED);
                sub.setUpdatedAt(Instant.now());
                subscriptionRepository.save(sub);
            }
            responseList.add(SubscriptionResponse.fromEntity(sub));
        }

        return responseList;
    }

    @Override
    public SubscriptionResponse getSubscriptionById(String subscriptionId, String currentUserId, boolean isAdmin) {
        log.info("Fetching subscription '{}' for user '{}' (isAdmin: {})", subscriptionId, currentUserId, isAdmin);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with ID: " + subscriptionId));

        // Enforce ownership: Non-admin users can view only their own subscriptions
        if (!isAdmin && !subscription.getUserId().equals(currentUserId)) {
            log.warn("Access denied: User '{}' attempted to view subscription '{}' belonging to '{}'",
                    currentUserId, subscriptionId, subscription.getUserId());
            throw new AccessDeniedException("Access denied: You do not have permission to view this subscription");
        }

        // Auto-expire check
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE &&
                subscription.getEndDate() != null &&
                subscription.getEndDate().isBefore(LocalDate.now())) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscription.setUpdatedAt(Instant.now());
            subscription = subscriptionRepository.save(subscription);
        }

        User subscriber = userRepository.findById(subscription.getUserId()).orElse(null);
        return SubscriptionResponse.fromEntityWithUser(subscription, subscriber);
    }

    @Override
    public SubscriptionResponse cancelSubscription(String subscriptionId, String userId) {
        log.info("User '{}' requested cancellation of subscription '{}'", userId, subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with ID: " + subscriptionId));

        // Enforce ownership
        if (!subscription.getUserId().equals(userId)) {
            log.warn("Cancellation access denied: User '{}' does not own subscription '{}'", userId, subscriptionId);
            throw new AccessDeniedException("Access denied: You cannot cancel another user's subscription");
        }

        // Check if already cancelled or expired
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Subscription is already cancelled");
        }

        if (subscription.getStatus() == SubscriptionStatus.EXPIRED ||
                (subscription.getEndDate() != null && subscription.getEndDate().isBefore(LocalDate.now()))) {
            if (subscription.getStatus() != SubscriptionStatus.EXPIRED) {
                subscription.setStatus(SubscriptionStatus.EXPIRED);
                subscription.setUpdatedAt(Instant.now());
                subscriptionRepository.save(subscription);
            }
            throw new IllegalStateException("Subscription has already expired and cannot be cancelled");
        }

        // Cancel the subscription
        Instant now = Instant.now();
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(now);
        subscription.setUpdatedAt(now);

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription '{}' successfully cancelled for user '{}'", subscriptionId, userId);

        User subscriber = userRepository.findById(userId).orElse(null);
        return SubscriptionResponse.fromEntityWithUser(saved, subscriber);
    }

    @Override
    public List<SubscriptionResponse> getOwnerMessSubscriptions(String messId, String ownerId, SubscriptionStatus status) {
        log.info("Owner '{}' fetching subscriptions for mess '{}', status filter: {}", ownerId, messId, status);

        // 1. Verify mess exists
        Mess mess = messRepository.findById(messId)
                .orElseThrow(() -> new MessNotFoundException("Mess not found with ID: " + messId));

        // 2. Verify ownership in service layer (defense-in-depth)
        if (!mess.getOwnerId().equals(ownerId)) {
            log.warn("Unauthorized mess access: Owner '{}' does not own mess '{}'", ownerId, messId);
            throw new UnauthorizedMessAccessException("You are not authorized to view subscriptions for this mess");
        }

        // 3. Query subscriptions
        List<Subscription> subscriptions;
        if (status != null) {
            subscriptions = subscriptionRepository.findByMessIdAndStatus(messId, status);
        } else {
            subscriptions = subscriptionRepository.findByMessId(messId);
        }

        // 4. Batch fetch subscriber user details for performance
        Map<String, User> userCache = new HashMap<>();
        List<SubscriptionResponse> responseList = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Subscription sub : subscriptions) {
            // Auto-expire check
            if (sub.getStatus() == SubscriptionStatus.ACTIVE && sub.getEndDate() != null && sub.getEndDate().isBefore(today)) {
                sub.setStatus(SubscriptionStatus.EXPIRED);
                sub.setUpdatedAt(Instant.now());
                subscriptionRepository.save(sub);
            }

            User user = userCache.get(sub.getUserId());
            if (user == null && sub.getUserId() != null) {
                user = userRepository.findById(sub.getUserId()).orElse(null);
                if (user != null) {
                    userCache.put(sub.getUserId(), user);
                }
            }

            responseList.add(SubscriptionResponse.fromEntityWithUser(sub, user));
        }

        return responseList;
    }

    @Override
    public List<SubscriptionResponse> getAllSubscriptions(SubscriptionStatus status) {
        log.info("Admin fetching all platform subscriptions, status filter: {}", status);

        List<Subscription> subscriptions;
        if (status != null) {
            subscriptions = subscriptionRepository.findByStatus(status);
        } else {
            subscriptions = subscriptionRepository.findAll();
        }

        Map<String, User> userCache = new HashMap<>();
        List<SubscriptionResponse> responseList = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Subscription sub : subscriptions) {
            // Auto-expire check
            if (sub.getStatus() == SubscriptionStatus.ACTIVE && sub.getEndDate() != null && sub.getEndDate().isBefore(today)) {
                sub.setStatus(SubscriptionStatus.EXPIRED);
                sub.setUpdatedAt(Instant.now());
                subscriptionRepository.save(sub);
            }

            User user = userCache.get(sub.getUserId());
            if (user == null && sub.getUserId() != null) {
                user = userRepository.findById(sub.getUserId()).orElse(null);
                if (user != null) {
                    userCache.put(sub.getUserId(), user);
                }
            }

            responseList.add(SubscriptionResponse.fromEntityWithUser(sub, user));
        }

        return responseList;
    }
}

