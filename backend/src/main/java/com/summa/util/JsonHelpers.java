package com.summa.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared serialization helpers to prevent duplication across services and controllers.
 */
public final class JsonHelpers {
    private static final Logger log = LoggerFactory.getLogger(JsonHelpers.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JsonHelpers() {}

    public static String toJson(Map<String, Object> map, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize map to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    public static String jsonString(String value) {
        if (value == null) return "null";
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            // Fallback to manual escaping if ObjectMapper fails
            StringBuilder sb = new StringBuilder("\"");
            for (int i = 0; i < value.length(); ) {
                int c = value.codePointAt(i);
                if (c == '"') {
                    sb.append("\\\"");
                } else if (c == '\\') {
                    sb.append("\\\\");
                } else if (c == '\n') {
                    sb.append("\\n");
                } else if (c == '\r') {
                    sb.append("\\r");
                } else if (c == '\t') {
                    sb.append("\\t");
                } else if (c < 0x20) {
                    sb.append(String.format("\\u%04x", c));
                } else {
                    sb.appendCodePoint(c);
                }
                i += Character.charCount(c);
            }
            return sb.append("\"").toString();
        }
    }

    /**
     * Parse an optional ISO-8601 instant. Blank/missing values return null.
     * Accepts both ISO-8601 strings and epoch-second long values.
     */
    public static Instant parseOptionalInstant(String value, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeException e) {
            try {
                return Instant.ofEpochSecond(Long.parseLong(value.trim()));
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid " + field + " format: " + value);
            }
        }
    }

    public static Integer parseIntSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value: " + s);
        }
    }

    public static Double parseDoubleSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value: " + s);
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return a == b;
        int lenA = a.length();
        int lenB = b.length();
        // Always iterate the full length of the longer string to prevent timing leaks
        // on length mismatch. Caller must ensure equal lengths for security use.
        int result = 0;
        int len = Math.max(lenA, lenB);
        for (int i = 0; i < len; i++) {
            char ca = i < lenA ? a.charAt(i) : 0;
            char cb = i < lenB ? b.charAt(i) : 0;
            result |= ca ^ cb;
        }
        // Still reject mismatched lengths, but after constant-time iteration
        if (lenA != lenB) result |= 0xFF;
        return result == 0;
    }
}
