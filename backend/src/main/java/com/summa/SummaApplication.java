package com.summa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SummaApplication {
    private static final Logger log = LoggerFactory.getLogger(SummaApplication.class);
    private static final int MIN_JWT_SECRET_BYTES = 32;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SummaApplication.class);
        app.addListeners(new ApplicationListener<ApplicationPreparedEvent>() {
            @Override
            public void onApplicationEvent(ApplicationPreparedEvent event) {
                validateStartupConfig(event.getApplicationContext().getEnvironment());
            }
        });
        app.addListeners(new ApplicationListener<ApplicationFailedEvent>() {
            @Override
            public void onApplicationEvent(ApplicationFailedEvent event) {
                log.error("Application failed to start: {}", event.getException().getMessage());
            }
        });
        app.run(args);
    }

    private static void validateStartupConfig(Environment env) {
        String jwtSecret = env.getProperty("summa.auth.jwt-secret");
        if (jwtSecret == null || jwtSecret.isBlank()) {
            log.error("FATAL: summa.auth.jwt-secret is not configured. Set SUMMA_JWT_SECRET environment variable.");
            throw new IllegalStateException("SUMMA_JWT_SECRET environment variable is required");
        }
        byte[] secretBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_JWT_SECRET_BYTES) {
            log.error("FATAL: summa.auth.jwt-secret must be at least {} bytes ({} bits). Found {} bytes.",
                    MIN_JWT_SECRET_BYTES, MIN_JWT_SECRET_BYTES * 8, secretBytes.length);
            throw new IllegalStateException(
                "SUMMA_JWT_SECRET must be at least " + (MIN_JWT_SECRET_BYTES * 8) + " bits ("
                + MIN_JWT_SECRET_BYTES + " bytes). Generate with: openssl rand -hex " + MIN_JWT_SECRET_BYTES);
        }
        log.info("JWT secret validated: {} bits", secretBytes.length * 8);
    }
}
