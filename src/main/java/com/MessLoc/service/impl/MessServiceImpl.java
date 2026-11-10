package com.MessLoc.service.impl;

import com.MessLoc.dto.mess.CreateMessRequest;
import com.MessLoc.dto.mess.MessDetailsResponse;
import com.MessLoc.dto.mess.MessResponse;
import com.MessLoc.dto.mess.PublicMessResponse;
import com.MessLoc.dto.mess.UpdateMessRequest;
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
import com.MessLoc.exception.UnauthorizedMessAccessException;
import com.MessLoc.repository.MessRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.service.MessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessServiceImpl implements MessService {

    private final MessRepository messRepository;
    private final UserRepository userRepository;

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public MessResponse createMess(CreateMessRequest request, String ownerId) {
        if (ownerId != null) {
            userRepository.findById(ownerId).ifPresent(owner -> {
                if (owner.getRole() != Role.OWNER) {
                    throw new AccessDeniedException("Only OWNER accounts can create mess listings");
                }
                if (owner.getStatus() == AccountStatus.PENDING) {
                    throw new OwnerPendingException("Your account is pending admin approval. Please wait for verification.");
                }
                if (owner.getStatus() != AccountStatus.ACTIVE) {
                    throw new AccountInactiveException("Your account is currently inactive. Please contact MessLoc support.");
                }
            });
        }

        validateCoordinates(request.getLatitude(), request.getLongitude());
        validateTime(request.getOpeningTime(), request.getClosingTime());

        Instant now = Instant.now();
        Mess mess = Mess.builder()
                .ownerId(ownerId)
                .name(request.getName().trim())
                .description(request.getDescription().trim())
                .address(request.getAddress().trim())
                .city(request.getCity().trim())
                .state(request.getState().trim())
                .pincode(request.getPincode().trim())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .contactNumber(request.getContactNumber().trim())
                .monthlyPrice(request.getMonthlyPrice())
                .foodType(request.getFoodType())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .facilities(request.getFacilities() != null ? request.getFacilities() : List.of())
                .verificationStatus(VerificationStatus.PENDING)
                .status(MessStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Mess savedMess = messRepository.save(mess);
        return mapToResponse(savedMess);
    }

    @Override
    public List<MessResponse> getOwnerMesses(String ownerId) {
        return messRepository.findByOwnerId(ownerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MessResponse getOwnerMess(String id, String ownerId) {
        Mess mess = getMessOwnedByOwner(id, ownerId);
        return mapToResponse(mess);
    }

    @Override
    public MessResponse updateMess(String id, UpdateMessRequest request, String ownerId) {
        Mess mess = getMessOwnedByOwner(id, ownerId);
        
        validateCoordinates(request.getLatitude(), request.getLongitude());
        validateTime(request.getOpeningTime(), request.getClosingTime());

        mess.setName(request.getName().trim());
        mess.setDescription(request.getDescription().trim());
        mess.setAddress(request.getAddress().trim());
        mess.setCity(request.getCity().trim());
        mess.setState(request.getState().trim());
        mess.setPincode(request.getPincode().trim());
        mess.setLatitude(request.getLatitude());
        mess.setLongitude(request.getLongitude());
        mess.setContactNumber(request.getContactNumber().trim());
        mess.setMonthlyPrice(request.getMonthlyPrice());
        mess.setFoodType(request.getFoodType());
        mess.setOpeningTime(request.getOpeningTime());
        mess.setClosingTime(request.getClosingTime());
        if (request.getFacilities() != null) {
            mess.setFacilities(request.getFacilities());
        }
        if (request.getStatus() != null) {
            mess.setStatus(request.getStatus());
        }
        mess.setUpdatedAt(Instant.now());

        Mess updatedMess = messRepository.save(mess);
        return mapToResponse(updatedMess);
    }

    @Override
    public void deactivateMess(String id, String ownerId) {
        Mess mess = getMessOwnedByOwner(id, ownerId);
        mess.setStatus(MessStatus.INACTIVE);
        mess.setUpdatedAt(Instant.now());
        messRepository.save(mess);
    }

    @Override
    public void activateMess(String id, String ownerId) {
        Mess mess = getMessOwnedByOwner(id, ownerId);
        mess.setStatus(MessStatus.ACTIVE);
        mess.setUpdatedAt(Instant.now());
        messRepository.save(mess);
    }

    @Override
    public void deleteMess(String id, String ownerId) {
        Mess mess = getMessOwnedByOwner(id, ownerId);
        messRepository.delete(mess);
    }

    // ==========================================
    // PUBLIC DISCOVERY OPERATIONS
    // ==========================================

    @Override
    public List<PublicMessResponse> getPublicMesses() {
        return getPublicMesses(null, null);
    }

    @Override
    public List<PublicMessResponse> getPublicMesses(String city) {
        return getPublicMesses(city, null);
    }

    @Override
    public List<PublicMessResponse> getPublicMesses(String city, FoodType foodType) {
        if (city != null && city.trim().isEmpty()) {
            throw new IllegalArgumentException("City search parameter cannot be blank");
        }

        String trimmedCity = (city != null) ? city.trim() : null;
        List<FoodType> targetFoodTypes = resolveTargetFoodTypes(foodType);

        List<Mess> messes;
        if (trimmedCity != null && targetFoodTypes != null) {
            messes = messRepository.findByVerificationStatusAndStatusAndCityIgnoreCaseAndFoodTypeIn(
                    VerificationStatus.APPROVED, MessStatus.ACTIVE, trimmedCity, targetFoodTypes);
        } else if (trimmedCity != null) {
            messes = messRepository.findByVerificationStatusAndStatusAndCityIgnoreCase(
                    VerificationStatus.APPROVED, MessStatus.ACTIVE, trimmedCity);
        } else if (targetFoodTypes != null) {
            messes = messRepository.findByVerificationStatusAndStatusAndFoodTypeIn(
                    VerificationStatus.APPROVED, MessStatus.ACTIVE, targetFoodTypes);
        } else {
            messes = messRepository.findByVerificationStatusAndStatus(
                    VerificationStatus.APPROVED, MessStatus.ACTIVE);
        }

        return messes.stream()
                .map(this::mapToPublicResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PublicMessResponse> getNearbyMesses(Double latitude, Double longitude, Double radius) {
        return getNearbyMesses(latitude, longitude, radius, null);
    }

    @Override
    public List<PublicMessResponse> getNearbyMesses(Double latitude, Double longitude, Double radius, FoodType foodType) {
        if (latitude == null) {
            throw new IllegalArgumentException("Latitude is required");
        }
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude == null) {
            throw new IllegalArgumentException("Longitude is required");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        if (radius == null) {
            radius = 5.0;
        }
        if (radius <= 0.0) {
            throw new IllegalArgumentException("Radius must be greater than 0");
        }
        if (radius > 100.0) {
            throw new IllegalArgumentException("Radius cannot exceed 100 km");
        }

        List<FoodType> targetFoodTypes = resolveTargetFoodTypes(foodType);
        List<Mess> candidates;
        if (targetFoodTypes != null) {
            candidates = messRepository.findByVerificationStatusAndStatusAndFoodTypeIn(
                    VerificationStatus.APPROVED, MessStatus.ACTIVE, targetFoodTypes);
        } else {
            candidates = messRepository.findByVerificationStatusAndStatus(
                    VerificationStatus.APPROVED, MessStatus.ACTIVE);
        }

        final double searchRadius = radius;
        return candidates.stream()
                .filter(m -> m.getLatitude() != null && m.getLongitude() != null)
                .map(m -> {
                    double dist = calculateHaversineDistanceKm(latitude, longitude, m.getLatitude(), m.getLongitude());
                    double roundedDist = Math.round(dist * 100.0) / 100.0;
                    PublicMessResponse resp = mapToPublicResponse(m);
                    resp.setDistanceInKm(roundedDist);
                    return resp;
                })
                .filter(resp -> resp.getDistanceInKm() <= searchRadius)
                .sorted(Comparator.comparingDouble(PublicMessResponse::getDistanceInKm))
                .collect(Collectors.toList());
    }

    @Override
    public MessDetailsResponse getPublicMess(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Mess ID cannot be blank");
        }

        Mess mess = messRepository.findById(id.trim())
                .orElseThrow(() -> new MessNotFoundException("Mess not found with id: " + id));

        if (mess.getVerificationStatus() != VerificationStatus.APPROVED || mess.getStatus() != MessStatus.ACTIVE) {
            throw new MessNotFoundException("Mess is not publicly available or pending verification");
        }

        return mapToMessDetailsResponse(mess);
    }

    // ==========================================
    // ADMIN OPERATIONS
    // ==========================================

    @Override
    public List<MessResponse> getAllMesses() {
        return messRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MessResponse> getPendingMesses() {
        return messRepository.findByVerificationStatus(VerificationStatus.PENDING).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void approveMess(String id) {
        Mess mess = messRepository.findById(id)
                .orElseThrow(() -> new MessNotFoundException("Mess not found with id: " + id));
        mess.setVerificationStatus(VerificationStatus.APPROVED);
        mess.setUpdatedAt(Instant.now());
        messRepository.save(mess);
    }

    @Override
    public void rejectMess(String id) {
        Mess mess = messRepository.findById(id)
                .orElseThrow(() -> new MessNotFoundException("Mess not found with id: " + id));
        mess.setVerificationStatus(VerificationStatus.REJECTED);
        mess.setUpdatedAt(Instant.now());
        messRepository.save(mess);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private Mess getMessOwnedByOwner(String messId, String ownerId) {
        Mess mess = messRepository.findById(messId)
                .orElseThrow(() -> new MessNotFoundException("Mess not found with id: " + messId));

        if (!mess.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedMessAccessException("You are not authorized to manage this mess");
        }

        return mess;
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private void validateTime(LocalTime openingTime, LocalTime closingTime) {
        if (openingTime != null && closingTime != null) {
            if (openingTime.equals(closingTime)) {
                throw new IllegalArgumentException("Opening time and closing time cannot be the exact same time");
            }
            if (openingTime.isAfter(closingTime)) {
                throw new IllegalArgumentException("Opening time must be before closing time");
            }
        }
    }

    private List<FoodType> resolveTargetFoodTypes(FoodType foodType) {
        if (foodType == null) {
            return null;
        }
        return switch (foodType) {
            case VEG -> List.of(FoodType.VEG, FoodType.BOTH);
            case NON_VEG -> List.of(FoodType.NON_VEG, FoodType.BOTH);
            case BOTH -> List.of(FoodType.BOTH);
        };
    }

    private double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double originLatRad = Math.toRadians(lat1);
        double targetLatRad = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(originLatRad) * Math.cos(targetLatRad) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    private MessResponse mapToResponse(Mess mess) {
        return MessResponse.builder()
                .id(mess.getId())
                .ownerId(mess.getOwnerId())
                .name(mess.getName())
                .description(mess.getDescription())
                .address(mess.getAddress())
                .city(mess.getCity())
                .state(mess.getState())
                .pincode(mess.getPincode())
                .latitude(mess.getLatitude())
                .longitude(mess.getLongitude())
                .contactNumber(mess.getContactNumber())
                .monthlyPrice(mess.getMonthlyPrice())
                .foodType(mess.getFoodType())
                .openingTime(mess.getOpeningTime())
                .closingTime(mess.getClosingTime())
                .facilities(mess.getFacilities())
                .verificationStatus(mess.getVerificationStatus())
                .status(mess.getStatus())
                .createdAt(mess.getCreatedAt())
                .updatedAt(mess.getUpdatedAt())
                .build();
    }

    private PublicMessResponse mapToPublicResponse(Mess mess) {
        return PublicMessResponse.builder()
                .id(mess.getId())
                .name(mess.getName())
                .description(mess.getDescription())
                .address(mess.getAddress())
                .city(mess.getCity())
                .state(mess.getState())
                .pincode(mess.getPincode())
                .latitude(mess.getLatitude())
                .longitude(mess.getLongitude())
                .contactNumber(mess.getContactNumber())
                .monthlyPrice(mess.getMonthlyPrice())
                .foodType(mess.getFoodType())
                .openingTime(mess.getOpeningTime())
                .closingTime(mess.getClosingTime())
                .facilities(mess.getFacilities())
                .build();
    }

    private MessDetailsResponse mapToMessDetailsResponse(Mess mess) {
        return MessDetailsResponse.builder()
                .id(mess.getId())
                .name(mess.getName())
                .description(mess.getDescription())
                .address(mess.getAddress())
                .city(mess.getCity())
                .state(mess.getState())
                .pincode(mess.getPincode())
                .latitude(mess.getLatitude())
                .longitude(mess.getLongitude())
                .contactNumber(mess.getContactNumber())
                .monthlyPrice(mess.getMonthlyPrice())
                .foodType(mess.getFoodType())
                .openingTime(mess.getOpeningTime())
                .closingTime(mess.getClosingTime())
                .facilities(mess.getFacilities())
                .build();
    }
}
