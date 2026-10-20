package com.summa.enums;

/**
 * The class vocabulary of the role-template catalog (DAT-030, DNC-061).
 *
 * <p>A template names the worker class it instantiates, and the catalog column
 * admits exactly two values — {@code persistent} and {@code ephemeral-subagent}
 * (DAT-010's {@code role_templates.class}). The spawn classes are three
 * ({@code persistent}, {@code ephemeral}, {@code ephemeral-subagent} — SPW-001);
 * an {@code ephemeral} worker instantiates a whitelisted {@code ephemeral-subagent}
 * template (SPW-021), so no template ever carries the bare {@code ephemeral} class.
 *
 * <p>The vocabulary is deliberately narrower than {@link AgentClass}: validating a
 * template against the agent-class enum would accept {@code ephemeral}, a value the
 * column's CHECK rejects — the write would abort as an integrity violation (409)
 * where the door owes a validation refusal (422, API-061).
 */
public enum RoleTemplateClass {
    PERSISTENT("persistent"),
    EPHEMERAL_SUBAGENT("ephemeral-subagent");

    private final String value;

    RoleTemplateClass(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RoleTemplateClass fromValue(String value) {
        for (RoleTemplateClass c : values()) {
            if (c.value.equals(value)) return c;
        }
        return null;
    }

    public static RoleTemplateClass requireFromValue(String value) {
        RoleTemplateClass c = fromValue(value);
        if (c == null) {
            throw new IllegalArgumentException(
                "Unknown template class: " + value + ". Must be one of: persistent, ephemeral-subagent");
        }
        return c;
    }

    /**
     * SPW-021: the template class a spawn class must name. A persistent hire names
     * a {@code persistent}-class template; an ephemeral worker and an ephemeral
     * subagent both name a whitelisted {@code ephemeral-subagent}-class one.
     */
    public static String templateClassForSpawnClass(String spawnClass) {
        if (AgentClass.PERSISTENT.getValue().equals(spawnClass)) {
            return PERSISTENT.getValue();
        }
        if (AgentClass.EPHEMERAL.getValue().equals(spawnClass)
                || AgentClass.EPHEMERAL_SUBAGENT.getValue().equals(spawnClass)) {
            return EPHEMERAL_SUBAGENT.getValue();
        }
        return null;
    }
}
