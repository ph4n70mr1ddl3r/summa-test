package com.summa.util;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
