package com.summa.enums;

/**
 * The ask status vocabulary (DAT-080).
 *
 * <ul>
 *   <li>{@code pending} — live, awaiting a response.</li>
 *   <li>{@code answered} — the quorum landed (ASK-050's N distinct accepts).</li>
 *   <li>{@code denied} — a member denied the ask; ASK-050: "a deny closes it
 *       denied immediately".</li>
 *   <li>{@code expired} — the deadline passed. A denial by default (ASK-011's
 *       {@code deny} behavior), but distinct from an explicit deny because the
 *       {@code escalate}/{@code reassign} behaviors open a successor ask.</li>
 *   <li>{@code withdrawn} — the originator retracted (ASK-030).</li>
 * </ul>
 */
public enum AskStatus {
    PENDING("pending"),
    ANSWERED("answered"),
    DENIED("denied"),
    EXPIRED("expired"),
    WITHDRAWN("withdrawn");

    private final String value;

    AskStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AskStatus fromValue(String value) {
        if (value == null) return null;
        for (AskStatus status : values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }
}
