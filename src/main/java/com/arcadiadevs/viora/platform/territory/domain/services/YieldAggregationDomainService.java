package com.arcadiadevs.viora.platform.territory.domain.services;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberIntake;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberRange;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberSplit;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ConfidenceLevel;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ConfidenceThresholds;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.IntakeConfidence;
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
            var kilograms = plot.projectedKg();
            var share = plot.greenShare();
            var plotGreen = share == null ? 0.0 : kilograms * share;
            var plotBlack = share == null ? 0.0 : kilograms * (1 - share);
            var plotUnclassified = share == null ? kilograms : 0.0;
            totals.add(kilograms, plotGreen, plotBlack, plotUnclassified);
            green += plotGreen;
            black += plotBlack;
            unclassified += plotUnclassified;
        }
        var status = sampled == 0 ? IntakeStatus.NO_DATA
                : sampled * 100.0 / plots.size() < PRELIMINARY_THRESHOLD_PERCENT ? IntakeStatus.PRELIMINARY
                : IntakeStatus.SUFFICIENT;
        Double total = status == IntakeStatus.NO_DATA || estimable == 0 ? null : green + black + unclassified;
        var sectors = new ArrayList<SectorIntake>();
        bySector.forEach((sector, totals) -> sectors.add(totals.of(sector)));
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
            totals.add(plot.greenKg() + plot.blackKg(), plot.greenKg(), plot.blackKg(), 0.0);
            green += plot.greenKg();
            black += plot.blackKg();
        }
        var status = settled == 0 ? IntakeStatus.NO_DATA
                : settled == plots.size() ? IntakeStatus.CLOSED : IntakeStatus.PARTIAL;
        var sectors = new ArrayList<SectorIntake>();
        bySector.forEach((sector, totals) -> sectors.add(totals.of(sector)));
        return new IntakeProjection(status, settled == 0 ? null : green + black, green, black, 0.0, plots.size(),
                settled, settled, sectors);
    }

    /**
     * Grades how far a projection can be trusted, by the lower of the plot and the hectare coverage: a few big plots
     * sampled or many small ones are not enough on their own.
     *
     * @param plots      the member plots that count
     * @param thresholds the coverage of each level
     * @return the level, its coverage and what is left to sample for the next one
     * @throws IllegalArgumentException if plots or thresholds is null
     */
    public static IntakeConfidence confidence(List<PlotYield> plots, ConfidenceThresholds thresholds) {
        if (plots == null || thresholds == null) {
            throw new IllegalArgumentException("territory.intake.plots.null");
        }
        if (plots.isEmpty()) {
            return new IntakeConfidence(ConfidenceLevel.LOW, 0.0, 0, 0.0);
        }
        var sampled = plots.stream().filter(PlotYield::sampled).count();
        var hectares = plots.stream().mapToDouble(YieldAggregationDomainService::hectaresOf).sum();
        var sampledHectares = plots.stream().filter(PlotYield::sampled)
                .mapToDouble(YieldAggregationDomainService::hectaresOf).sum();
        var plotCoverage = sampled * 100.0 / plots.size();
        // Without areas the plots alone tell the coverage.
        var coverage = hectares > 0 ? Math.min(plotCoverage, sampledHectares * 100 / hectares) : plotCoverage;
        var level = coverage >= thresholds.highPercent() ? ConfidenceLevel.HIGH
                : coverage >= thresholds.mediumPercent() ? ConfidenceLevel.MEDIUM
                : ConfidenceLevel.LOW;
        if (level == ConfidenceLevel.HIGH) {
            return new IntakeConfidence(level, round(coverage), 0, 0.0);
        }
        var target = (level == ConfidenceLevel.LOW ? thresholds.mediumPercent() : thresholds.highPercent()) / 100;
        var pendingPlots = (int) Math.max(0, Math.ceil(target * plots.size() - 1e-9) - sampled);
        var pendingHectares = Math.max(0, target * hectares - sampledHectares);
        return new IntakeConfidence(level, round(coverage), pendingPlots, round(pendingHectares));
    }

    /**
     * Splits the projected intake by the caliber ranges of the plant. Each estimable plot adds its kilograms to the
     * range of the caliber its producer last weighed, split green and black as in the projection.
     *
     * @param plots  the member plots that count
     * @param ranges the ranges of the plant; empty when it set none
     * @return the split, or {@link CaliberSplit#NONE} without ranges
     * @throws IllegalArgumentException if plots or ranges is null
     */
    public static CaliberSplit splitByCaliber(List<PlotYield> plots, List<CaliberRange> ranges) {
        if (plots == null || ranges == null) {
            throw new IllegalArgumentException("territory.intake.plots.null");
        }
        if (ranges.isEmpty()) {
            return CaliberSplit.NONE;
        }
        var green = new double[ranges.size()];
        var black = new double[ranges.size()];
        var unclassified = new double[ranges.size()];
        var unsized = 0.0;
        var outOfRange = 0.0;
        for (var plot : plots) {
            if (!plot.estimable()) {
                continue;
            }
            if (plot.fruitsPerKg() == null) {
                unsized += plot.projectedKg();
                continue;
            }
            var index = indexOf(ranges, plot.fruitsPerKg());
            if (index < 0) {
                outOfRange += plot.projectedKg();
                continue;
            }
            var share = plot.greenShare();
            if (share == null) {
                unclassified[index] += plot.projectedKg();
            } else {
                green[index] += plot.projectedKg() * share;
                black[index] += plot.projectedKg() * (1 - share);
            }
        }
        var split = new ArrayList<CaliberIntake>();
        for (int i = 0; i < ranges.size(); i++) {
            split.add(new CaliberIntake(ranges.get(i), green[i], black[i], unclassified[i]));
        }
        return new CaliberSplit(split, unsized, outOfRange);
    }

    private static int indexOf(List<CaliberRange> ranges, double fruitsPerKg) {
        for (int i = 0; i < ranges.size(); i++) {
            if (ranges.get(i).contains(fruitsPerKg)) {
                return i;
            }
        }
        return -1;
    }

    private static double hectaresOf(PlotYield plot) {
        return plot.areaHa() == null ? 0.0 : plot.areaHa();
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static final class Totals {
        private int plots;
        private int sampled;
        private Double kilograms;
        private Double green;
        private Double black;
        private Double unclassified;

        // The parts start together with the total, so a sector none of whose plots can tell has none of them.
        private void add(double value, double greenPart, double blackPart, double unclassifiedPart) {
            kilograms = plus(kilograms, value);
            green = plus(green, greenPart);
            black = plus(black, blackPart);
            unclassified = plus(unclassified, unclassifiedPart);
        }

        private SectorIntake of(SectorPlacement sector) {
            return new SectorIntake(sector, plots, sampled, kilograms, green, black, unclassified);
        }

        private static Double plus(Double total, double value) {
            return total == null ? value : total + value;
        }
    }
}
