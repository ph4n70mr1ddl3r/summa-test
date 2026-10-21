package com.summa.model;

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
}