package com.summa.enums;

public enum GoalStatus {
    ACTIVE("active"),
    MET("met"),
    MISSED("missed"),
    RETIRED("retired");

    private final String value;

    GoalStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static GoalStatus fromValue(String value) {
        for (GoalStatus s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }

    public static GoalStatus requireFromValue(String value) {
        GoalStatus s = fromValue(value);
        if (s == null) {
            throw new IllegalArgumentException("Invalid goal status: " + value + ". Must be one of: active, met, missed, retired");
        }
        return s;
    }
}
