package com.summa.model;

/**
 * ASK-011: the explicit expiry behavior of an ask.
 *
 * <p>The values are the wire/storage vocabulary ({@code deny}, {@code escalate},
 * {@code reassign}) and are validated at every write door so an unknown value is
 * a validation refusal (422, API-061) rather than a CHECK abort (409).
 */
public enum ExpiryBehavior {
    DENY("deny"),
    ESCALATE("escalate"),
    REASSIGN("reassign");

    private final String value;

    ExpiryBehavior(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ExpiryBehavior fromValue(String value) {
        for (ExpiryBehavior b : values()) {
            if (b.value.equals(value)) return b;
        }
        return null;
    }

    public static ExpiryBehavior requireFromValue(String value) {
        ExpiryBehavior b = fromValue(value);
        if (b == null) {
            throw new IllegalArgumentException(
                "Unknown expiryBehavior: " + value + ". Must be one of: deny, escalate, reassign");
        }
        return b;
    }
}
