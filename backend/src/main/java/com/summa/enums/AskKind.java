package com.summa.enums;

public enum AskKind {
    APPROVAL("approval"),
    QUESTION("question"),
    ASSIGNMENT("assignment"),
    SPAWN_REQUEST("spawn_request"),
    PROMOTION("promotion");

    private final String value;

    AskKind(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AskKind fromValue(String value) {
        for (AskKind k : values()) {
            if (k.value.equals(value)) return k;
        }
        return null;
    }

    public static AskKind requireFromValue(String value) {
        AskKind k = fromValue(value);
        if (k == null) {
            throw new IllegalArgumentException("Unknown AskKind: " + value + ". Must be one of: approval, question, assignment, spawn_request, promotion");
        }
        return k;
    }
}
