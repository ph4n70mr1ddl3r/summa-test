package com.summa.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonHelpersTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void toJson_returnsJsonString() {
        Map<String, Object> map = Map.of("key", "value", "num", 42);
        String json = JsonHelpers.toJson(map, mapper);
        assertNotNull(json);
        assertTrue(json.contains("\"key\":\"value\""));
        assertTrue(json.contains("\"num\":42"));
    }

    @Test
    void toJson_nullMapper_usesDefault() {
        Map<String, Object> map = Map.of("a", "b");
        String json = JsonHelpers.toJson(map, null);
        assertNotNull(json);
        assertTrue(json.contains("\"a\":\"b\""));
    }

    @Test
    void jsonString_escapesSpecialCharacters() {
        String input = "hello \"world\" \n\t\\ path";
        String json = JsonHelpers.jsonString(input);
        assertEquals("\"hello \\\"world\\\" \\n\\t\\\\ path\"", json);
    }

    @Test
    void jsonString_nullInput_returnsNullLiteral() {
        assertEquals("null", JsonHelpers.jsonString(null));
    }

    @Test
    void parseOptionalInstant_iso8601String() {
        Instant result = JsonHelpers.parseOptionalInstant("2024-01-15T10:30:00Z", "time");
        assertNotNull(result);
        assertEquals(Instant.parse("2024-01-15T10:30:00Z"), result);
    }

    @Test
    void parseOptionalInstant_epochSeconds() {
        Instant result = JsonHelpers.parseOptionalInstant("1705312200", "time");
        assertNotNull(result);
        assertEquals(Instant.ofEpochSecond(1705312200L), result);
    }

    @Test
    void parseOptionalInstant_blank_returnsNull() {
        assertNull(JsonHelpers.parseOptionalInstant("", "time"));
        assertNull(JsonHelpers.parseOptionalInstant("   ", "time"));
    }

    @Test
    void parseOptionalInstant_null_returnsNull() {
        assertNull(JsonHelpers.parseOptionalInstant(null, "time"));
    }

    @Test
    void parseOptionalInstant_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            JsonHelpers.parseOptionalInstant("not-a-date", "time"));
    }

    @Test
    void parseIntSafe_validInt() {
        assertEquals(42, JsonHelpers.parseIntSafe("42"));
    }

    @Test
    void parseIntSafe_blank_returnsNull() {
        assertNull(JsonHelpers.parseIntSafe(""));
        assertNull(JsonHelpers.parseIntSafe(null));
    }

    @Test
    void parseIntSafe_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            JsonHelpers.parseIntSafe("not-an-int"));
    }

    @Test
    void parseDoubleSafe_validDouble() {
        assertEquals(3.14, JsonHelpers.parseDoubleSafe("3.14"));
    }

    @Test
    void parseDoubleSafe_blank_returnsNull() {
        assertNull(JsonHelpers.parseDoubleSafe(""));
        assertNull(JsonHelpers.parseDoubleSafe(null));
    }

    @Test
    void parseDoubleSafe_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            JsonHelpers.parseDoubleSafe("not-a-number"));
    }

    @Test
    void normalizeTrailingSlash_stripsTrailing() {
        assertEquals("/api/v1", JsonHelpers.normalizeTrailingSlash("/api/v1/"));
    }

    @Test
    void normalizeTrailingSlash_preservesRoot() {
        assertEquals("/", JsonHelpers.normalizeTrailingSlash("/"));
    }

    @Test
    void normalizeTrailingSlash_null_returnsNull() {
        assertNull(JsonHelpers.normalizeTrailingSlash(null));
    }

    @Test
    void stripIdPrefix_stripsHColon() {
        assertEquals("human-1", JsonHelpers.stripIdPrefix("h:human-1"));
    }

    @Test
    void stripIdPrefix_stripsAColon() {
        assertEquals("agent-1", JsonHelpers.stripIdPrefix("a:agent-1"));
    }

    @Test
    void stripIdPrefix_noPrefix_returnsAsIs() {
        assertEquals("plain-id", JsonHelpers.stripIdPrefix("plain-id"));
    }

    @Test
    void stripIdPrefix_null_returnsNull() {
        assertNull(JsonHelpers.stripIdPrefix(null));
    }

    @Test
    void constantTimeEquals_sameStrings_returnsTrue() {
        assertTrue(JsonHelpers.constantTimeEquals("abc", "abc"));
    }

    @Test
    void constantTimeEquals_differentStrings_returnsFalse() {
        assertFalse(JsonHelpers.constantTimeEquals("abc", "def"));
    }

    @Test
    void constantTimeEquals_differentLengths_returnsFalse() {
        assertFalse(JsonHelpers.constantTimeEquals("abc", "abcd"));
    }

    @Test
    void constantTimeEquals_nullInputs() {
        assertTrue(JsonHelpers.constantTimeEquals(null, null));
        assertFalse(JsonHelpers.constantTimeEquals(null, "abc"));
        assertFalse(JsonHelpers.constantTimeEquals("abc", null));
    }

    @Test
    void instant_serialization_deserialization_roundtrip() throws Exception {
        com.fasterxml.jackson.databind.module.SimpleModule mod = new com.fasterxml.jackson.databind.module.SimpleModule();
        mod.addSerializer(Instant.class, new com.summa.util.InstantSerializers.InstantEpochSecondSerializer());
        mod.addDeserializer(Instant.class, new com.summa.util.InstantSerializers.InstantEpochSecondDeserializer());
        ObjectMapper roundtripMapper = new ObjectMapper();
        roundtripMapper.registerModule(mod);

        Instant original = Instant.parse("2024-01-15T10:30:00Z");
        String json = roundtripMapper.writeValueAsString(Map.of("ts", original));
        Instant parsed = roundtripMapper.readValue(json, InstantHolder.class).ts;
        assertEquals(original, parsed);
    }

    @Test
    void instant_deserialization_acceptsIsoString() throws Exception {
        com.fasterxml.jackson.databind.module.SimpleModule mod = new com.fasterxml.jackson.databind.module.SimpleModule();
        mod.addSerializer(Instant.class, new com.summa.util.InstantSerializers.InstantEpochSecondSerializer());
        mod.addDeserializer(Instant.class, new com.summa.util.InstantSerializers.InstantEpochSecondDeserializer());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(mod);

        String isoJson = "{\"ts\":\"2024-01-15T10:30:00Z\"}";
        Instant parsed = mapper.readValue(isoJson, InstantHolder.class).ts;
        assertEquals(Instant.parse("2024-01-15T10:30:00Z"), parsed);
    }

    @Test
    void instant_deserialization_acceptsEpochLong() throws Exception {
        com.fasterxml.jackson.databind.module.SimpleModule mod = new com.fasterxml.jackson.databind.module.SimpleModule();
        mod.addSerializer(Instant.class, new com.summa.util.InstantSerializers.InstantEpochSecondSerializer());
        mod.addDeserializer(Instant.class, new com.summa.util.InstantSerializers.InstantEpochSecondDeserializer());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(mod);

        String epochJson = "{\"ts\":1705312200}";
        Instant parsed = mapper.readValue(epochJson, InstantHolder.class).ts;
        assertEquals(Instant.ofEpochSecond(1705312200L), parsed);
    }

    static class InstantHolder {
        public Instant ts;
    }
}
