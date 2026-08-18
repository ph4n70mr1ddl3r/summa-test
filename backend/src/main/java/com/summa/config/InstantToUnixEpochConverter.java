package com.summa.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;

@Converter
public class InstantToUnixEpochConverter implements AttributeConverter<Instant, Long> {

    @Override
    public Long convertToDatabaseColumn(Instant attribute) {
        return attribute != null ? attribute.getEpochSecond() : null;
    }

    @Override
    public Instant convertToEntityAttribute(Long dbData) {
        return dbData != null ? Instant.ofEpochSecond(dbData) : null;
    }
}
