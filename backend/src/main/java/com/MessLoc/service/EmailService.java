package com.MessLoc.service;

/**
 * Contract for outbound transactional email delivery in MessLoc.
 *
 * <p>The default implementation ({@link com.MessLoc.service.impl.NoOpEmailService})
 * logs but does not send actual emails. To enable real email delivery:
 * <ol>
 *   <li>Add {@code spring-boot-starter-mail} to {@code pom.xml}.</li>
 *   <li>Set the following environment variables:
 *     <ul>
 *       <li>{@code MAIL_HOST} – SMTP server hostname (e.g., smtp.gmail.com)</li>
 *       <li>{@code MAIL_PORT} – SMTP port (e.g., 587)</li>
 *       <li>{@code MAIL_USERNAME} – SMTP username / email address</li>
 *       <li>{@code MAIL_PASSWORD} – SMTP password or application password</li>
 *       <li>{@code MAIL_FROM} – Sender address shown to recipients</li>
 *       <li>{@code APP_FRONTEND_BASE_URL} – Base URL for constructing reset links</li>
 *     </ul>
 *   </li>
 *   <li>Create an {@code SmtpEmailService} implementing this interface and annotate it
 *       with {@code @Service @Primary} to override the no-op default.</li>
 * </ol>
 * </p>
 */
public interface EmailService {

    /**
     * Sends a password reset email containing a one-time reset link.
     *
     * @param toEmail   recipient's email address
     * @param userName  recipient's display name (used in email body)
     * @param resetLink full HTTPS reset URL with the embedded raw token parameter
     */
    void sendPasswordResetEmail(String toEmail, String userName, String resetLink);
}

