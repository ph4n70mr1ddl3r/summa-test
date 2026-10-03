package com.summa.util;

import java.util.Set;
import java.util.regex.Pattern;

public final class KeyedUnionValidator {
    public static final Pattern KEYED_UNION_PATTERN = Pattern.compile("^[ha]:.+$");
    // Reserved non-keyed-union identifiers permitted in specific contexts per DAT-120
    public static final Set<String> RESERVED_IDS = Set.of("system", "admins");

    private KeyedUnionValidator() {}

    public static void validate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (RESERVED_IDS.contains(value)) return;
        if (!KEYED_UNION_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a valid keyed union (h:<human-id> or a:<agent-id>)");
        }
    }
}
