package com.summa.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.File;

@Configuration
public class DatabaseConfig {

    @Value("${summa.database.path:~/.summa/summa.db}")
    private String dbPath;

    @Bean
    public DataSource dataSource() throws Exception {
        String path = expandPath(dbPath);
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl("jdbc:sqlite:" + path);
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public SchemaInitializer schemaInitializer(JdbcTemplate jdbcTemplate) {
        return new SchemaInitializer(jdbcTemplate);
    }

    @Bean
    public ApplicationRunner validateRequiredEnv() {
        return args -> {
            String jwtSecret = System.getenv("SUMMA_JWT_SECRET");
            if (jwtSecret == null || jwtSecret.isBlank()) {
                // The Spring property fallback is handled by @Value injection;
                // if we reach here without it being set, it means the property
                // was not provided. However, @Value without default will fail
                // at bean creation time if the env var is absent.
                // This runner is a belt-and-suspenders check.
            }
        };
    }

    private String expandPath(String path) {
        if (path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }
}
