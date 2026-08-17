package com.summa.enums;

public enum AgentClass {
    PERSISTENT("persistent"),
    EPHEMERAL("ephemeral");

    private final String value;

    AgentClass(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
