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
}
