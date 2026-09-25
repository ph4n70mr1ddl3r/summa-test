package com.summa.enums;

public enum AgentStatus {
    REQUESTED("requested"),
    ACTIVE("active"),
    SUSPENDED("suspended"),
    RETIRING("retiring"),
    ARCHIVED("archived");

    private final String value;

    AgentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public static AgentStatus fromValue(String value) {
        for (AgentStatus s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }
}
