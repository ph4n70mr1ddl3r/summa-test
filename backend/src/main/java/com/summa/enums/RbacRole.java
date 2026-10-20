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

    /**
     * The authority ladder: admin &gt; owner &gt; member &gt; viewer. A role change
     * that lowers the rank is a demotion (OFB-030/OFB-033) and runs the dependency
     * walk; one that raises it is an elevation and must not.
     */
    public int rank() {
        switch (this) {
            case ADMIN: return 3;
            case OWNER: return 2;
            case MEMBER: return 1;
            case VIEWER: return 0;
            default: return 0;
        }
    }

    public boolean canManageDomains() {
        return this == ADMIN || this == OWNER;
    }

    public boolean canSpawn() {
        return this == ADMIN || this == MEMBER || this == OWNER;
    }

    public static RbacRole fromValue(String value) {
        for (RbacRole r : values()) {
            if (r.value.equals(value)) return r;
        }
        return null;
    }

    public static RbacRole requireFromValue(String value) {
        RbacRole r = fromValue(value);
        if (r == null) {
            throw new IllegalArgumentException("Unknown RbacRole: " + value + ". Must be one of: admin, owner, member, viewer");
        }
        return r;
    }
}
