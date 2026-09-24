package com.summa.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

@TestConfiguration
public class TestDatabaseConfig {

    @Bean
    public JdbcTemplate jdbcTemplate(EmbeddedDatabaseBuilder builder) {
        return new JdbcTemplate(builder
                .setType(EmbeddedDatabaseType.H2)
                .setName("summatest")
                .build());
    }
}
