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
                if (!s.isEmpty() && !s.startsWith("--")) {
                    try {
                        jdbcTemplate.execute(s);
                    } catch (Exception e) {
                        String msg = e.getMessage();
                        if (msg != null && msg.toLowerCase().contains("already exists")) {
                            // Expected - schema already initialized
                        } else {
                            log.error("Schema init failure: {}", msg);
                            throw new RuntimeException("Schema initialization failed: " + msg, e);
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
     *   <li>strips {@code --} line comments and C-style block comments
     *       (outside string literals), so a comment preceding a statement no
     *       longer discards that statement;</li>
     *   <li>respects single-quoted string literals (incl. {@code ''} escapes);</li>
     *   <li>tracks CREATE TRIGGER ... BEGIN ... END state to avoid splitting on
     *       semicolons inside trigger bodies.</li>
     * </ul>
     */
    private List<String> parseSqlStatements(String sql) {
        String withoutComments = stripComments(sql);
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        int triggerDepth = 0;

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
                current.append(c);
                triggerDepth++;
                i += 3; // skip "EGIN"
            } else if (!inSingleQuote && isWordBoundaryMatch(withoutComments, i, "END")) {
                if (triggerDepth > 0) {
                    triggerDepth--;
                }
                current.append(c);
                i += 2; // skip "ND"
            } else if (c == ';' && triggerDepth == 0) {
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
                if (i + 1 < sql.length()) i++; // skip '/'
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
