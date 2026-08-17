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
}
