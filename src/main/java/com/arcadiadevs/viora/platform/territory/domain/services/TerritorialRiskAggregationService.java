package com.arcadiadevs.viora.platform.territory.domain.services;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.DailyMinimum;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotRiskAssessment;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotRiskSignals;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskLevel;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskReason;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskReasonType;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Domain service of the territorial traffic light: the color of each member plot and the order a manager visits
 * them.
 * <ul>
 *   <li><b>Red:</b> overload above 30 %, an open critical incident, or a forecast minimum of 1.5 °C or less today or
 *   tomorrow.</li>
 *   <li><b>Amber:</b> an open warning incident.</li>
 *   <li><b>Grey:</b> no valid sampling this campaign, so the load is unknown. It never counts as green.</li>
 *   <li><b>Green:</b> valid sampling, overload of 30 % or less, and nothing of the above.</li>
 * </ul>
 * <p>The biennial bearing index is informative and never changes the color. The thresholds wait for the agronomist's
 * review; until then they are the ones the team agreed.</p>
 */
public final class TerritorialRiskAggregationService {

    /** Overload above this percentage is red. */
    public static final double OVERLOAD_THRESHOLD_PERCENT = 30.0;
    /** A forecast minimum at or below this is red. */
    public static final double FROST_THRESHOLD_CELSIUS = 1.5;
    /** How many days ahead, today included, a frost forecast counts: about 48 hours. */
    public static final int FROST_WINDOW_DAYS = 2;

    private static final Comparator<PlotRiskAssessment> BY_LEVEL =
            Comparator.comparingInt(assessment -> assessment.level().ordinal());

    private TerritorialRiskAggregationService() {
    }

    /**
     * Gives a member plot its color and reasons.
     *
     * @param signals what the other contexts tell about the plot
     * @return the assessment
     * @throws IllegalArgumentException if the signals are null
     */
    public static PlotRiskAssessment assess(PlotRiskSignals signals) {
        if (signals == null) {
            throw new IllegalArgumentException("territory.risk.signals.null");
        }
        var overload = overloadPercent(signals);
        var reasons = new ArrayList<RiskReason>();
        if (overload != null && overload > OVERLOAD_THRESHOLD_PERCENT) {
            reasons.add(new RiskReason(RiskReasonType.OVERLOAD, null, overload, null));
        }
        signals.criticalIncidents().forEach(type ->
                reasons.add(new RiskReason(RiskReasonType.CRITICAL_INCIDENT, type, null, null)));
        signals.forecastMinimums().stream()
                .filter(day -> !day.date().isBefore(signals.today())
                        && day.date().isBefore(signals.today().plusDays(FROST_WINDOW_DAYS)))
                .filter(day -> day.minimumCelsius() <= FROST_THRESHOLD_CELSIUS)
                .min(Comparator.comparingDouble(DailyMinimum::minimumCelsius))
                .ifPresent(day -> reasons.add(new RiskReason(RiskReasonType.FROST_FORECAST, null,
                        day.minimumCelsius(), day.date())));
        var red = !reasons.isEmpty();
        signals.warningIncidents().forEach(type ->
                reasons.add(new RiskReason(RiskReasonType.WARNING_INCIDENT, type, null, null)));
        RiskLevel level;
        if (red) {
            level = RiskLevel.CRITICAL;
        } else if (!signals.warningIncidents().isEmpty()) {
            level = RiskLevel.MODERATE;
        } else if (!signals.sampled()) {
            level = RiskLevel.NO_DATA;
        } else {
            level = RiskLevel.OPTIMAL;
        }
        return new PlotRiskAssessment(level, reasons, overload);
    }

    /**
     * Orders two plots for a visit: red first, then the larger overload, amber, grey and green; on a tie, the closer
     * one first, and a plot without a distance last.
     *
     * @param first          the first plot
     * @param firstDistance  its distance to the manager in kilometers; null when unknown
     * @param second         the second plot
     * @param secondDistance its distance to the manager in kilometers; null when unknown
     * @return negative when the first is visited before
     */
    public static int comparePriority(PlotRiskAssessment first, Double firstDistance, PlotRiskAssessment second,
                                      Double secondDistance) {
        var byLevel = BY_LEVEL.compare(first, second);
        if (byLevel != 0) {
            return byLevel;
        }
        if (first.level() == RiskLevel.CRITICAL) {
            var byOverload = Double.compare(valueOf(second.overloadPercent()), valueOf(first.overloadPercent()));
            if (byOverload != 0) {
                return byOverload;
            }
        }
        return Comparator.nullsLast(Comparator.<Double>naturalOrder()).compare(firstDistance, secondDistance);
    }

    private static Double overloadPercent(PlotRiskSignals signals) {
        var pre = signals.preThinningFruitsPerShoot();
        var target = signals.targetFruitsPerShoot();
        if (!signals.sampled() || pre == null || target == null || target <= 0) {
            return null;
        }
        // Registering the thinning settles the overload until a new sampling says otherwise.
        if (signals.thinningExecuted() || pre <= target) {
            return 0.0;
        }
        return Math.round((pre / target - 1) * 1000.0) / 10.0;
    }

    private static double valueOf(Double value) {
        return value == null ? 0.0 : value;
    }
}
