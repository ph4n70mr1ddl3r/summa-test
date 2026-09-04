package com.summa.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.Instant;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Additional CORS origins (comma-separated). Defaults cover local dev;
     * production deployments fronting an external API host set e.g.
     * {@code SUMMA_CORS_ORIGINS=https://app.example.com}.
     */
    @org.springframework.beans.factory.annotation.Value("${summa.cors.origins:}")
    private String extraCorsOrigins = "";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var mapping = registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
        if (extraCorsOrigins != null && !extraCorsOrigins.isBlank()) {
            for (String origin : extraCorsOrigins.split(",")) {
                String o = origin.trim();
                if (!o.isEmpty()) {
                    mapping.allowedOrigins(o);
                }
            }
        }
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("InstantAsEpochSeconds");
        module.addSerializer(Instant.class, new InstantEpochSecondSerializer());
        module.addDeserializer(Instant.class, new InstantEpochSecondDeserializer());
        mapper.registerModule(module);
        return mapper;
    }

    static class InstantEpochSecondSerializer extends JsonSerializer<Instant> {
        @Override
        public void serialize(Instant value, JsonGenerator gen, com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeNumber(value.getEpochSecond());
            }
        }
    }

    static class InstantEpochSecondDeserializer extends JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            // Accept epoch seconds (what we write) and ISO-8601 strings
            // (what the console sends, e.g. DnaGoal window endpoints).
            switch (p.currentToken()) {
                case VALUE_NUMBER_INT:
                    return Instant.ofEpochSecond(p.getLongValue());
                case VALUE_STRING: {
                    String text = p.getText();
                    if (text == null || text.isBlank()) return null;
                    String t = text.trim();
                    try {
                        return Instant.parse(t);
                    } catch (java.time.DateTimeException e) {
                        try {
                            return Instant.ofEpochSecond(Long.parseLong(t));
                        } catch (NumberFormatException nfe) {
                            throw new IOException("Invalid Instant value: " + text, e);
                        }
                    }
                }
                case VALUE_NULL:
                    return null;
                default:
                    throw new IOException("Expected epoch-second number or ISO-8601 string for Instant, got " + p.currentToken());
            }
        }
    }
}
