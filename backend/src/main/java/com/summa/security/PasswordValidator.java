package com.summa.security;

/**
 * Shared password validation rules used across OrgService and AuthController.
 */
public final class PasswordValidator {
    private PasswordValidator() {}

    public static void validate(String password) {
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters");
        }
        if (password.length() > 256) {
            throw new IllegalArgumentException("Password must not exceed 256 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }
}
