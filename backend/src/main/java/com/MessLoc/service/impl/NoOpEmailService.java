package com.MessLoc.service.impl;

import com.MessLoc.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default fallback implementation of {@link EmailService}.
 * Logs transactional email dispatch without performing actual SMTP network transmission.
 *
 * <p>To enable live SMTP delivery in production:
 * <ul>
 *   <li>Add {@code spring-boot-starter-mail} dependency to Maven.</li>
 *   <li>Configure {@code MAIL_HOST}, {@code MAIL_PORT}, {@code MAIL_USERNAME},
 *       {@code MAIL_PASSWORD}, and {@code MAIL_FROM} environment variables.</li>
 *   <li>Provide an {@code SmtpEmailService} bean annotated with {@code @Primary}.</li>
 * </ul>
 * </p>
 */
@Service
public class NoOpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailService.class);

    @Override
    public void sendPasswordResetEmail(String toEmail, String userName, String resetLink) {
        log.info("Transactional Email: Password reset link generated for '{}' (user: '{}'). [SMTP not configured; link delivery mocked in logs]",
                toEmail, userName);
    }
}

