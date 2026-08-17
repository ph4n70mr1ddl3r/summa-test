package com.summa.enums;

public enum AskKind {
    APPROVAL("approval"),
    QUESTION("question"),
    ASSIGNMENT("assignment"),
    SPAWN_REQUEST("spawn_request");

    private final String value;

    AskKind(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
