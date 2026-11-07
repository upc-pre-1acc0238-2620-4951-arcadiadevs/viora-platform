package com.arcadiadevs.viora.platform.territory.application.queryservices;

import java.time.Instant;
import java.util.List;

/**
 * Read model of the intake of a campaign, as G30 shows it.
 *
 * @param campaignYear                the campaign
 * @param kind                        {@code PROJECTION} while in progress, {@code SETTLED} once past
 * @param status                      {@code NO_DATA}, {@code PRELIMINARY} or {@code SUFFICIENT} for a projection;
 *                                    {@code NO_DATA}, {@code PARTIAL} or {@code CLOSED} for a past campaign
 * @param totalTons                   the tons; null when there is no figure
 * @param greenTons                   green olives, by each plot's history for a projection
 * @param blackTons                   black olives
 * @param unclassifiedTons            tons of plots without history, «Sin clasificar»
 * @param plots                       the member plots counted
 * @param sampledPlots                those sampled this campaign, or those that settled a past one
 * @param estimablePlots              those that add a figure
 * @param coveragePercent             the share of plots sampled or settled
 * @param preliminaryThresholdPercent the coverage from which a projection stops being preliminary
 * @param sectors                     the intake by sector
 * @param evaluatedAt                 when it was computed
 */
public record IntakeForecastView(int campaignYear, String kind, String status, Double totalTons, double greenTons,
                                 double blackTons, double unclassifiedTons, int plots, int sampledPlots,
                                 int estimablePlots, double coveragePercent, double preliminaryThresholdPercent,
                                 List<SectorIntakeView> sectors, Instant evaluatedAt) {
}
