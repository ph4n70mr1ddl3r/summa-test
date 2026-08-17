package com.summa.enums;

public enum RbacRole {
    ADMIN("admin"),
    OWNER("owner"),
    MEMBER("member"),
    VIEWER("viewer");

    private final String value;

    RbacRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean canWrite() {
        return this != VIEWER;
    }

    public boolean canManageDomains() {
        return this == ADMIN || this == OWNER;
    }

    public boolean canSpawn() {
        return this == ADMIN || this == MEMBER || this == OWNER;
    }
}
