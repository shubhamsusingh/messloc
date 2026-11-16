package com.MessLoc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Represents a secure one-time password reset token stored in MongoDB.
 * Only the SHA-256 hash of the raw token is persisted — the raw token is never stored.
 *
 * <p>MongoDB TTL index on {@code expiryDate} automatically removes expired documents.
 * Service-layer expiry check is also enforced as a defence-in-depth measure.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "password_reset_tokens")
@CompoundIndex(name = "userId_used_idx", def = "{'userId': 1, 'used': 1}")
public class PasswordResetToken {

    @Id
    private String id;

    /**
     * The user this token belongs to (references {@code users._id}).
     */
    private String userId;

    /**
     * SHA-256 hex-encoded hash of the raw plaintext token.
     * The raw token is transmitted to the user but never stored here.
     */
    @Indexed(unique = true)
    private String tokenHash;

    /**
     * Absolute expiry time. MongoDB TTL index expires the document automatically.
     * Service layer also validates this explicitly.
     */
    @Indexed(expireAfter = "0s")
    private Instant expiryDate;

    /**
     * Whether this token has already been consumed (used=true means it cannot be reused).
     */
    @Builder.Default
    private boolean used = false;

    /**
     * When the token was created (used for rate-limiting checks).
     */
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * When the token was consumed (null if not yet used).
     */
    private Instant usedAt;
}

