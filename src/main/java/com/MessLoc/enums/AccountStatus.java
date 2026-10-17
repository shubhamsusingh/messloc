package com.MessLoc.enums;

/**
 * Represents the lifecycle and operational status of an account.
 * 
 * ACTIVE: Account is fully functional and permitted to access authorized features.
 * INACTIVE: Account has been temporarily deactivated or disabled.
 * BLOCKED: Account has been banned or blocked due to policy violations (authentication rejected).
 * PENDING: Account created but awaiting manual verification (e.g., OWNER awaiting admin approval).
 */
public enum AccountStatus {
    ACTIVE,
    INACTIVE,
    BLOCKED,
    PENDING
}
