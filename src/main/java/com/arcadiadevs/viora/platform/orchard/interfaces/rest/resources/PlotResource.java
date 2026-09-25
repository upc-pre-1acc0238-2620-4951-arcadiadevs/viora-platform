package com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * Public REST resource representing an olive orchard plot.
 *
 * @param id              the unique identifier of the plot
 * @param producerId      the managing producer identifier
 * @param name            the descriptive plot name
 * @param variety         the olive botanical variety
 * @param areaHa          the surface area in hectares
 * @param treeDensity     the tree density count per hectare
 * @param rowSpacingM     the distance between rows in meters
 * @param treeSpacingM    the distance between trees in meters
 * @param polygonGeoJson  the cadastral polygon in GeoJSON format
 * @param lastPruningDate optional date of last pruning
 * @param status          the lifecycle status
 * @param revision        the optimistic concurrency revision
 */
@Schema(
        name = "PlotResource",
        description = "Response resource representing an orchard plot",
        example = "{\"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"producerId\": \"550e8400-e29b-41d4-a716-446655440000\", \"name\": \"Cuartel San Jerónimo\", \"variety\": \"CRIOLLA\", \"areaHa\": 1.25, \"treeDensity\": 286, \"rowSpacingM\": 7.0, \"treeSpacingM\": 5.0, \"polygonGeoJson\": \"{\\\"type\\\":\\\"Polygon\\\",\\\"coordinates\\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}\", \"lastPruningDate\": null, \"status\": \"ACTIVE\", \"revision\": 0}"
)
@NullMarked
public record PlotResource(
        @Schema(description = "Unique plot identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String id,

        @Schema(description = "Managing producer identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        String producerId,

        @Schema(description = "Descriptive name of the orchard plot", example = "Cuartel San Jerónimo")
        String name,

        @Schema(description = "Botanical olive variety", example = "CRIOLLA", allowableValues = {"CRIOLLA", "SEVILLANA", "MANZANILLA", "ARBEQUINA"})
        String variety,

        @Schema(description = "Surface area in hectares", example = "1.25")
        Double areaHa,

        @Schema(description = "Calculated tree density per hectare", example = "286")
        Integer treeDensity,

        @Schema(description = "Row spacing in meters", example = "7.0")
        Double rowSpacingM,

        @Schema(description = "Tree spacing in meters", example = "5.0")
        Double treeSpacingM,

        @Schema(description = "GeoJSON polygon geometry in WGS84 coordinates", example = "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}")
        String polygonGeoJson,

        @Schema(description = "Date of last registered pruning labor", example = "2026-06-15")
        @Nullable LocalDate lastPruningDate,

        @Schema(description = "Lifecycle status of the plot", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
        String status,

        @Schema(description = "Optimistic locking revision number", example = "0")
        Long revision
) {
}
