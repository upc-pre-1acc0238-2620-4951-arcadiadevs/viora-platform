package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.util.List;

/**
 * The color of a member plot and why.
 *
 * @param level           the color
 * @param reasons         the reasons, the most serious first; empty for green and grey
 * @param overloadPercent how much more fruit than sustainable, in percent; null without a valid sampling, zero when
 *                        within the target or once thinned
 */
public record PlotRiskAssessment(RiskLevel level, List<RiskReason> reasons, Double overloadPercent) {

    /**
     * Validates the level and copies the reasons.
     *
     * @throws IllegalArgumentException if the level is null
     */
    public PlotRiskAssessment {
        if (level == null) {
            throw new IllegalArgumentException("territory.risk.level.null");
        }
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
