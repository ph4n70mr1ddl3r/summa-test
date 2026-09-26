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

    public static MemoryTier fromValue(String value) {
        for (MemoryTier t : values()) {
            if (t.value.equals(value)) return t;
        }
        return null;
    }

    public static MemoryTier requireFromValue(String value) {
        MemoryTier t = fromValue(value);
        if (t == null) {
            throw new IllegalArgumentException("Unknown MemoryTier: " + value + ". Must be one of: personal, project, proposal");
        }
        return t;
    }
}
