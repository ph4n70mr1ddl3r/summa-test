package com.summa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Schema migrations for databases created before a CHECK constraint widened.
 *
 * <p>{@code schema.sql} is applied with {@code CREATE TABLE IF NOT EXISTS}, so an
 * existing deployment keeps the table it was created with — a widened CHECK
 * constraint never reaches it. SQLite cannot alter a CHECK constraint in place,
 * so the table is rebuilt: the rows are copied into a table with the new
 * constraint, the old table is dropped and the new one is renamed.
 *
 * <p>DAT-080's ask status vocabulary gained {@code denied} (ASK-050's terminal:
 * a deny closes the ask denied immediately). Databases created before that
 * carry the four-value CHECK and would abort the write with a constraint
 * violation, so the rebuild runs on every start-up.
 */
public final class SchemaMigrations {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrations.class);

    /** The table name the migration rebuilds. */
    static final String ASK_TABLE = "asks";

    /** The temporary name the rebuilt table carries before the rename. */
    static final String ASK_TABLE_STAGING = "asks_status_denied_migration";

    private SchemaMigrations() {
    }

    /**
     * Does the stored {@code asks} CREATE TABLE admit the {@code denied} status?
     *
     * <p>The check is scoped to the {@code status IN (...)} clause so a stray
     * {@code 'denied'} elsewhere in the DDL cannot mask a narrow constraint.
     * A table without a status CHECK needs no migration.
     *
     * @param createSql the {@code sqlite_master.sql} text of the asks table
     * @return true when the table already carries the widened vocabulary
     */
    public static boolean askStatusAllowsDenied(String createSql) {
        if (createSql == null) {
            return false;
        }
        int start = createSql.indexOf("status IN (");
        if (start < 0) {
            // No status CHECK at all — nothing to widen.
            return true;
        }
        int end = createSql.indexOf(')', start);
        String clause = end < 0 ? createSql.substring(start) : createSql.substring(start, end);
        return clause.contains("'denied'");
    }

    /**
     * The statements that rebuild the {@code asks} table with the widened
     * status vocabulary, preserving every row.
     */
    public static List<String> askDeniedStatusMigrationStatements() {
        List<String> statements = new ArrayList<>();
        statements.add(
                "CREATE TABLE " + ASK_TABLE_STAGING + " ("
                        + "id TEXT PRIMARY KEY, "
                        + "kind TEXT NOT NULL CHECK (kind IN ('approval', 'question', 'assignment', 'spawn_request', 'promotion')), "
                        + "\"from\" TEXT NOT NULL CHECK (\"from\" LIKE 'h:%' OR \"from\" LIKE 'a:%' OR \"from\" = 'system'), "
                        + "\"to\" TEXT NOT NULL CHECK (\"to\" = 'admins' OR \"to\" LIKE 'h:%' OR \"to\" LIKE 'a:%'), "
                        + "payload TEXT NOT NULL DEFAULT '{}', "
                        + "initiative_id TEXT, "
                        + "workspace_id TEXT, "
                        + "status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'answered', 'denied', 'expired', 'withdrawn')), "
                        + "deadline INTEGER NOT NULL, "
                        + "created_at INTEGER NOT NULL DEFAULT (unixepoch()), "
                        + "sla_tier TEXT NOT NULL DEFAULT 'standard' CHECK (sla_tier IN ('critical', 'standard', 'bulk')), "
                        + "escalation TEXT, "
                        + "expiry_behavior TEXT NOT NULL DEFAULT 'deny' CHECK (expiry_behavior IN ('deny', 'escalate', 'reassign')), "
                        + "responded_at INTEGER, "
                        + "quorum_required INTEGER NOT NULL DEFAULT 1 CHECK (quorum_required >= 1), "
                        + "responses TEXT NOT NULL DEFAULT '[]', "
                        + "collapsed_count INTEGER NOT NULL DEFAULT 0, "
                        + "updated_at INTEGER NOT NULL DEFAULT (unixepoch()), "
                        + "FOREIGN KEY (initiative_id) REFERENCES initiatives(id) ON DELETE SET NULL, "
                        + "FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL)");
        statements.add(
                "INSERT INTO " + ASK_TABLE_STAGING + " (id, kind, \"from\", \"to\", payload, initiative_id, "
                        + "workspace_id, status, deadline, created_at, sla_tier, escalation, expiry_behavior, "
                        + "responded_at, quorum_required, responses, collapsed_count, updated_at) "
                        + "SELECT id, kind, \"from\", \"to\", payload, initiative_id, workspace_id, status, deadline, "
                        + "created_at, sla_tier, escalation, expiry_behavior, responded_at, quorum_required, responses, "
                        + "collapsed_count, updated_at FROM " + ASK_TABLE);
        statements.add("DROP TABLE " + ASK_TABLE);
        statements.add("ALTER TABLE " + ASK_TABLE_STAGING + " RENAME TO " + ASK_TABLE);
        // The indexes are dropped with the old table and recreated against the new one.
        statements.add("CREATE INDEX IF NOT EXISTS idx_asks_deadline ON asks(deadline)");
        statements.add("CREATE INDEX IF NOT EXISTS idx_asks_status ON asks(status)");
        statements.add("CREATE INDEX IF NOT EXISTS idx_asks_to_status ON asks(\"to\", status)");
        statements.add("CREATE INDEX IF NOT EXISTS idx_asks_initiative ON asks(initiative_id)");
        statements.add("CREATE INDEX IF NOT EXISTS idx_asks_workspace ON asks(workspace_id)");
        return statements;
    }

    /**
     * Widen the {@code asks} status vocabulary when the deployed table predates
     * the {@code denied} terminal.
     *
     * @return true when a migration ran
     */
    public static boolean migrateAskStatusVocabulary(JdbcTemplate jdbcTemplate) {
        String createSql;
        try {
            createSql = jdbcTemplate.queryForObject(
                    "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?",
                    String.class, ASK_TABLE);
        } catch (Exception e) {
            // No sqlite_master (a non-SQLite database) or no asks table — nothing to do.
            log.debug("Ask status migration skipped: {}", e.getMessage());
            return false;
        }
        if (createSql == null) {
            log.debug("Ask status migration skipped: no asks table");
            return false;
        }
        if (askStatusAllowsDenied(createSql)) {
            return false;
        }
        log.info("Widening the asks status vocabulary to include 'denied' (DAT-080, ASK-050)");
        List<String> statements = askDeniedStatusMigrationStatements();
        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (java.sql.Statement statement = connection.createStatement()) {
                for (String sql : statements) {
                    statement.execute(sql);
                }
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
            return null;
        });
        log.info("asks status vocabulary widened to include 'denied'");
        return true;
    }
}
