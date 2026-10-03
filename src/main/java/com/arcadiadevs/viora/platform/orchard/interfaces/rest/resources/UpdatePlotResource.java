package com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Resource payload for updating the boundaries and attributes of an orchard plot.
 */
@Schema(
        name = "UpdatePlotResource",
        description = "Request payload for updating plot boundaries, frame, and pruning metadata",
        example = "{\"name\": \"Cuartel San Jerónimo Rectificado\", \"rowSpacingM\": 6.5, \"treeSpacingM\": 4.5, \"lastPruningDate\": \"2026-09-10\", \"polygonGeoJson\": \"{\\\"type\\\":\\\"Polygon\\\",\\\"coordinates\\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}\"}"
)
public record UpdatePlotResource(
        @NotBlank(message = "Plot name cannot be blank")
        @Size(min = 3, max = 100, message = "Plot name must be between 3 and 100 characters")
        @Schema(description = "Descriptive name of the orchard plot", example = "Cuartel San Jerónimo Rectificado", minLength = 3, maxLength = 100)
        String name,

        @NotNull(message = "Row spacing cannot be null")
        @Positive(message = "Row spacing must be strictly greater than 0")
        @Schema(description = "Row spacing in meters", example = "6.5")
        Double rowSpacingM,

        @NotNull(message = "Tree spacing cannot be null")
        @Positive(message = "Tree spacing must be strictly greater than 0")
        @Schema(description = "Tree spacing in meters", example = "4.5")
        Double treeSpacingM,

        @Schema(description = "Date of last pruning labor (ISO-8601)", example = "2026-09-10")
        LocalDate lastPruningDate,

        @NotBlank(message = "Polygon GeoJSON cannot be blank")
        @Schema(description = "GeoJSON polygon geometry in WGS84 coordinates", example = "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}")
        String polygonGeoJson,

        @Schema(description = "Corrected botanical olive variety; omit it to keep the current one", example = "SEVILLANA", allowableValues = {"CRIOLLA", "SEVILLANA", "MANZANILLA", "ARBEQUINA"})
        String variety
) {
}
