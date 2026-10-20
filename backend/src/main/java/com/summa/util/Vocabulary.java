package com.summa.util;

import java.util.Arrays;

/**
 * API-061: validation before constraint — a narrow column's vocabulary is checked
 * at the door, so a request that names a value the column's CHECK rejects answers
 * 422 validation instead of aborting the write as an integrity violation (a 409).
 */
public final class Vocabulary {

    private Vocabulary() {
    }

    /**
     * Require that the value is one of the allowed spellings.
     *
     * @param value  the caller-supplied value (null or blank is accepted as absent)
     * @param field  the field name, for the refusal message
     * @param allowed the vocabulary the column admits
     * @return the value when it is allowed, null when it is absent
     */
    public static String requireOneOf(String value, String field, String... allowed) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (String candidate : allowed) {
            if (candidate.equals(value)) {
                return value;
            }
        }
        throw new IllegalArgumentException(
                "Invalid " + field + ": " + value + ". Must be one of: " + String.join(", ", Arrays.asList(allowed)));
    }
}
