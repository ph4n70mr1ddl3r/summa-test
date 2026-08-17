package com.summa.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {

    private final PasswordEncoder passwordEncoder;

    public PasswordUtil(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hash(String password) {
        if (password == null) return null;
        return passwordEncoder.encode(password);
    }

    public boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null) return false;
        return passwordEncoder.matches(password, storedHash);
    }
}
