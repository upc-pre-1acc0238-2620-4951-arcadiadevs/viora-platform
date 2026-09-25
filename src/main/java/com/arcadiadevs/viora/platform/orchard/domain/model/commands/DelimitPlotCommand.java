package com.arcadiadevs.viora.platform.orchard.domain.model.commands;

/**
 * Command to delimit and register a new orchard plot.
 *
 * @param producerId   the managing producer identifier
 * @param name         the descriptive plot name
 * @param variety      the botanical variety
 * @param geometry     the cadastral polygon GeoJSON string
 * @param rowSpacingM  the distance between rows in meters
 * @param treeSpacingM the distance between trees in meters
 */
public record DelimitPlotCommand(
        String producerId,
        String name,
        String variety,
        String geometry,
        Double rowSpacingM,
        Double treeSpacingM
) {

    /**
     * Constructor verifying mandatory non-null command attributes using domain i18n keys.
     *
     * @param producerId   the managing producer identifier
     * @param name         the descriptive plot name
     * @param variety      the botanical variety
     * @param geometry     the cadastral polygon GeoJSON string
     * @param rowSpacingM  the distance between rows in meters
     * @param treeSpacingM the distance between trees in meters
     * @throws IllegalArgumentException if any mandatory argument is null
     */
    public DelimitPlotCommand {
        if (producerId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
        if (name == null) {
            throw new IllegalArgumentException("plot.name.blank");
        }
        if (variety == null) {
            throw new IllegalArgumentException("plot.variety.blank");
        }
        if (geometry == null) {
            throw new IllegalArgumentException("plot.geometry.empty");
        }
        if (rowSpacingM == null || treeSpacingM == null) {
            throw new IllegalArgumentException("plot.spacing.positive");
        }
    }
}
