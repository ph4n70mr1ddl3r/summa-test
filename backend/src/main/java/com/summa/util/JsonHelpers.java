package com.summa.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Map;

/**
 * Shared serialization helpers to prevent duplication across services and controllers.
 */
public final class JsonHelpers {
    private JsonHelpers() {}

    public static String toJson(Map<String, Object> map, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static String jsonString(String value) {
        if (value == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append("\"").toString();
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
}
