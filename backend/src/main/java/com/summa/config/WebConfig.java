package com.summa.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.summa.util.InstantSerializers;

import java.time.Instant;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Additional CORS origins (comma-separated). Defaults cover local dev;
     * production deployments fronting an external API host set e.g.
     * {@code SUMMA_CORS_ORIGINS=https://app.example.com}.
     */
    @Value("${summa.cors.origins:}")
    private String extraCorsOrigins = "";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // The servlet context path is /api (server.servlet.context-path), and
        // Spring MVC matches CORS mappings against the *servlet-relative* path,
        // so the registry key is /** — not /api/**, which would never match.
        var mapping = registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "https://localhost:*", "http://127.0.0.1:*", "https://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
        if (extraCorsOrigins != null && !extraCorsOrigins.isBlank()) {
            for (String origin : extraCorsOrigins.split(",")) {
                String o = origin.trim();
                if (!o.isEmpty() && !o.contains("*")) {
                    // Production origins must be HTTPS; localhost dev may use HTTP.
                    if (o.startsWith("http://localhost") || o.startsWith("http://127.0.0.1")) {
                        mapping.allowedOriginPatterns(o);
                    } else if (o.startsWith("https://")) {
                        mapping.allowedOriginPatterns(o);
                    }
                }
            }
        }
    }

    /**
     * Customizes Spring Boot's auto-configured ObjectMapper to register
     * the shared Instant serializers/deserializers, avoiding the creation
     * of a second ObjectMapper instance (which would duplicate the module
     * already present in JsonHelpers' static fallback mapper).
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer instantEpochSecondsCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("InstantAsEpochSeconds");
            module.addSerializer(Instant.class, new InstantSerializers.InstantEpochSecondSerializer());
            module.addDeserializer(Instant.class, new InstantSerializers.InstantEpochSecondDeserializer());
            builder.modules(module);
        };
    }
}
