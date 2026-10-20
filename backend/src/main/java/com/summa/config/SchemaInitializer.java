package com.summa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class SchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("schema.sql");
            String sql;
            try (InputStream is = resource.getInputStream()) {
                sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            List<String> statements = parseSqlStatements(sql);

            for (String statement : statements) {
                String s = statement.trim();
                if (!s.isEmpty()) {
                    try {
                        jdbcTemplate.execute(s);
                    } catch (Exception e) {
                        String msg = e.getMessage();
                        // Idempotent re-run: swallow "already exists" only for the
                        // DDL shapes that actually use IF NOT EXISTS. Anything else
                        // failing with that message is a real defect — rethrow.
                        if (msg != null && msg.toLowerCase().contains("already exists")
                                && isIdempotentCreate(s)) {
                            // Expected - schema already initialized
                        } else {
                            log.error("Schema init failure: {}", msg);
                            throw new RuntimeException("Schema initialization failed: " + msg, e);
                        }
                    }
                }
            }

            // schema.sql is applied with CREATE TABLE IF NOT EXISTS, so a widened
            // CHECK constraint never reaches a database created before it. The
            // migrations rebuild the affected table (DAT-080's ask status vocabulary).
            SchemaMigrations.migrateAskStatusVocabulary(jdbcTemplate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    /**
     * Only CREATE ... IF NOT EXISTS shapes may legitimately fail with
     * "already exists" on an idempotent re-run; every other statement that
     * carries that message signals a broken schema and must not be swallowed.
     */
    private static boolean isIdempotentCreate(String statement) {
        String upper = statement.trim().toUpperCase();
        return upper.startsWith("CREATE TABLE IF NOT EXISTS")
            || upper.startsWith("CREATE UNIQUE INDEX IF NOT EXISTS")
            || upper.startsWith("CREATE INDEX IF NOT EXISTS")
            || upper.startsWith("CREATE TRIGGER IF NOT EXISTS")
            || upper.startsWith("CREATE VIRTUAL TABLE IF NOT EXISTS");
    }

    /**
     * Parse SQL into statements:
     * <ul>
     *   <li>strips {@code --} line comments and C-style block comments
     *       (outside string literals), so a comment preceding a statement no
     *       longer discards that statement;</li>
     *   <li>respects single-quoted string literals (incl. {@code ''} escapes);</li>
     *   <li>tracks CREATE TRIGGER ... BEGIN ... END state to avoid splitting on
     *       semicolons inside trigger bodies.</li>
     * </ul>
     * <p>
     * A trigger body is delimited by an outer {@code BEGIN} / matching
     * {@code END}; {@code END} keywords inside the body that close a CASE
     * expression are handled by decrementing a CASE depth counter first, so
     * only the final outer END closes the trigger.
     */
    private List<String> parseSqlStatements(String sql) {
        String withoutComments = stripComments(sql);
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        int triggerDepth = 0;
        int caseDepth = 0;

        for (int i = 0; i < withoutComments.length(); i++) {
            char c = withoutComments.charAt(i);

            if (c == '\'' && !inSingleQuote) {
                inSingleQuote = true;
                current.append(c);
            } else if (c == '\'' && inSingleQuote) {
                if (i + 1 < withoutComments.length() && withoutComments.charAt(i + 1) == '\'') {
                    current.append(c).append(withoutComments.charAt(++i));
                } else {
                    inSingleQuote = false;
                    current.append(c);
                }
            } else if (!inSingleQuote && isWordBoundaryMatch(withoutComments, i, "BEGIN")) {
                current.append("BEGIN");
                triggerDepth++;
                i += 4; // skip past "EGIN" (already appended "BEGIN")
            } else if (!inSingleQuote && isWordBoundaryMatch(withoutComments, i, "CASE")) {
                current.append("CASE");
                caseDepth++;
                i += 3; // skip past "ASE" (already appended "CASE")
            } else if (!inSingleQuote && isWordBoundaryMatch(withoutComments, i, "END")) {
                if (caseDepth > 0) {
                    caseDepth--; // closes a CASE expression
                } else if (triggerDepth > 0) {
                    triggerDepth--; // closes the trigger body
                }
                current.append("END");
                i += 2; // skip past "ND" (already appended "END")
            } else if (c == ';' && triggerDepth == 0) {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current = new StringBuilder();
                caseDepth = 0;
            } else {
                current.append(c);
            }
        }

        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            statements.add(remaining);
        }

        return statements;
    }

    /**
     * Remove both {@code --} line comments and C-style block comments,
     * ignoring occurrences inside single-quoted string literals.
     */
    private String stripComments(String sql) {
        StringBuilder out = new StringBuilder(sql.length());
        boolean inSingleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'' && !inSingleQuote) {
                inSingleQuote = true;
                out.append(c);
            } else if (c == '\'' && inSingleQuote) {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    out.append(c).append(sql.charAt(++i));
                } else {
                    inSingleQuote = false;
                    out.append(c);
                }
            } else if (!inSingleQuote && c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                // Skip to end of line (keep the newline itself).
                while (i < sql.length() && sql.charAt(i) != '\n') {
                    i++;
                }
                if (i < sql.length()) {
                    out.append('\n');
                }
            } else if (!inSingleQuote && c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < sql.length() && !(sql.charAt(i) == '*' && sql.charAt(i + 1) == '/')) {
                    i++;
                }
                if (i + 1 >= sql.length()) {
                    throw new RuntimeException("Unclosed block comment in schema SQL");
                }
                i++; // skip '/'
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private boolean isWordBoundaryMatch(String text, int offset, String word) {
        if (offset + word.length() > text.length()) return false;
        for (int j = 0; j < word.length(); j++) {
            if (text.charAt(offset + j) != word.charAt(j)) return false;
        }
        boolean leftOk = offset == 0 || !isWordChar(text.charAt(offset - 1));
        int after = offset + word.length();
        boolean rightOk = after >= text.length() || !isWordChar(text.charAt(after));
        return leftOk && rightOk;
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
