package com.arcadiadevs.viora.platform.settlement.application.internal.queryservices;

import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalThinningService.CampaignProgress;
import com.arcadiadevs.viora.platform.settlement.application.queryservices.ProvisionalDossierPdf;
import com.arcadiadevs.viora.platform.settlement.application.queryservices.ProvisionalDossierQueryService;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.ProvisionalDossierContent;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.ProvisionalDossierPdfGenerator;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetProvisionalDossierByPlotIdQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetProvisionalDossierPdfByPlotIdQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Reads the provisional dossier of a plot from Orchard, Thinning and Telemetry each time it is asked, and renders it.
 * Nothing is stored and nothing is hashed: it reflects the data at the cut-off instant.
 */
@Service
@Transactional(readOnly = true)
public class ProvisionalDossierQueryServiceImpl implements ProvisionalDossierQueryService {

    private final ExternalOrchardService externalOrchardService;
    private final ExternalThinningService externalThinningService;
    private final ExternalTelemetryService externalTelemetryService;
    private final ProvisionalDossierPdfGenerator provisionalDossierPdfGenerator;
    private final Clock clock;

    /**
     * Creates the query service.
     *
     * @param externalOrchardService         the plots
     * @param externalThinningService        the sampling, load and prescription
     * @param externalTelemetryService       the sensor coverage
     * @param provisionalDossierPdfGenerator the PDF renderer
     * @param clock                          the application clock
     * @throws IllegalArgumentException if any dependency is null
     */
    public ProvisionalDossierQueryServiceImpl(ExternalOrchardService externalOrchardService,
                                              ExternalThinningService externalThinningService,
                                              ExternalTelemetryService externalTelemetryService,
                                              ProvisionalDossierPdfGenerator provisionalDossierPdfGenerator,
                                              Clock clock) {
        if (externalOrchardService == null || externalThinningService == null || externalTelemetryService == null
                || provisionalDossierPdfGenerator == null || clock == null) {
            throw new IllegalArgumentException("settlement.provisional_dossier.dependency.null");
        }
        this.externalOrchardService = externalOrchardService;
        this.externalThinningService = externalThinningService;
        this.externalTelemetryService = externalTelemetryService;
        this.provisionalDossierPdfGenerator = provisionalDossierPdfGenerator;
        this.clock = clock;
    }

    @Override
    public Optional<ProvisionalDossierContent> handle(GetProvisionalDossierByPlotIdQuery query) {
        return read(query.plotId(), query.holderName(), query.organizationName());
    }

    @Override
    public Optional<ProvisionalDossierPdf> handle(GetProvisionalDossierPdfByPlotIdQuery query) {
        return read(query.plotId(), query.holderName(), query.organizationName())
                .map(content -> new ProvisionalDossierPdf(provisionalDossierPdfGenerator.render(content),
                        content.cutOffAt()));
    }

    private Optional<ProvisionalDossierContent> read(PlotId plotId, String holderName, String organizationName) {
        if (externalOrchardService.findActivePlotOwner(plotId).isEmpty()) {
            return Optional.empty();
        }
        var cutOff = clock.instant();
        var today = LocalDate.ofInstant(cutOff, ProducerTimeZone.ZONE);
        var campaign = new CampaignYear(today.getYear());
        var plot = externalOrchardService.describePlot(plotId);
        var progress = externalThinningService.findCampaignProgress(plotId, campaign);
        var campaignStart = today.withDayOfYear(1).atStartOfDay(ProducerTimeZone.ZONE).toInstant();
        var climate = externalTelemetryService.findClimateCoverage(plotId, campaignStart, cutOff);
        var pre = progress.map(CampaignProgress::preThinningFruitsPerShoot).orElse(null);
        var target = progress.map(CampaignProgress::targetFruitsPerShoot).orElse(null);
        return Optional.of(new ProvisionalDossierContent(plot.name(), plot.variety(), plot.areaHectares(),
                holderName, organizationName, campaign, cutOff, pre, target, overTarget(pre, target),
                progress.map(CampaignProgress::percentageToRemove).orElse(null),
                progress.map(CampaignProgress::windowOpensOn).orElse(null),
                progress.map(CampaignProgress::windowClosesOn).orElse(null),
                progress.map(CampaignProgress::sampledTrees).orElse(0),
                progress.map(CampaignProgress::lastSampledOn).orElse(null),
                progress.map(CampaignProgress::executedOn).orElse(null),
                climate.linkedNodes(), climate.daysWithReadings()));
    }

    private static Double overTarget(Double pre, Double target) {
        if (pre == null || target == null || target <= 0) {
            return null;
        }
        return BigDecimal.valueOf((pre / target - 1) * 100).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
