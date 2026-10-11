package com.summa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.beans.factory.annotation.Value;
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
        var mapping = registry.addMapping("/api/**")
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

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("InstantAsEpochSeconds");
        module.addSerializer(Instant.class, new InstantSerializers.InstantEpochSecondSerializer());
        module.addDeserializer(Instant.class, new InstantSerializers.InstantEpochSecondDeserializer());
        mapper.registerModule(module);
        return mapper;
    }
}
