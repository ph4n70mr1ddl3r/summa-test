package com.summa.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;

/**
 * Shared Jackson serializers/deserializers for {@link Instant} fields.
 * Serializes to epoch-second longs; deserializes both epoch-second longs
 * and ISO-8601 strings (for forward compatibility with console input).
 * <p>
 * Register with any {@code ObjectMapper} via:
 * <pre>
 *   SimpleModule mod = new SimpleModule("InstantAsEpochSeconds");
 *   mod.addSerializer(Instant.class, new InstantEpochSecondSerializer());
 *   mod.addDeserializer(Instant.class, new InstantEpochSecondDeserializer());
 *   mapper.registerModule(mod);
 * </pre>
 */
public final class InstantSerializers {
    private InstantSerializers() {}

    public static class InstantEpochSecondSerializer extends JsonSerializer<Instant> {
        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeNumber(value.getEpochSecond());
            }
        }
    }

    public static class InstantEpochSecondDeserializer extends JsonDeserializer<Instant> {
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
