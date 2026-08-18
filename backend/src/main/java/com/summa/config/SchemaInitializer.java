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
                        if (!e.getMessage().contains("already exists")) {
                            log.warn("Schema init warning: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    /**
     * Parse SQL into statements respecting string literals (single quotes) so that
     * semicolons inside FTS5 expressions and trigger bodies are not split.
     */
    private List<String> parseSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            
            if (c == '\'' && !inSingleQuote) {
                inSingleQuote = true;
                current.append(c);
            } else if (c == '\'' && inSingleQuote) {
                // Check for escaped quote ''
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    current.append(c).append(sql.charAt(++i));
                } else {
                    inSingleQuote = false;
                    current.append(c);
                }
            } else if (c == ';' && !inSingleQuote) {
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
}
