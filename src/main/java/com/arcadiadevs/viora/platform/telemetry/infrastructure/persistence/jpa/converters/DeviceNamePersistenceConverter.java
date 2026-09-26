package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.converters;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceName;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA attribute converter mapping domain {@link DeviceName} to database {@link String}.
 */
@Converter(autoApply = true)
public class DeviceNamePersistenceConverter implements AttributeConverter<DeviceName, String> {

    @Override
    public String convertToDatabaseColumn(DeviceName attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public DeviceName convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new DeviceName(dbData);
    }
}
