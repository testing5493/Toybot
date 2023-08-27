package com.jagrosh.vortex.hibernate.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;

@Converter
public class PreciseToSecondInstantConverter implements AttributeConverter<Instant, Long> {
    @Override
    public Long convertToDatabaseColumn(Instant attribute) {
        return attribute.getEpochSecond();
    }

    @Override
    public Instant convertToEntityAttribute(Long dbData) {
        return Instant.ofEpochSecond(dbData);
    }
}
