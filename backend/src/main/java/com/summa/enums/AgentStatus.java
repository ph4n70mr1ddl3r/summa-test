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
}
