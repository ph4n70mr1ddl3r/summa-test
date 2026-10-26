package com.summa.config;

import com.summa.model.ExpiryBehavior;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ExpiryBehaviorConverter implements AttributeConverter<ExpiryBehavior, String> {

    @Override
    public String convertToDatabaseColumn(ExpiryBehavior attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ExpiryBehavior convertToEntityAttribute(String dbData) {
        return ExpiryBehavior.fromValue(dbData);
    }
}
