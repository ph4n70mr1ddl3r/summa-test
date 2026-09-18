package com.summa.util;

import java.util.regex.Pattern;

public final class KeyedUnionValidator {
    public static final Pattern KEYED_UNION_PATTERN = Pattern.compile("^[ha]?:.+$|^[a-zA-Z0-9_-]+$");

    private KeyedUnionValidator() {}

    public static void validate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (!KEYED_UNION_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a valid keyed union (h:<human-id> or a:<agent-id>)");
        }
    }
}
