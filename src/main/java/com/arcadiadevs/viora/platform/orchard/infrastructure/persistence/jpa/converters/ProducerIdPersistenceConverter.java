package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.converters;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

/**
 * JPA attribute converter mapping {@link ProducerId} to a database {@link UUID}.
 */
@Converter(autoApply = true)
public class ProducerIdPersistenceConverter implements AttributeConverter<ProducerId, UUID> {

    @Override
    public UUID convertToDatabaseColumn(ProducerId attribute) {
        return attribute == null ? null : UUID.fromString(attribute.producerId());
    }

    @Override
    public ProducerId convertToEntityAttribute(UUID dbData) {
        return dbData == null ? null : new ProducerId(dbData.toString());
    }
}
