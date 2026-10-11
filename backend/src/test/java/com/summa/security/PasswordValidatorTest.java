package com.summa.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    @Test
    void validate_minLengthSatisfied_allows() {
        assertDoesNotThrow(() -> PasswordValidator.validate("Abcdefghij1!"));
    }

    @Test
    void validate_tooShortRejects() {
        assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate("Abcdefg1!"));
    }

    @Test
    void validate_nullPassword_throws() {
        assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate(null));
    }

    @Test
    void validate_blankPassword_throws() {
        assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate(""));
    }

    @Test
    void validate_missingUpper_rejects() {
        assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate("abcdefgh1!"));
    }

    @Test
    void validate_missingLower_rejects() {
        assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate("ABCDEFGH1!"));
    }

    @Test
    void validate_missingDigit_rejects() {
        assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate("Abcdefgh!!"));
    }

    @Test
    void validate_missingSpecial_rejects() {
        assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate("Abcdefgh1"));
    }

    @Test
    void validate_allRequirementsMet_allows() {
        assertDoesNotThrow(() -> PasswordValidator.validate("SecureP@ssw0rd!"));
    }

    @Test
    void validate_exactly72Bytes_allows() {
        // 72 ASCII chars = 72 bytes
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 72; i++) {
            sb.append('a');
        }
        sb.setCharAt(0, 'A');
        sb.setCharAt(10, '1');
        sb.setCharAt(20, '!');
        assertDoesNotThrow(() -> PasswordValidator.validate(sb.toString()));
    }

    @Test
    void validate_over72Bytes_rejects() {
        // 73 ASCII chars = 73 bytes > 72
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 73; i++) {
            sb.append('a');
        }
        sb.setCharAt(0, 'A');
        sb.setCharAt(10, '1');
        sb.setCharAt(20, '!');
        assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate(sb.toString()));
    }

    @Test
    void validate_multiByteChars_byteLimit() {
        // Multi-byte UTF-8 char: "😀" is 4 bytes. 17 chars * 4 = 68 bytes + "Aa1!" = 72 bytes exactly.
        String multiByte = "Aa" + "😀".repeat(17) + "1!";
        // "Aa" (2) + 17*"😀" (68) + "1!" (2) = 72 bytes
        assertDoesNotThrow(() -> PasswordValidator.validate(multiByte));
    }
}
