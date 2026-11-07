package com.MessLoc.service.impl;

import com.MessLoc.dto.request.UpdateUserProfileRequest;
import com.MessLoc.dto.response.UserProfileResponse;
import com.MessLoc.entity.User;
import com.MessLoc.exception.ResourceNotFoundException;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserProfileResponse getAuthenticatedUserProfile(String email) {
        String normalizedEmail = email != null ? email.toLowerCase().trim() : "";
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for email: " + normalizedEmail));

        log.debug("Retrieved profile for user '{}'", normalizedEmail);
        return UserProfileResponse.fromEntity(user);
    }

    @Override
    public UserProfileResponse updateAuthenticatedUserProfile(String email, UpdateUserProfileRequest request) {
        String normalizedEmail = email != null ? email.toLowerCase().trim() : "";
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for email: " + normalizedEmail));

        boolean updated = false;

        if (request != null) {
            if (request.getName() != null) {
                String trimmedName = request.getName().trim();
                if (trimmedName.isEmpty()) {
                    throw new IllegalArgumentException("Name cannot be blank");
                }
                user.setName(trimmedName);
                updated = true;
            }

            if (request.getPhone() != null) {
                String trimmedPhone = request.getPhone().trim();
                if (trimmedPhone.isEmpty()) {
                    throw new IllegalArgumentException("Phone number cannot be blank");
                }
                user.setPhone(trimmedPhone);
                updated = true;
            }

            if (request.getCity() != null) {
                String trimmedCity = request.getCity().trim();
                user.setCity(trimmedCity.isEmpty() ? null : trimmedCity);
                updated = true;
            }

            if (request.getProfileImage() != null) {
                String trimmedImage = request.getProfileImage().trim();
                user.setProfileImage(trimmedImage.isEmpty() ? null : trimmedImage);
                updated = true;
            }
        }

        User savedUser = updated ? userRepository.save(user) : user;
        log.info("Profile updated successfully for user '{}'", normalizedEmail);
        return UserProfileResponse.fromEntity(savedUser);
    }
}
