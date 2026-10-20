package com.summa.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ask status vocabulary migration (DAT-080's `denied` terminal).
 *
 * <p>The application's own test profile runs on H2 with the schema built from the
 * entities, so {@code schema.sql} is never executed there — the migration is
 * exercised here against a real SQLite database instead, against the DDL a
 * deployment created before the vocabulary widened.
 */
class SchemaMigrationsTest {

    private static final String LEGACY_ASKS_DDL = """
            CREATE TABLE asks (
                id TEXT PRIMARY KEY,
                kind TEXT NOT NULL CHECK (kind IN ('approval', 'question', 'assignment', 'spawn_request', 'promotion')),
                "from" TEXT NOT NULL CHECK ("from" LIKE 'h:%' OR "from" LIKE 'a:%' OR "from" = 'system'),
                "to" TEXT NOT NULL CHECK ("to" = 'admins' OR "to" LIKE 'h:%' OR "to" LIKE 'a:%'),
                payload TEXT NOT NULL DEFAULT '{}',
                initiative_id TEXT,
                workspace_id TEXT,
                status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'answered', 'expired', 'withdrawn')),
                deadline INTEGER NOT NULL,
                created_at INTEGER NOT NULL DEFAULT (unixepoch()),
                sla_tier TEXT NOT NULL DEFAULT 'standard' CHECK (sla_tier IN ('critical', 'standard', 'bulk')),
                escalation TEXT,
                expiry_behavior TEXT NOT NULL DEFAULT 'deny' CHECK (expiry_behavior IN ('deny', 'escalate', 'reassign')),
                responded_at INTEGER,
                quorum_required INTEGER NOT NULL DEFAULT 1 CHECK (quorum_required >= 1),
                responses TEXT NOT NULL DEFAULT '[]',
                collapsed_count INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT (unixepoch())
            )
            """;

    /**
     * A file-backed SQLite database: an in-memory one is per-connection, and the
     * JdbcTemplate opens a new connection for every statement.
     */
    private JdbcTemplate sqlite(String name) throws Exception {
        java.nio.file.Path file = java.nio.file.Files.createTempFile("summa-" + name + "-", ".db");
        file.toFile().deleteOnExit();
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:sqlite:" + file.toAbsolutePath());
        JdbcTemplate template = new JdbcTemplate(ds);
        template.execute("PRAGMA foreign_keys=OFF");
        return template;
    }

    @Test
    void askStatusAllowsDenied_detectsANarrowVocabulary() {
        assertFalse(SchemaMigrations.askStatusAllowsDenied(LEGACY_ASKS_DDL));
        assertTrue(SchemaMigrations.askStatusAllowsDenied(
            LEGACY_ASKS_DDL.replace("'pending', 'answered', 'expired', 'withdrawn'",
                "'pending', 'answered', 'denied', 'expired', 'withdrawn'")));
    }

    @Test
    void askStatusAllowsDenied_isScopedToTheStatusClause() {
        // A stray 'denied' elsewhere in the DDL must not mask a narrow status
        // constraint.
        String ddl = LEGACY_ASKS_DDL.replace("escalation TEXT,", "escalation TEXT, -- 'denied' is a value here");
        assertFalse(SchemaMigrations.askStatusAllowsDenied(ddl));
    }

    @Test
    void askStatusAllowsDenied_aTableWithoutAStatusCheckNeedsNoMigration() {
        assertTrue(SchemaMigrations.askStatusAllowsDenied("CREATE TABLE asks (id TEXT PRIMARY KEY)"));
    }

    @Test
    void migrateAskStatusVocabulary_widensTheDeployedTableAndKeepsTheRows() throws Exception {
        JdbcTemplate jdbc = sqlite("migration");
        jdbc.execute(LEGACY_ASKS_DDL);
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_asks_status ON asks(status)");
        jdbc.update("INSERT INTO asks (id, kind, \"from\", \"to\", payload, status, deadline) "
            + "VALUES (?, 'approval', 'h:human-1', 'h:human-2', '{}', 'pending', ?)",
            "ask-1", 1_700_000_000L);

        // The narrow vocabulary refuses a denied row — the defect the migration closes.
        assertThrows(Exception.class, () -> jdbc.update(
            "INSERT INTO asks (id, kind, \"from\", \"to\", payload, status, deadline) "
                + "VALUES (?, 'approval', 'h:human-1', 'h:human-2', '{}', 'denied', ?)",
            "ask-2", 1_700_000_000L));

        assertTrue(SchemaMigrations.migrateAskStatusVocabulary(jdbc));

        // The row survived the rebuild.
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, status FROM asks");
        assertEquals(1, rows.size());
        assertEquals("ask-1", rows.get(0).get("id"));

        // The widened vocabulary admits the denied terminal.
        jdbc.update("INSERT INTO asks (id, kind, \"from\", \"to\", payload, status, deadline) "
            + "VALUES (?, 'approval', 'h:human-1', 'h:human-2', '{}', 'denied', ?)",
            "ask-2", 1_700_000_000L);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM asks", Integer.class));

        // The indexes are recreated against the rebuilt table.
        assertEquals(1, jdbc.queryForObject(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='idx_asks_status'", Integer.class));
    }

    @Test
    void migrateAskStatusVocabulary_isANoOpOnAWidenedTable() throws Exception {
        JdbcTemplate jdbc = sqlite("noop");
        jdbc.execute(LEGACY_ASKS_DDL.replace("'pending', 'answered', 'expired', 'withdrawn'",
            "'pending', 'answered', 'denied', 'expired', 'withdrawn'"));
        assertFalse(SchemaMigrations.migrateAskStatusVocabulary(jdbc));
    }
}
