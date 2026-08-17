package com.summa.enums;

public enum MemoryTier {
    PERSONAL("personal"),
    PROJECT("project"),
    PROPOSAL("proposal");

    private final String value;

    MemoryTier(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
