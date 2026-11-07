package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * The intake of a campaign of the manager's license.
 *
 * @param campaignYear                the campaign
 * @param kind                        {@code PROJECTION} while in progress, {@code SETTLED} once past
 * @param status                      {@code NO_DATA}, {@code PRELIMINARY} or {@code SUFFICIENT} for a projection;
 *                                    {@code NO_DATA}, {@code PARTIAL} or {@code CLOSED} for a past campaign
 * @param totalTons                   the tons; null when there is no figure
 * @param greenTons                   green olives
 * @param blackTons                   black olives
 * @param unclassifiedTons            tons of plots without history, «Sin clasificar»
 * @param plots                       the member plots counted
 * @param sampledPlots                those sampled, or those that settled a past campaign
 * @param estimablePlots              those that add a figure
 * @param coveragePercent             the share of plots sampled or settled
 * @param preliminaryThresholdPercent the coverage from which a projection stops being preliminary
 * @param sectors                     the intake by sector; only when reading one campaign
 * @param evaluatedAt                 when it was computed
 */
@Schema(name = "IntakeForecastResource", description = "Intake of a campaign: projected or settled")
public record IntakeForecastResource(
        @Schema(example = "2026") int campaignYear,
        @Schema(example = "PROJECTION") String kind,
        @Schema(example = "SUFFICIENT") String status,
        @Schema(example = "1240.0") Double totalTons,
        @Schema(example = "700.0") double greenTons,
        @Schema(example = "420.0") double blackTons,
        @Schema(example = "120.0") double unclassifiedTons,
        @Schema(example = "60") int plots,
        @Schema(example = "37") int sampledPlots,
        @Schema(example = "31") int estimablePlots,
        @Schema(example = "61.7") double coveragePercent,
        @Schema(example = "50.0") double preliminaryThresholdPercent,
        List<SectorIntakeResource> sectors,
        Instant evaluatedAt
) {
}
