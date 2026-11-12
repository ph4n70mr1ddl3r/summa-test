package com.summa.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyedUnionValidatorTest {

    @Test
    void valid_human_keyed_union_passes() {
        KeyedUnionValidator.validate("h:human-123", "proposedBy");
    }

    @Test
    void valid_agent_keyed_union_passes() {
        KeyedUnionValidator.validate("a:agent-456", "proposedBy");
    }

    @Test
    void null_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            KeyedUnionValidator.validate(null, "field"));
    }

    @Test
    void blank_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            KeyedUnionValidator.validate("  ", "field"));
    }

    @Test
    void reserved_system_bypasses_pattern_check_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            KeyedUnionValidator.validate("system", "proposedBy"));
    }

    @Test
    void reserved_admins_bypasses_pattern_check_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            KeyedUnionValidator.validate("admins", "proposedBy"));
    }

    @Test
    void invalid_format_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            KeyedUnionValidator.validate("invalid", "proposedBy"));
    }

    @Test
    void human_without_prefix_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            KeyedUnionValidator.validate("human-123", "proposedBy"));
    }

    @Test
    void agent_without_prefix_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            KeyedUnionValidator.validate("agent-456", "proposedBy"));
    }
}