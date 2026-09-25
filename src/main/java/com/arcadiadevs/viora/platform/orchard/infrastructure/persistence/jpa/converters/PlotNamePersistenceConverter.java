package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.converters;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotName;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA attribute converter mapping {@link PlotName} to a database {@link String}.
 */
@Converter(autoApply = true)
public class PlotNamePersistenceConverter implements AttributeConverter<PlotName, String> {

    @Override
    public String convertToDatabaseColumn(PlotName attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public PlotName convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new PlotName(dbData);
    }
}
