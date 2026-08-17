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
}
