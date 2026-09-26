package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.converters;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

/**
 * JPA attribute converter mapping domain {@link DeviceId} to database {@link UUID}.
 */
@Converter(autoApply = true)
public class DeviceIdPersistenceConverter implements AttributeConverter<DeviceId, UUID> {

    @Override
    public UUID convertToDatabaseColumn(DeviceId attribute) {
        return attribute == null ? null : UUID.fromString(attribute.deviceId());
    }

    @Override
    public DeviceId convertToEntityAttribute(UUID dbData) {
        return dbData == null ? null : new DeviceId(dbData.toString());
    }
}
