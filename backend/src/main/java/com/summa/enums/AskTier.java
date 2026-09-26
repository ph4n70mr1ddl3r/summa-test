package com.summa.enums;

public enum AskTier {
    CRITICAL("critical"),
    STANDARD("standard"),
    BULK("bulk");

    private final String value;

    AskTier(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AskTier fromValue(String value) {
        for (AskTier t : values()) {
            if (t.value.equals(value)) return t;
        }
        return null;
    }

    public static AskTier requireFromValue(String value) {
        AskTier t = fromValue(value);
        if (t == null) {
            throw new IllegalArgumentException("Unknown AskTier: " + value + ". Must be one of: critical, standard, bulk");
        }
        return t;
    }
}
