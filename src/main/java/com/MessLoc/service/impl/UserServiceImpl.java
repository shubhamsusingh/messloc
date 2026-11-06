package com.MessLoc.service.impl;

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
        String normalizedEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for email: " + normalizedEmail));

        log.debug("Retrieved profile for user '{}'", normalizedEmail);
        return UserProfileResponse.fromEntity(user);
    }
}

