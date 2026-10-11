package com.summa.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
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
    private static final ObjectMapper MAPPER = buildMapper();
    private JsonHelpers() {}

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("InstantAsEpochSeconds");
        module.addSerializer(Instant.class, new InstantEpochSecondSerializer());
        module.addDeserializer(Instant.class, new InstantEpochSecondDeserializer());
        mapper.registerModule(module);
        return mapper;
    }

    public static String toJson(Map<String, Object> map, ObjectMapper mapper) {
        if (mapper == null) {
            mapper = MAPPER;
        }
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
     * Normalize a URL path by stripping a trailing slash (except for root "/").
     */
    public static String normalizeTrailingSlash(String path) {
        if (path == null) return null;
        return path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
    }

    /**
     * Strip keyed-union prefix (h:/a:) from a member/agent ID.
     */
    public static String stripIdPrefix(String id) {
        if (id == null) return null;
        return id.replaceFirst("^[ha]?:", "");
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     * Returns false immediately on length mismatch to avoid length-oracle side channels.
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return a == b;
        int lenA = a.length();
        int lenB = b.length();
        if (lenA != lenB) return false;
        int result = 0;
        for (int i = 0; i < lenA; i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    static class InstantEpochSecondSerializer extends JsonSerializer<Instant> {
        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeNumber(value.getEpochSecond());
            }
        }
    }

    static class InstantEpochSecondDeserializer extends JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            JsonToken token = p.currentToken();
            if (token == null) return null;
            switch (token) {
                case VALUE_NUMBER_INT:
                    return Instant.ofEpochSecond(p.getLongValue());
                case VALUE_STRING: {
                    String text = p.getText();
                    if (text == null || text.isBlank()) return null;
                    String t = text.trim();
                    try {
                        return Instant.parse(t);
                    } catch (DateTimeException e) {
                        try {
                            return Instant.ofEpochSecond(Long.parseLong(t));
                        } catch (NumberFormatException nfe) {
                            throw new IOException("Invalid Instant value: " + text, e);
                        }
                    }
                }
                case VALUE_NULL:
                    return null;
                default:
                    throw new IOException("Expected epoch-second number or ISO-8601 string for Instant, got " + p.currentToken());
            }
        }
    }
}
