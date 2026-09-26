package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.converters;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

/**
 * JPA attribute converter mapping domain {@link PlotId} to database {@link UUID}.
 */
@Converter(autoApply = true)
public class PlotIdPersistenceConverter implements AttributeConverter<PlotId, UUID> {

    @Override
    public UUID convertToDatabaseColumn(PlotId attribute) {
        return attribute == null ? null : UUID.fromString(attribute.plotId());
    }

    @Override
    public PlotId convertToEntityAttribute(UUID dbData) {
        return dbData == null ? null : new PlotId(dbData.toString());
    }
}
