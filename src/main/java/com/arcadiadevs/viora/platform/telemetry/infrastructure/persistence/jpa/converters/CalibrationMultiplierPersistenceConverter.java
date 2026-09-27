package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.converters;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.CalibrationMultiplier;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA attribute converter mapping domain {@link CalibrationMultiplier} to database {@link Double}.
 */
@Converter(autoApply = true)
public class CalibrationMultiplierPersistenceConverter implements AttributeConverter<CalibrationMultiplier, Double> {

    @Override
    public Double convertToDatabaseColumn(CalibrationMultiplier attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public CalibrationMultiplier convertToEntityAttribute(Double dbData) {
        return dbData == null ? CalibrationMultiplier.defaultMultiplier() : new CalibrationMultiplier(dbData);
    }
}
