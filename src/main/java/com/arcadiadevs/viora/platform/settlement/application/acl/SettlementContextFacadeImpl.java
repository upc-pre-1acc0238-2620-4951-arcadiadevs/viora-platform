package com.arcadiadevs.viora.platform.settlement.application.acl;

import com.arcadiadevs.viora.platform.settlement.application.queryservices.ProvisionalDossierQueryService;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetProvisionalDossierByPlotIdQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetProvisionalDossierPdfByPlotIdQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.interfaces.acl.SettlementContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-only implementation of the Settlement facade. It reads the settlements of the plot's agronomic report and
 * names each caliber with the commercial size scale, as the settlement endpoints do.
 */
@Service
@Transactional(readOnly = true)
public class SettlementContextFacadeImpl implements SettlementContextFacade {

    private final AgronomicReportRepository reportRepository;
    private final CommercialSizeGradeQueryService commercialSizeGradeQueryService;
    private final ProvisionalDossierQueryService provisionalDossierQueryService;

    /**
     * Constructs the facade.
     *
     * @param reportRepository                the agronomic reports, which hold the settlements of a plot
     * @param commercialSizeGradeQueryService names the commercial size grade of a caliber
     * @param provisionalDossierQueryService  reads and renders provisional dossiers
     */
    public SettlementContextFacadeImpl(AgronomicReportRepository reportRepository,
                                       CommercialSizeGradeQueryService commercialSizeGradeQueryService,
                                       ProvisionalDossierQueryService provisionalDossierQueryService) {
        this.reportRepository = reportRepository;
        this.commercialSizeGradeQueryService = commercialSizeGradeQueryService;
        this.provisionalDossierQueryService = provisionalDossierQueryService;
    }

    @Override
    public List<SettledCaliber> findSettledCalibers(String plotId) {
        if (!isUuid(plotId)) {
            return List.of();
        }
        return reportRepository.findByPlotId(new PlotId(plotId))
                .map(report -> report.snapshot().settlements().stream()
                        .filter(settlement -> settlement.commercialFruitsPerKg() != null
                                && settlement.commercialFruitsPerKg() > 0)
                        // One caliber per campaign: the latest settlement of the year wins
                        .collect(Collectors.toMap(settlement -> settlement.campaignYear().value(),
                                settlement -> settlement,
                                (first, second) -> isLater(second, first) ? second : first,
                                TreeMap::new))
                        .values().stream()
                        .map(settlement -> new SettledCaliber(
                                settlement.campaignYear().value(),
                                settlement.commercialFruitsPerKg(),
                                commercialSizeGradeQueryService.gradeOf(settlement.commercialFruitsPerKg())
                                        .orElse(null)))
                        .toList())
                .orElse(List.of());
    }

    private static boolean isLater(HarvestSettlementSnapshot candidate, HarvestSettlementSnapshot current) {
        if (candidate.settledAt() == null) {
            return false;
        }
        return current.settledAt() == null || candidate.settledAt().isAfter(current.settledAt());
    }

    private static boolean isUuid(String value) {
        if (value == null) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public Optional<ProvisionalDossier> findProvisionalDossier(String plotId, String holderName,
                                                               String organizationName) {
        return plotIdOf(plotId)
                .flatMap(id -> provisionalDossierQueryService.handle(
                        new GetProvisionalDossierByPlotIdQuery(id, holderName, organizationName)))
                .map(content -> new ProvisionalDossier(content.plotName(), content.variety(), content.areaHectares(),
                        content.campaignYear().value(), content.cutOffAt(), content.preThinningFruitsPerShoot(),
                        content.targetFruitsPerShoot(), content.overTargetPercentage(),
                        content.prescribedRemovalPercentage(), content.windowOpensOn(), content.windowClosesOn(),
                        content.sampledTrees(), content.lastSampledOn(), content.thinnedOn(), content.linkedNodes(),
                        content.daysWithReadings()));
    }

    @Override
    public Optional<ProvisionalDossierDocument> renderProvisionalDossier(String plotId, String holderName,
                                                                         String organizationName) {
        return plotIdOf(plotId)
                .flatMap(id -> provisionalDossierQueryService.handle(
                        new GetProvisionalDossierPdfByPlotIdQuery(id, holderName, organizationName)))
                .map(pdf -> new ProvisionalDossierDocument(pdf.content(), pdf.cutOffAt()));
    }

    private static Optional<PlotId> plotIdOf(String plotId) {
        try {
            return Optional.of(new PlotId(plotId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
