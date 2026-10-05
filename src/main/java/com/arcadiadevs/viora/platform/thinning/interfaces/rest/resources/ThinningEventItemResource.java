package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;

/**
 * REST response item resource representing an agronomic milestone event in the thinning lifecycle.
 *
 * @param id                  unique event identifier
 * @param eventType           type of milestone event (e.g. SAMPLING_COMPLETED, THINNING_EXECUTED)
 * @param prescriptionId      thinning prescription identifier
 * @param confirmationId      execution confirmation identifier (null for non-execution events)
 * @param plotId              plot identifier
 * @param plotName            human-readable plot name
 * @param campaignYear        agricultural campaign year
 * @param occurredAt          timestamp when event occurred
 * @param evaluatedTreesCount evaluated trees count (sampling events only)
 * @param totalShootsCount    total shoots counted (sampling events only)
 * @param totalFruitsCount    total fruits counted (sampling events only)
 * @param meanFruitsPerShoot  mean fruits per shoot (sampling events only)
 * @param isRepresentative    representativeness flag (sampling events only)
 * @param removalPercentage   percentage of fruits removed (execution events only)
 * @param removedKg           biomass removed in kilograms (execution events only)
 * @param executedDate        execution date (execution events only)
 * @param laborCrewSize       worker crew size (execution events only)
 * @param timeliness          timeliness status (execution events only)
 */
@Schema(name = "ThinningEventItemResource", description = "Agronomic milestone event in the fruit thinning lifecycle")
@NullMarked
public record ThinningEventItemResource(
        @Schema(description = "Unique event identifier UUID", example = "a1b2c3d4-0001-4000-8000-000000000001")
        String id,

        @Schema(description = "Event type", example = "SAMPLING_COMPLETED", allowableValues = {"SAMPLING_COMPLETED", "THINNING_EXECUTED"})
        String eventType,

        @Schema(description = "Prescription UUID", example = "f1e2d3c4-1111-4000-8000-000000000001")
        String prescriptionId,

        @Schema(description = "Execution confirmation UUID (null for non-execution events)", example = "c1b2a3d4-2222-4000-8000-000000000002")
        @Nullable String confirmationId,

        @Schema(description = "Plot UUID", example = "550e8400-e29b-41d4-a716-446655440001")
        String plotId,

        @Schema(description = "Human-readable plot name", example = "La Yarada 02")
        String plotName,

        @Schema(description = "Agricultural campaign year", example = "2026")
        Integer campaignYear,

        @Schema(description = "Timestamp when event occurred", example = "2026-11-18T09:30:00Z")
        Instant occurredAt,

        @Schema(description = "Count of unique trees evaluated (sampling events only)", example = "5")
        @Nullable Integer evaluatedTreesCount,

        @Schema(description = "Total shoots counted (sampling events only)", example = "60")
        @Nullable Integer totalShootsCount,

        @Schema(description = "Total set fruits counted (sampling events only)", example = "37")
        @Nullable Integer totalFruitsCount,

        @Schema(description = "Mean fruits per shoot (sampling events only)", example = "0.62")
        @Nullable Double meanFruitsPerShoot,

        @Schema(description = "Statistical representativeness reached flag (sampling events only)", example = "true")
        @Nullable Boolean isRepresentative,

        @Schema(description = "Percentage of fruit removed (execution events only)", example = "30.0")
        @Nullable Double removalPercentage,

        @Schema(description = "Total biomass removed in kg (execution events only)", example = "420.0")
        @Nullable Double removedKg,

        @Schema(description = "Field execution date (execution events only)", example = "2026-11-14")
        @Nullable LocalDate executedDate,

        @Schema(description = "Worker crew size (execution events only)", example = "4")
        @Nullable Integer laborCrewSize,

        @Schema(description = "Execution timeliness category (execution events only)", example = "OPTIMAL")
        @Nullable String timeliness
) {
}
