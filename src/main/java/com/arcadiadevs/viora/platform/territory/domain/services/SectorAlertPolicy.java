package com.arcadiadevs.viora.platform.territory.domain.services;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotRisk;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskLevel;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskReason;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskReasonType;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertFacts;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertType;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Domain service deciding which alerts a sector deserves from the colors of its member plots.
 * <ul>
 *   <li><b>Sector in critical:</b> at least one plot in red for overload or a critical incident other than frost.
 *   It leads with the plot of the largest overload.</li>
 *   <li><b>Frost:</b> at least one plot with frost forecast or an open frost incident. It leads with the lowest
 *   forecast minimum. It is «Atención» for the sector, even though the plot itself is red.</li>
 * </ul>
 * <p>A frost alone does not make the sector critical: it has its own alert, so the manager does not get it twice.</p>
 */
public final class SectorAlertPolicy {

    private static final String FROST_INCIDENT = "FROST_WARNING";

    private SectorAlertPolicy() {
    }

    /**
     * Decides the alerts a sector deserves now.
     *
     * @param plots the member plots of the sector
     * @return the facts of each alert it deserves; empty when none
     */
    public static Map<SectorAlertType, SectorAlertFacts> alertsFor(List<PlotRisk> plots) {
        var alerts = new EnumMap<SectorAlertType, SectorAlertFacts>(SectorAlertType.class);
        var size = plots.size();
        var critical = plots.stream().filter(SectorAlertPolicy::isCriticalBeyondFrost).toList();
        if (!critical.isEmpty()) {
            var lead = critical.stream()
                    .max(Comparator.comparingDouble(plot -> valueOf(plot.assessment().overloadPercent())))
                    .orElseThrow();
            var reason = lead.assessment().reasons().stream()
                    .filter(each -> each.type() == RiskReasonType.OVERLOAD
                            || each.type() == RiskReasonType.CRITICAL_INCIDENT && !isFrost(each))
                    .findFirst().orElseThrow();
            alerts.put(SectorAlertType.SECTOR_CRITICAL, new SectorAlertFacts(critical.size(), size, lead.plotId(),
                    lead.producerId(), reason.type(), reason.value(), null));
        }
        var frozen = plots.stream().filter(plot -> plot.assessment().reasons().stream()
                .anyMatch(SectorAlertPolicy::isFrost)).toList();
        if (!frozen.isEmpty()) {
            var coldest = frozen.stream()
                    .flatMap(plot -> frostForecast(plot).map(reason -> Map.entry(plot, reason)).stream())
                    .min(Comparator.comparingDouble(entry -> entry.getValue().value()));
            var lead = coldest.map(Map.Entry::getKey).orElse(frozen.getFirst());
            var reason = coldest.map(Map.Entry::getValue).orElse(lead.assessment().reasons().stream()
                    .filter(SectorAlertPolicy::isFrost).findFirst().orElseThrow());
            alerts.put(SectorAlertType.FROST, new SectorAlertFacts(frozen.size(), size, lead.plotId(),
                    lead.producerId(), reason.type(), reason.value(), reason.date()));
        }
        return alerts;
    }

    /**
     * The color of a sector: its most urgent plot's, where a sector without plots has no data.
     *
     * @param plots the member plots of the sector
     * @return the sector's color
     */
    public static RiskLevel levelOf(List<PlotRisk> plots) {
        return plots.stream()
                .map(plot -> plot.assessment().level())
                .min(Comparator.naturalOrder())
                .orElse(RiskLevel.NO_DATA);
    }

    private static boolean isCriticalBeyondFrost(PlotRisk plot) {
        return plot.assessment().level() == RiskLevel.CRITICAL && plot.assessment().reasons().stream()
                .anyMatch(reason -> reason.type() == RiskReasonType.OVERLOAD
                        || reason.type() == RiskReasonType.CRITICAL_INCIDENT && !isFrost(reason));
    }

    private static boolean isFrost(RiskReason reason) {
        return reason.type() == RiskReasonType.FROST_FORECAST || FROST_INCIDENT.equals(reason.incidentType());
    }

    private static Optional<RiskReason> frostForecast(PlotRisk plot) {
        return plot.assessment().reasons().stream()
                .filter(reason -> reason.type() == RiskReasonType.FROST_FORECAST && reason.value() != null)
                .findFirst();
    }

    private static double valueOf(Double value) {
        return value == null ? 0.0 : value;
    }
}
