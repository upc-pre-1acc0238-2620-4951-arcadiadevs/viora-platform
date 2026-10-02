package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReportSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.AgronomicReportPersistenceEntity;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.HarvestSettlementPersistenceEntity;

import java.util.HashSet;
import java.util.UUID;

/** Maps agronomic reports and their settlements between the domain and JPA, in both directions. */
public final class AgronomicReportPersistenceAssembler {

    private AgronomicReportPersistenceAssembler() {
    }

    public static AgronomicReportPersistenceEntity toNewEntity(AgronomicReport report) {
        var snapshot = report.snapshot();
        var entity = new AgronomicReportPersistenceEntity();
        entity.setId(UUID.fromString(snapshot.id().reportId()));
        entity.setPlotId(UUID.fromString(snapshot.plotId().plotId()));
        entity.setProducerId(UUID.fromString(snapshot.producerId().userId()));
        appendNewSettlements(entity, snapshot);
        return entity;
    }

    /** Settlements are immutable: only the ones missing in the entity are appended. */
    public static void appendNewSettlements(AgronomicReportPersistenceEntity entity, AgronomicReportSnapshot snapshot) {
        var known = new HashSet<UUID>();
        entity.getSettlements().forEach(s -> known.add(s.getId()));
        for (var settlement : snapshot.settlements()) {
            UUID id = UUID.fromString(settlement.id().settlementId());
            if (!known.contains(id)) {
                entity.getSettlements().add(toEntity(entity, settlement));
            }
        }
    }

    public static AgronomicReport toDomain(AgronomicReportPersistenceEntity entity) {
        var reportId = new ReportId(entity.getId().toString());
        var plotId = new PlotId(entity.getPlotId().toString());
        var settlements = entity.getSettlements().stream()
                .map(s -> toSnapshot(reportId, plotId, s))
                .toList();
        return AgronomicReport.reconstitute(new AgronomicReportSnapshot(reportId, plotId,
                new UserId(entity.getProducerId().toString()), settlements, entity.getRevision()));
    }

    private static HarvestSettlementPersistenceEntity toEntity(AgronomicReportPersistenceEntity parent,
            HarvestSettlementSnapshot snapshot) {
        var entity = new HarvestSettlementPersistenceEntity();
        entity.setId(UUID.fromString(snapshot.id().settlementId()));
        entity.setReport(parent);
        entity.setCampaignYear(snapshot.campaignYear().value());
        entity.setGreenOlivesKg(snapshot.greenOlivesWeight().kilograms());
        entity.setBlackOlivesKg(snapshot.blackOlivesWeight().kilograms());
        entity.setTotalYieldKg(snapshot.totalHarvestWeight().kilograms());
        entity.setCommercialFruitsPerKg(snapshot.commercialFruitsPerKg());
        entity.setNotes(snapshot.notes());
        entity.setStatus(snapshot.status().name());
        entity.setSettledAt(snapshot.settledAt());

        var balance = snapshot.thinningBalance();
        entity.setThinningStatus(balance.status().name());
        entity.setThinningExecutedDate(balance.executedDate());
        entity.setPrescribedRemovalPercentage(balance.prescribedRemovalPercentage());
        entity.setActualRemovalPercentage(balance.actualRemovalPercentage());
        entity.setRemovalDeviationPoints(balance.deviationPercentagePoints());

        var curve = snapshot.trendCurve();
        entity.setCurveStatus(curve.status().name());
        entity.setBaselineCampaigns(curve.baselineCampaigns());
        entity.setSettledCampaigns(curve.settledCampaigns());
        entity.setBaselineYieldKg(curve.baselineYieldKg());
        entity.setBaselineAlternationIndex(curve.baselineAlternationIndex());
        entity.setManagedAlternationIndex(curve.managedAlternationIndex());
        entity.setAmplitudeReductionRate(curve.amplitudeReductionRate());
        entity.setStabilizationTargetAchieved(curve.targetAchieved());
        entity.setInterannualVarianceKg2(curve.interannualVarianceKg2());
        entity.setCoefficientOfVariation(curve.coefficientOfVariation());
        return entity;
    }

    private static HarvestSettlementSnapshot toSnapshot(ReportId reportId, PlotId plotId,
            HarvestSettlementPersistenceEntity entity) {
        var balance = new ThinningBalance(ThinningComplianceStatus.valueOf(entity.getThinningStatus()),
                entity.getThinningExecutedDate(), entity.getPrescribedRemovalPercentage(),
                entity.getActualRemovalPercentage(), entity.getRemovalDeviationPoints());
        var curve = new StabilizationTrendCurve(StabilizationStatus.valueOf(entity.getCurveStatus()),
                entity.getBaselineCampaigns(), entity.getSettledCampaigns(), entity.getBaselineYieldKg(),
                entity.getBaselineAlternationIndex(), entity.getManagedAlternationIndex(),
                entity.getAmplitudeReductionRate(), entity.getStabilizationTargetAchieved(),
                entity.getInterannualVarianceKg2(), entity.getCoefficientOfVariation());
        return new HarvestSettlementSnapshot(new SettlementId(entity.getId().toString()), reportId, plotId,
                new CampaignYear(entity.getCampaignYear()), new OliveWeight(entity.getGreenOlivesKg()),
                new OliveWeight(entity.getBlackOlivesKg()), new OliveWeight(entity.getTotalYieldKg()),
                entity.getCommercialFruitsPerKg(), entity.getNotes(), SettlementStatus.valueOf(entity.getStatus()),
                entity.getSettledAt(), balance, curve);
    }
}
