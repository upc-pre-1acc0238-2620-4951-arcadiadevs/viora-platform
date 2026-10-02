package com.arcadiadevs.viora.platform.settlement.domain.model.aggregates;

import com.arcadiadevs.viora.platform.settlement.domain.model.entities.HarvestSettlement;
import com.arcadiadevs.viora.platform.settlement.domain.model.events.CampaignHarvestSettledEvent;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.services.StabilizationCurveCalculatorService;
import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Aggregate Root holding the audited productive memory of one plot: its immutable annual settlements.
 *
 * <p>Each settlement freezes its own balance and curve, so a later campaign never alters a closed one and a
 * certification can reference {@code reportId + campaignYear} unambiguously.</p>
 */
public class AgronomicReport extends AbstractDomainAggregateRoot<AgronomicReport> {

    private final ReportId id;
    private final PlotId plotId;
    private final UserId producerId;
    private final List<HarvestSettlementSnapshot> settlements;
    private final Long revision;

    private AgronomicReport(ReportId id, PlotId plotId, UserId producerId,
            List<HarvestSettlementSnapshot> settlements, Long revision) {
        this.id = id;
        this.plotId = plotId;
        this.producerId = producerId;
        this.settlements = new ArrayList<>(settlements);
        this.revision = revision;
    }

    /**
     * Opens the report of a plot.
     *
     * @param plotId     plot of the report
     * @param producerId owner producer
     * @return an empty report
     */
    public static AgronomicReport createForPlot(PlotId plotId, UserId producerId) {
        if (plotId == null || producerId == null) {
            throw new IllegalArgumentException("settlement.reference.null");
        }
        return new AgronomicReport(new ReportId(), plotId, producerId, List.of(), null);
    }

    public static AgronomicReport reconstitute(AgronomicReportSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("settlement.report.snapshot.null");
        }
        return new AgronomicReport(snapshot.id(), snapshot.plotId(), snapshot.producerId(),
                snapshot.settlements(), snapshot.revision());
    }

    /**
     * Settles a campaign once: computes its total, freezes the thinning balance and the stabilization curve,
     * and registers {@link CampaignHarvestSettledEvent}.
     *
     * @param campaignYear          campaign to settle
     * @param green                 green olives delivered
     * @param black                 black olives delivered
     * @param commercialFruitsPerKg optional caliber of the delivered olives
     * @param notes                 optional notes
     * @param thinningBalance       balance against the thinning prescription of the campaign
     * @param historicalYields      Phenology history of the plot (total kg per campaign)
     * @param clock                 clock stamping the settlement
     * @return the new settlement
     */
    public HarvestSettlementSnapshot settleCampaign(CampaignYear campaignYear, OliveWeight green, OliveWeight black,
            Double commercialFruitsPerKg, String notes, ThinningBalance thinningBalance,
            SortedMap<Integer, Double> historicalYields, Clock clock) {
        if (campaignYear == null) {
            throw new IllegalArgumentException("settlement.campaign_year.null");
        }
        if (settlementOf(campaignYear).isPresent()) {
            throw new IllegalStateException("settlement.campaign.already_settled");
        }
        var total = HarvestSettlement.calculateTotalWeight(green, black);

        SortedMap<Integer, Double> settledYields = new TreeMap<>();
        settlements.forEach(s -> settledYields.put(s.campaignYear().value(), s.totalHarvestWeight().kilograms()));
        settledYields.put(campaignYear.value(), total.kilograms());
        var curve = StabilizationCurveCalculatorService.computeCurve(historicalYields, settledYields);

        var settlement = HarvestSettlement.create(id, plotId, campaignYear, green, black, commercialFruitsPerKg,
                notes, thinningBalance, curve, clock.instant()).snapshot();
        settlements.add(settlement);
        registerDomainEvent(new CampaignHarvestSettledEvent(UUID.randomUUID().toString(), id.reportId(),
                settlement.id().settlementId(), plotId.plotId(), campaignYear.value(),
                green.kilograms(), black.kilograms(), total.kilograms(), commercialFruitsPerKg,
                settlement.settledAt()));
        return settlement;
    }

    /**
     * Selects the frozen settlement of a campaign (used by certification).
     *
     * @param campaignYear campaign to look for
     * @return the settlement, if the campaign is settled
     */
    public Optional<HarvestSettlementSnapshot> settlementOf(CampaignYear campaignYear) {
        return settlements.stream().filter(s -> s.campaignYear().equals(campaignYear)).findFirst();
    }

    /** Curve of the latest settled campaign, or empty when nothing is settled yet. */
    public Optional<StabilizationTrendCurve> trendCurve() {
        return settlements.stream()
                .max(Comparator.comparing(s -> s.campaignYear().value()))
                .map(HarvestSettlementSnapshot::trendCurve);
    }

    public AgronomicReportSnapshot snapshot() {
        return new AgronomicReportSnapshot(id, plotId, producerId, settlements, revision);
    }
}
