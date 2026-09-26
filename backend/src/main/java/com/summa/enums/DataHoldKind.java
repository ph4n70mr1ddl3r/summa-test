package com.summa.enums;

public enum DataHoldKind {
    MEMBER("member"),
    DOMAIN("domain");

    private final String value;

    DataHoldKind(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DataHoldKind fromValue(String value) {
        for (DataHoldKind k : values()) {
            if (k.value.equals(value)) return k;
        }
        return null;
    }

    public static DataHoldKind requireFromValue(String value) {
        DataHoldKind k = fromValue(value);
        if (k == null) {
            throw new IllegalArgumentException("Unknown DataHoldKind: " + value + ". Must be one of: member, domain");
        }
        return k;
    }
}
