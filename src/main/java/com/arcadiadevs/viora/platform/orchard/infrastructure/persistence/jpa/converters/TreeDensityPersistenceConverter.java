package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.converters;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.TreeDensity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA attribute converter mapping {@link TreeDensity} to a database {@link Integer}.
 */
@Converter(autoApply = true)
public class TreeDensityPersistenceConverter implements AttributeConverter<TreeDensity, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TreeDensity attribute) {
        return attribute == null ? null : attribute.treesPerHectare();
    }

    @Override
    public TreeDensity convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : new TreeDensity(dbData);
    }
}
