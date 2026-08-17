package com.summa.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class SchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public SchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("schema.sql");
            String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            List<String> statements = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            
            for (String line : sql.split(";")) {
                current.append(line).append(";");
                String trimmed = current.toString().trim();
                if (!trimmed.isEmpty() && trimmed.endsWith(";")) {
                    statements.add(trimmed);
                    current = new StringBuilder();
                }
            }
            
            for (String statement : statements) {
                String s = statement.trim();
                if (!s.isEmpty() && !s.startsWith("--")) {
                    try {
                        jdbcTemplate.execute(s);
                    } catch (Exception e) {
                        if (!e.getMessage().contains("already exists")) {
                            System.err.println("Schema init warning: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
}
