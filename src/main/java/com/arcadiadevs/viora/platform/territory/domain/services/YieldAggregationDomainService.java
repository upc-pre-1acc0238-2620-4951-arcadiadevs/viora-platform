package com.arcadiadevs.viora.platform.territory.domain.services;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.IntakeProjection;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.IntakeStatus;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSettlement;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotYield;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorIntake;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorPlacement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain service adding up the intake of a campaign from the member plots: projected while it is in progress, settled
 * once it is past.
 * <ul>
 *   <li>Only estimable plots add up, with no extrapolation: a plot that cannot be estimated adds nothing, and no
 *   factor stands in for it.</li>
 *   <li>Each plot's kilograms split into green and black by the share of its own settled history; a plot without
 *   history goes to «Sin clasificar».</li>
 *   <li>Coverage is the share of plots sampled. Below half, the projection is preliminary; without any sampling there
 *   is no figure.</li>
 * </ul>
 */
public final class YieldAggregationDomainService {

    /** Sampling coverage, in percent, from which the projection stops being preliminary. */
    public static final double PRELIMINARY_THRESHOLD_PERCENT = 50.0;

    private YieldAggregationDomainService() {
    }

    /**
     * Projects the intake of a campaign in progress.
     *
     * @param plots the member plots that count
     * @return the projection
     * @throws IllegalArgumentException if plots is null
     */
    public static IntakeProjection project(List<PlotYield> plots) {
        if (plots == null) {
            throw new IllegalArgumentException("territory.intake.plots.null");
        }
        var green = 0.0;
        var black = 0.0;
        var unclassified = 0.0;
        var sampled = 0;
        var estimable = 0;
        Map<SectorPlacement, Totals> bySector = new LinkedHashMap<>();
        for (var plot : plots) {
            var totals = bySector.computeIfAbsent(plot.sector(), ignored -> new Totals());
            totals.plots++;
            if (plot.sampled()) {
                sampled++;
                totals.sampled++;
            }
            if (!plot.estimable()) {
                continue;
            }
            estimable++;
            totals.add(plot.projectedKg());
            if (plot.greenShare() == null) {
                unclassified += plot.projectedKg();
            } else {
                green += plot.projectedKg() * plot.greenShare();
                black += plot.projectedKg() * (1 - plot.greenShare());
            }
        }
        var status = sampled == 0 ? IntakeStatus.NO_DATA
                : sampled * 100.0 / plots.size() < PRELIMINARY_THRESHOLD_PERCENT ? IntakeStatus.PRELIMINARY
                : IntakeStatus.SUFFICIENT;
        Double total = status == IntakeStatus.NO_DATA || estimable == 0 ? null : green + black + unclassified;
        var sectors = new ArrayList<SectorIntake>();
        bySector.forEach((sector, totals) ->
                sectors.add(new SectorIntake(sector, totals.plots, totals.sampled, totals.kilograms)));
        return new IntakeProjection(status, total, green, black, unclassified, plots.size(), sampled, estimable,
                sectors);
    }

    /**
     * Adds up the intake settled in a past campaign. Every kilogram is classified, since the mill weighed green and
     * black apart.
     *
     * @param plots the member plots that count
     * @return the intake: closed when every plot settled, partial when some did, without data when none did
     * @throws IllegalArgumentException if plots is null
     */
    public static IntakeProjection settled(List<PlotSettlement> plots) {
        if (plots == null) {
            throw new IllegalArgumentException("territory.intake.plots.null");
        }
        var green = 0.0;
        var black = 0.0;
        var settled = 0;
        Map<SectorPlacement, Totals> bySector = new LinkedHashMap<>();
        for (var plot : plots) {
            var totals = bySector.computeIfAbsent(plot.sector(), ignored -> new Totals());
            totals.plots++;
            if (!plot.settled()) {
                continue;
            }
            settled++;
            totals.sampled++;
            totals.add(plot.greenKg() + plot.blackKg());
            green += plot.greenKg();
            black += plot.blackKg();
        }
        var status = settled == 0 ? IntakeStatus.NO_DATA
                : settled == plots.size() ? IntakeStatus.CLOSED : IntakeStatus.PARTIAL;
        var sectors = new ArrayList<SectorIntake>();
        bySector.forEach((sector, totals) ->
                sectors.add(new SectorIntake(sector, totals.plots, totals.sampled, totals.kilograms)));
        return new IntakeProjection(status, settled == 0 ? null : green + black, green, black, 0.0, plots.size(),
                settled, settled, sectors);
    }

    private static final class Totals {
        private int plots;
        private int sampled;
        private Double kilograms;

        private void add(double value) {
            kilograms = kilograms == null ? value : kilograms + value;
        }
    }
}
