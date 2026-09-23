package com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.configuration.strategy;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import java.util.Locale;

import static io.github.encryptorcode.pluralize.Pluralize.pluralize;

/**
 * Hibernate physical naming strategy that converts identifiers to snake case.
 * Table names are also pluralized before snake-case conversion.
 *
 * @since 1.0
 * @see org.hibernate.boot.model.naming.PhysicalNamingStrategy
 */
public class SnakeCasePhysicalNamingStrategy implements PhysicalNamingStrategy {

    /**
     * Converts the Catalog Name to Snake Case
     * @param identifier catalog name identifier
     * @param jdbcEnvironment JDBC environment
     * @return Snake Case Catalog Name
     */
    @Override
    public Identifier toPhysicalCatalogName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return this.toSnakeCase(identifier);
    }

    /**
     * Converts the Schema Name to Snake Case
     * @param identifier schema name identifier
     * @param jdbcEnvironment jdbc environment
     * @return Snake Case Schema Name
     */
    @Override
    public Identifier toPhysicalSchemaName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return this.toSnakeCase(identifier);
    }

    /**
     * Converts the Table Name to Snake Case and Pluralizes it
     * @param identifier table name
     * @param jdbcEnvironment jdbc environment
     * @return Snake Case and Pluralized Table Name
     */
    @Override
    public Identifier toPhysicalTableName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return this.toSnakeCase(this.toPlural(identifier));
    }
    
    /**
     * Converts the Sequence Name to Snake Case
     * @param identifier sequence name
     * @param jdbcEnvironment jdbc environment
     * @return Snake Case Sequence Name
     */
    @Override
    public Identifier toPhysicalSequenceName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return this.toSnakeCase(identifier);
    }

    /**
     * Converts the Column Name to Snake Case
     * @param identifier column name
     * @param jdbcEnvironment jdbc environment
     * @return Snake Case Column Name
     */
    @Override
    public Identifier toPhysicalColumnName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return this.toSnakeCase(identifier);
    }

    /**
     * Converts the Identifier to Snake Case
     * @param identifier object identifier
     * @return Snake Case Identifier
     */
    private Identifier toSnakeCase(final Identifier identifier) {
        if (identifier == null) {
            return null;
        }

        final String regex = "([a-z])([A-Z])";
        final String replacement = "$1_$2";
        final String newName = identifier.getText()
                .replaceAll(regex, replacement)
                .toLowerCase(Locale.ROOT);
        return Identifier.toIdentifier(newName);
    }

    /**
     * Pluralizes the Identifier
     * @param identifier object identifier
     * @return Pluralized Identifier
     */
    private Identifier toPlural(final Identifier identifier) {
        if (identifier == null) {
            return null;
        }
        final String newName = pluralize(identifier.getText());
        return Identifier.toIdentifier(newName);
    }

}
