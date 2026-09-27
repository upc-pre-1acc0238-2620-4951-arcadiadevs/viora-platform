package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.converters;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.SensorDepth;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA attribute converter mapping domain {@link SensorDepth} to database {@link Integer}.
 */
@Converter(autoApply = true)
public class SensorDepthPersistenceConverter implements AttributeConverter<SensorDepth, Integer> {

    @Override
    public Integer convertToDatabaseColumn(SensorDepth attribute) {
        return attribute == null ? null : attribute.depthCm();
    }

    @Override
    public SensorDepth convertToEntityAttribute(Integer dbData) {
        return dbData == null ? SensorDepth.none() : SensorDepth.of(dbData);
    }
}
