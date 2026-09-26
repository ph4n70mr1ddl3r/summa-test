package com.summa.enums;

public enum DomainAccess {
    PUBLIC("public"),
    DOMAIN("domain"),
    NAMED("named");

    private final String value;

    DomainAccess(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isRestricted() {
        return this != PUBLIC;
    }

    public static DomainAccess fromValue(String value) {
        for (DomainAccess d : values()) {
            if (d.value.equals(value)) return d;
        }
        return null;
    }

    public static DomainAccess requireFromValue(String value) {
        DomainAccess d = fromValue(value);
        if (d == null) {
            throw new IllegalArgumentException("Unknown DomainAccess: " + value + ". Must be one of: public, domain, named");
        }
        return d;
    }
}
