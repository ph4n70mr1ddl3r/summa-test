package com.summa.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ASK-011: expiry behavior is a closed wire/storage vocabulary, and the typed
 * field must not leak a second JSON property beside the documented string.
 */
class ExpiryBehaviorTest {

    @Test
    void fromValue_mapsTheWireVocabulary() {
        assertEquals(ExpiryBehavior.DENY, ExpiryBehavior.fromValue("deny"));
        assertEquals(ExpiryBehavior.ESCALATE, ExpiryBehavior.fromValue("escalate"));
        assertEquals(ExpiryBehavior.REASSIGN, ExpiryBehavior.fromValue("reassign"));
        assertNull(ExpiryBehavior.fromValue("maybe"));
        assertNull(ExpiryBehavior.fromValue(null));
    }

    @Test
    void requireFromValue_rejectsAnUnknownValueWithTheVocabulary() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ExpiryBehavior.requireFromValue("maybe"));
        assertTrue(ex.getMessage().contains("deny, escalate, reassign"));
    }

    @Test
    void askSerialization_exposesTheDocumentedStringOnly() throws Exception {
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setExpiryBehavior("escalate");

        String json = new ObjectMapper().writeValueAsString(ask);

        assertTrue(json.contains("\"expiryBehavior\":\"escalate\""));
        assertFalse(json.contains("expiryBehaviorEnum"));
        Ask roundTripped = new ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(json, Ask.class);
        assertEquals("escalate", roundTripped.getExpiryBehavior());
    }
}
