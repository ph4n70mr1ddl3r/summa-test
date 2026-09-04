package com.summa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
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
            String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            List<String> statements = parseSqlStatements(sql);
            
            for (String statement : statements) {
                String s = statement.trim();
                if (!s.isEmpty() && !s.startsWith("--")) {
                    try {
                        jdbcTemplate.execute(s);
                    } catch (Exception e) {
                        String msg = e.getMessage();
                        if (msg != null && (msg.contains("already exists") || msg.contains("table") && msg.contains("exists"))) {
                            // Expected — schema already initialized
                        } else {
                            log.warn("Schema init warning: {}", msg);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    /**
     * Parse SQL into statements:
     * <ul>
     *   <li>strips {@code --} line comments (outside string literals), so a
     *       comment preceding a statement no longer discards that statement;</li>
     *   <li>respects single-quoted string literals (incl. {@code ''} escapes);</li>
     *   <li>does not split inside {@code CREATE TRIGGER ... BEGIN ... END}
     *       bodies, whose inner {@code ;} previously shattered every trigger
     *       into an "incomplete input" fragment plus a stray {@code END}
     *       (both were swallowed as warnings, leaving zero triggers).</li>
     * </ul>
     */
    private List<String> parseSqlStatements(String sql) {
        String withoutComments = stripLineComments(sql);
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;

        for (int i = 0; i < withoutComments.length(); i++) {
            char c = withoutComments.charAt(i);

            if (c == '\'' && !inSingleQuote) {
                inSingleQuote = true;
                current.append(c);
            } else if (c == '\'' && inSingleQuote) {
                // Check for escaped quote ''
                if (i + 1 < withoutComments.length() && withoutComments.charAt(i + 1) == '\'') {
                    current.append(c).append(withoutComments.charAt(++i));
                } else {
                    inSingleQuote = false;
                    current.append(c);
                }
            } else if (c == ';' && !inSingleQuote && !isInsideTriggerBody(current)) {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current = new StringBuilder();
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
     * Remove {@code --} comments running to end-of-line, ignoring occurrences
     * inside single-quoted string literals.
     */
    private String stripLineComments(String sql) {
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
            } else if (c == '-' && !inSingleQuote && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                // Skip to end of line (keep the newline itself).
                while (i < sql.length() && sql.charAt(i) != '\n') {
                    i++;
                }
                if (i < sql.length()) {
                    out.append('\n');
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * True while the accumulated buffer is inside a trigger body, i.e. it
     * opened a {@code BEGIN} that has not yet been closed by {@code END}.
     * Whole-word match so column names like {@code weekend} don't count.
     */
    private boolean isInsideTriggerBody(StringBuilder buffer) {
        String upper = buffer.toString().toUpperCase();
        int begins = countWord(upper, "BEGIN");
        if (begins == 0) {
            return false;
        }
        int ends = countWord(upper, "END");
        return begins > ends;
    }

    private int countWord(String text, String word) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(word, idx)) >= 0) {
            boolean leftOk = idx == 0 || !isWordChar(text.charAt(idx - 1));
            int after = idx + word.length();
            boolean rightOk = after >= text.length() || !isWordChar(text.charAt(after));
            if (leftOk && rightOk) {
                count++;
            }
            idx = after;
        }
        return count;
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
