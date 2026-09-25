package com.summa.enums;

public enum AgentClass {
    PERSISTENT("persistent"),
    EPHEMERAL("ephemeral"),
    EPHEMERAL_SUBAGENT("ephemeral-subagent");

    private final String value;

    AgentClass(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isEphemeral() {
        return this == EPHEMERAL || this == EPHEMERAL_SUBAGENT;
    }

    public static AgentClass fromValue(String value) {
        for (AgentClass c : values()) {
            if (c.value.equals(value)) return c;
        }
        return null;
    }
}
