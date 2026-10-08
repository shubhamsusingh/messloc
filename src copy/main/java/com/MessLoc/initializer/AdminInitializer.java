package com.MessLoc.initializer;

import com.MessLoc.entity.User;
import com.MessLoc.enums.AccountStatus;
import com.MessLoc.enums.Role;
import com.MessLoc.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${messmate.admin.email}")
    private String adminEmail;

    @Value("${messmate.admin.password}")
    private String adminPassword;

    @Value("${messmate.admin.name}")
    private String adminName;

    @Value("${messmate.admin.phone}")
    private String adminPhone;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            if (!userRepository.existsByRole(Role.ADMIN)) {
                String normalizedEmail = adminEmail.toLowerCase().trim();

                User admin = User.builder()
                        .name(adminName)
                        .email(normalizedEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .phone(adminPhone)
                        .role(Role.ADMIN)
                        .status(AccountStatus.ACTIVE)
                        .build();

                userRepository.save(admin);
                log.info("==========================================================");
                log.info("MessMate: Initial ADMIN account successfully seeded!");
                log.info("Email: {}", normalizedEmail);
                log.info("Role: ADMIN | Status: ACTIVE");
                log.info("==========================================================");
            } else {
                log.info("MessMate: An ADMIN account already exists in database. Skipping seed.");
            }
        } catch (Exception ex) {
            log.warn("AdminInitializer encountered an issue during startup (e.g. database not reachable yet): {}", ex.getMessage());
        }
    }
}
