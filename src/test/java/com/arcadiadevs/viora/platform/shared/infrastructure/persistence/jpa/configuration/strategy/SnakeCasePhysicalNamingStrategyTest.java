package com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.configuration.strategy;

import org.hibernate.boot.model.naming.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SnakeCasePhysicalNamingStrategyTest {

    private SnakeCasePhysicalNamingStrategy namingStrategy;

    @BeforeEach
    void setUp() {
        namingStrategy = new SnakeCasePhysicalNamingStrategy();
    }

    @Test
    void toPhysicalTableNameConvertsToSnakeCaseAndPluralizes() {
        Identifier tableName = Identifier.toIdentifier("DeviceMetric");
        Identifier physicalTableName = namingStrategy.toPhysicalTableName(tableName, null);

        assertEquals("device_metrics", physicalTableName.getText());
    }

    @Test
    void toPhysicalTableNameHandlesSimpleEntityNames() {
        Identifier tableName = Identifier.toIdentifier("Measurement");
        Identifier physicalTableName = namingStrategy.toPhysicalTableName(tableName, null);

        assertEquals("measurements", physicalTableName.getText());
    }

    @Test
    void toPhysicalColumnNameConvertsCamelCaseToSnakeCase() {
        Identifier columnName = Identifier.toIdentifier("createdAt");
        Identifier physicalColumnName = namingStrategy.toPhysicalColumnName(columnName, null);

        assertEquals("created_at", physicalColumnName.getText());
    }

    @Test
    void toPhysicalColumnNameHandlesMultiWordIdentifiers() {
        Identifier columnName = Identifier.toIdentifier("measuredDecimalValue");
        Identifier physicalColumnName = namingStrategy.toPhysicalColumnName(columnName, null);

        assertEquals("measured_decimal_value", physicalColumnName.getText());
    }

    @Test
    void toPhysicalCatalogAndSchemaNamesConvertToSnakeCase() {
        Identifier catalog = Identifier.toIdentifier("coreCatalog");
        Identifier schema = Identifier.toIdentifier("coreSchema");

        assertEquals("core_catalog", namingStrategy.toPhysicalCatalogName(catalog, null).getText());
        assertEquals("core_schema", namingStrategy.toPhysicalSchemaName(schema, null).getText());
    }

    @Test
    void toPhysicalSequenceNameConvertsToSnakeCase() {
        Identifier sequence = Identifier.toIdentifier("deviceMetricSeq");
        assertEquals("device_metric_seq", namingStrategy.toPhysicalSequenceName(sequence, null).getText());
    }

    @Test
    void handlesNullIdentifiersGracefully() {
        assertNull(namingStrategy.toPhysicalTableName(null, null));
        assertNull(namingStrategy.toPhysicalColumnName(null, null));
        assertNull(namingStrategy.toPhysicalCatalogName(null, null));
        assertNull(namingStrategy.toPhysicalSchemaName(null, null));
        assertNull(namingStrategy.toPhysicalSequenceName(null, null));
    }
}
