package com.MessLoc.service.impl;

import com.MessLoc.dto.mess.CreateMessRequest;
import com.MessLoc.dto.mess.MessResponse;
import com.MessLoc.dto.mess.PublicMessResponse;
import com.MessLoc.dto.mess.UpdateMessRequest;
import com.MessLoc.entity.Mess;
import com.MessLoc.enums.MessStatus;
import com.MessLoc.enums.VerificationStatus;
import com.MessLoc.exception.MessNotFoundException;
import com.MessLoc.exception.UnauthorizedMessAccessException;
import com.MessLoc.repository.MessRepository;
import com.MessLoc.service.MessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessServiceImpl implements MessService {

    private final MessRepository messRepository;

    @Override
    public MessResponse createMess(CreateMessRequest request, String ownerId) {
        validateCoordinates(request.getLatitude(), request.getLongitude());
        validateTime(request.getOpeningTime(), request.getClosingTime());

        Mess mess = Mess.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .contactNumber(request.getContactNumber())
                .monthlyPrice(request.getMonthlyPrice())
                .foodType(request.getFoodType())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .facilities(request.getFacilities() != null ? request.getFacilities() : List.of())
                .verificationStatus(VerificationStatus.PENDING)
                .status(MessStatus.ACTIVE)
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

        mess.setName(request.getName());
        mess.setDescription(request.getDescription());
        mess.setAddress(request.getAddress());
        mess.setCity(request.getCity());
        mess.setState(request.getState());
        mess.setPincode(request.getPincode());
        mess.setLatitude(request.getLatitude());
        mess.setLongitude(request.getLongitude());
        mess.setContactNumber(request.getContactNumber());
        mess.setMonthlyPrice(request.getMonthlyPrice());
        mess.setFoodType(request.getFoodType());
        mess.setOpeningTime(request.getOpeningTime());
        mess.setClosingTime(request.getClosingTime());
        mess.setFacilities(request.getFacilities() != null ? request.getFacilities() : List.of());
        mess.setStatus(request.getStatus());

        Mess updatedMess = messRepository.save(mess);
        return mapToResponse(updatedMess);
    }

    @Override
    public void deactivateMess(String id, String ownerId) {
        Mess mess = getMessOwnedByOwner(id, ownerId);
        mess.setStatus(MessStatus.INACTIVE);
        messRepository.save(mess);
    }

    @Override
    public void activateMess(String id, String ownerId) {
        Mess mess = getMessOwnedByOwner(id, ownerId);
        mess.setStatus(MessStatus.ACTIVE);
        messRepository.save(mess);
    }

    @Override
    public void deleteMess(String id, String ownerId) {
        // Soft delete
        deactivateMess(id, ownerId);
    }

    @Override
    public List<PublicMessResponse> getPublicMesses() {
        return messRepository.findByVerificationStatusAndStatus(VerificationStatus.APPROVED, MessStatus.ACTIVE)
                .stream()
                .map(this::mapToPublicResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PublicMessResponse getPublicMess(String id) {
        Mess mess = messRepository.findById(id)
                .orElseThrow(() -> new MessNotFoundException("Mess not found with id: " + id));

        if (mess.getVerificationStatus() != VerificationStatus.APPROVED || mess.getStatus() != MessStatus.ACTIVE) {
            throw new MessNotFoundException("Mess is not publicly available");
        }

        return mapToPublicResponse(mess);
    }

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
        messRepository.save(mess);
    }

    @Override
    public void rejectMess(String id) {
        Mess mess = messRepository.findById(id)
                .orElseThrow(() -> new MessNotFoundException("Mess not found with id: " + id));
        mess.setVerificationStatus(VerificationStatus.REJECTED);
        messRepository.save(mess);
    }

    private Mess getMessOwnedByOwner(String messId, String ownerId) {
        Mess mess = messRepository.findById(messId)
                .orElseThrow(() -> new MessNotFoundException("Mess not found with id: " + messId));

        if (!mess.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedMessAccessException("You are not authorized to manage this mess");
        }

        return mess;
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private void validateTime(java.time.LocalTime openingTime, java.time.LocalTime closingTime) {
        // Only validate if assumed same day
        // For now, if openingTime >= closingTime, we throw
        // This is assuming same-day opening/closing
        if (openingTime != null && closingTime != null && !openingTime.isBefore(closingTime)) {
            throw new IllegalArgumentException("Opening time must be before closing time");
        }
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
}
