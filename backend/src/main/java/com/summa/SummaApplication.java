package com.summa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    public static final int DEFAULT_MIN_JWT_SECRET_LENGTH = 32;

    @Value("${summa.auth.jwt-secret-min-length:" + DEFAULT_MIN_JWT_SECRET_LENGTH + "}")
    private int minJwtSecretLength;

    public int getMinJwtSecretLength() {
        return minJwtSecretLength;
    }

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
            log.error("FATAL: summa.auth.jwt-secret is not configured. Set SUMMA_AUTH_JWT_SECRET environment variable.");
            throw new IllegalStateException("SUMMA_AUTH_JWT_SECRET environment variable is required");
        }
        int minLen = env.getProperty("summa.auth.jwt-secret-min-length", int.class, DEFAULT_MIN_JWT_SECRET_LENGTH);
        if (jwtSecret.length() < minLen) {
            log.error("FATAL: summa.auth.jwt-secret must be at least {} characters ({} bits recommended). Found {} characters.",
                    minLen, minLen * 8, jwtSecret.length());
            throw new IllegalStateException(
                "SUMMA_AUTH_JWT_SECRET must be at least " + minLen + " characters ("
                + minLen * 8 + " bits). Generate with: openssl rand -hex " + minLen);
        }
        log.info("JWT secret validated: {} characters (min: {})", jwtSecret.length(), minLen);
    }
}
