package com.summa.security;

import java.util.regex.Pattern;

/**
 * Shared password validation rules used across OrgService and AuthController.
 */
public final class PasswordValidator {
    private static final Pattern UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL = Pattern.compile(".*[^A-Za-z0-9].*");

    private PasswordValidator() {}

    public static void validate(String password) {
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters");
        }
        if (password.length() > 256) {
            throw new IllegalArgumentException("Password must not exceed 256 characters");
        }
        if (!UPPER.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!LOWER.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!DIGIT.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
        if (!SPECIAL.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }
}
