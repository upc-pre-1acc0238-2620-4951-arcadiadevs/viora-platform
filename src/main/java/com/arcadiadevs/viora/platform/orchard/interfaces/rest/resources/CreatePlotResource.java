package com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Resource for creating and delimiting an orchard plot.
 */
@Schema(
        name = "CreatePlotResource",
        description = "Request payload for creating and delimiting a new orchard plot",
        example = "{\"producerId\": \"550e8400-e29b-41d4-a716-446655440000\", \"name\": \"Cuartel San Jerónimo\", \"variety\": \"CRIOLLA\", \"polygonGeoJson\": \"{\\\"type\\\":\\\"Polygon\\\",\\\"coordinates\\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}\", \"rowSpacingM\": 7.0, \"treeSpacingM\": 5.0}"
)
public record CreatePlotResource(
        @NotBlank(message = "Producer id cannot be blank")
        @Schema(description = "Managing producer UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String producerId,

        @NotBlank(message = "Plot name cannot be blank")
        @Size(min = 3, max = 100, message = "Plot name must be between 3 and 100 characters")
        @Schema(description = "Descriptive name of the orchard plot", example = "Cuartel San Jerónimo", minLength = 3, maxLength = 100)
        String name,

        @NotBlank(message = "Variety cannot be blank")
        @Schema(description = "Botanical olive variety", example = "CRIOLLA", allowableValues = {"CRIOLLA", "SEVILLANA", "MANZANILLA", "ARBEQUINA"})
        String variety,

        @NotBlank(message = "Polygon GeoJSON cannot be blank")
        @Schema(description = "GeoJSON polygon geometry in WGS84 coordinates", example = "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}")
        String polygonGeoJson,

        @NotNull(message = "Row spacing cannot be null")
        @Positive(message = "Row spacing must be strictly greater than 0")
        @Schema(description = "Row spacing in meters", example = "7.0")
        Double rowSpacingM,

        @NotNull(message = "Tree spacing cannot be null")
        @Positive(message = "Tree spacing must be strictly greater than 0")
        @Schema(description = "Tree spacing in meters", example = "5.0")
        Double treeSpacingM
) {
}
