package com.arcadiadevs.viora.platform.phenology.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

import java.time.Instant;

/**
 * Public REST response resource representing an annual harvest record in the olive orchard.
 *
 * @param id                    the unique harvest entry identifier UUID
 * @param plotId                the logical reference UUID of the olive plot
 * @param campaignYear          the agricultural campaign year
 * @param totalYieldKg          the total volume harvested in kilograms
 * @param greenKg               the kilograms of green olives
 * @param blackKg               the kilograms of black olives
 * @param bearingClassification the classified bearing behavior (ON_YEAR, OFF_YEAR, BALANCED, INSUFFICIENT_DATA)
 * @param calculatedBbi         the assessed Hoblyn Biennial Bearing Index
 * @param recordedAt            the timestamp when recorded
 */
@Schema(
        name = "HarvestRecordResource",
        description = "Response resource representing an annual olive harvest record",
        example = "{\"id\": \"550e8400-e29b-41d4-a716-446655440001\", \"plotId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"campaignYear\": 2025, \"totalYieldKg\": 14250.0, \"greenKg\": 8200.0, \"blackKg\": 6050.0, \"bearingClassification\": \"ON_YEAR\", \"calculatedBbi\": 0.42, \"recordedAt\": \"2026-09-27T17:00:00Z\"}"
)
@NullMarked
public record HarvestRecordResource(
        @Schema(description = "Unique harvest record identifier", example = "550e8400-e29b-41d4-a716-446655440001")
        String id,

        @Schema(description = "Managing plot identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String plotId,

        @Schema(description = "Agricultural campaign year", example = "2025")
        Integer campaignYear,

        @Schema(description = "Total olive fruit mass harvested in kilograms", example = "14250.0")
        Double totalYieldKg,

        @Schema(description = "Green olive mass in kilograms", example = "8200.0")
        Double greenKg,

        @Schema(description = "Black olive mass in kilograms", example = "6050.0")
        Double blackKg,

        @Schema(description = "Bearing alternation classification", example = "ON_YEAR", allowableValues = {"ON_YEAR", "OFF_YEAR", "BALANCED", "INSUFFICIENT_DATA"})
        String bearingClassification,

        @Schema(description = "Assessed Hoblyn Biennial Bearing Index", example = "0.42")
        Double calculatedBbi,

        @Schema(description = "Timestamp when the harvest was officially registered", example = "2026-09-27T17:00:00Z")
        Instant recordedAt
) {
}
