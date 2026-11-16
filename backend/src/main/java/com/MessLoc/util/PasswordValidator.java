package com.MessLoc.util;

import java.util.regex.Pattern;

/**
 * Shared password policy validator enforcing consistent complexity rules across
 * registration, password change, and password reset workflows.
 */
public final class PasswordValidator {

    /**
     * Policy requirements:
     * - Minimum 8 characters, maximum 100 characters
     * - At least one lowercase letter [a-z]
     * - At least one uppercase letter [A-Z]
     * - At least one decimal digit [0-9]
     * - At least one special symbol from allowed set
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/]).{8,100}$"
    );

    private PasswordValidator() {
        // Prevent instantiation of utility class
    }

    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public static void validatePassword(String password) {
        if (!isValid(password)) {
            throw new IllegalArgumentException(
                    "Password must be between 8 and 100 characters long and contain at least one uppercase letter, " +
                    "one lowercase letter, one digit, and one special character."
            );
        }
    }
}

