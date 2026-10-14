package com.arcadiadevs.viora.platform.settlement.application.internal.commandservices;

import com.arcadiadevs.viora.platform.settlement.application.commandservices.SealAgronomicDossierCommandService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.SealedDossier;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SealAgronomicDossierCommand;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.SealedDossierContent;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.SealedDossierPdfGenerator;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierFacts;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.SealedDossierRepository;
import com.arcadiadevs.viora.platform.settlement.domain.services.CryptographicHashService;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Seals the dossier of a settled campaign: gathers what every context knows about it, freezes those facts, renders
 * the PDF, hashes the final bytes and stores both. Sealing again returns the stored dossier untouched.
 */
@Service
@Transactional
public class SealAgronomicDossierCommandServiceImpl implements SealAgronomicDossierCommandService {

    private static final String RESOURCE = "AgronomicDossier";

    private final AgronomicReportRepository reportRepository;
    private final SealedDossierRepository dossierRepository;
    private final ExternalOrchardService orchard;
    private final ExternalThinningService thinning;
    private final ExternalPhenologyService phenology;
    private final ExternalTelemetryService telemetry;
    private final SealedDossierPdfGenerator pdfGenerator;
    private final CryptographicHashService hashService;
    private final Clock clock;

    public SealAgronomicDossierCommandServiceImpl(AgronomicReportRepository reportRepository,
            SealedDossierRepository dossierRepository, ExternalOrchardService orchard, ExternalThinningService thinning,
            ExternalPhenologyService phenology, ExternalTelemetryService telemetry,
            SealedDossierPdfGenerator pdfGenerator, CryptographicHashService hashService, Clock clock) {
        this.reportRepository = reportRepository;
        this.dossierRepository = dossierRepository;
        this.orchard = orchard;
        this.thinning = thinning;
        this.phenology = phenology;
        this.telemetry = telemetry;
        this.pdfGenerator = pdfGenerator;
        this.hashService = hashService;
        this.clock = clock;
    }

    @Override
    public Result<Sealing, ApplicationError> handle(SealAgronomicDossierCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "settlement.command.null"));
        }
        final PlotId plotId;
        final CampaignYear campaignYear;
        try {
            plotId = new PlotId(command.plotId());
            campaignYear = new CampaignYear(command.campaignYear());
            new UserId(command.actorId());
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError("request", exception.getMessage()));
        }
        var owner = orchard.findActivePlotOwner(plotId);
        if (owner.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Plot", plotId.plotId()));
        }
        if (!owner.get().equals(new UserId(command.actorId()))) {
            return Result.failure(ApplicationError.forbidden("Plot", "settlement.plot.not_owner"));
        }
        var existing = dossierRepository.findByPlotIdAndCampaignYear(plotId, campaignYear);
        if (existing.isPresent()) {
            return Result.success(new Sealing(existing.get(), false));
        }
        var settlement = reportRepository.findByPlotId(plotId).flatMap(report -> report.settlementOf(campaignYear));
        if (settlement.isEmpty()) {
            return Result.failure(ApplicationError.businessRuleViolation(RESOURCE,
                    "settlement.dossier.campaign_not_settled"));
        }

        Instant sealedAt = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        var content = content(plotId, owner.get(), campaignYear, settlement.get(), sealedAt);
        final SealedDossier.Sealed sealed;
        try {
            sealed = SealedDossier.seal(content, pdfGenerator, hashService);
        } catch (DossierRenderingException exception) {
            return Result.failure(ApplicationError.unexpected(RESOURCE, "settlement.dossier.render.failed"));
        }
        dossierRepository.save(sealed.dossier().snapshot(), sealed.document());
        return Result.success(new Sealing(sealed.dossier().snapshot(), true));
    }

    private SealedDossierContent content(PlotId plotId, UserId producerId, CampaignYear campaignYear,
            HarvestSettlementSnapshot settlement, Instant sealedAt) {
        var plot = orchard.describePlot(plotId);
        var plan = thinning.findCampaignThinningPlan(plotId, campaignYear).orElse(null);
        var balance = settlement.thinningBalance();
        // The campaign runs from the first of January to the settlement: the climate it actually went through
        Instant campaignStart = LocalDate.of(campaignYear.value(), 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        var climate = telemetry.findClimateCoverage(plotId, campaignStart, settlement.settledAt());

        // Producer-recorded history up to this campaign; the settled weight is the official figure of this one
        SortedMap<Integer, Double> history = new TreeMap<>(phenology.findHistoricalYields(plotId)
                .headMap(campaignYear.value()));
        history.put(campaignYear.value(), settlement.totalHarvestWeight().kilograms());

        var facts = new DossierFacts(
                plot.name(),
                plot.variety(),
                plot.areaHectares(),
                plan == null ? null : plan.preThinningFruitsPerShoot(),
                plan == null ? null : plan.targetFruitsPerShoot(),
                balance != null && balance.prescribedRemovalPercentage() != null
                        ? balance.prescribedRemovalPercentage()
                        : plan == null ? null : plan.percentageToRemove(),
                plan == null ? null : plan.windowOpensOn(),
                plan == null ? null : plan.windowClosesOn(),
                balance == null ? null : balance.actualRemovalPercentage(),
                balance == null ? null : balance.executedDate(),
                balance == null ? "NOT_RECORDED" : balance.status().name(),
                settlement.totalHarvestWeight().kilograms(),
                settlement.greenOlivesWeight().kilograms(),
                settlement.blackOlivesWeight().kilograms(),
                settlement.receiptNumber() == null ? null : settlement.receiptNumber().value(),
                settlement.weighedOn() == null ? null : settlement.weighedOn().value(),
                climate.linkedNodes(),
                climate.daysWithReadings(),
                history.firstKey(),
                history.lastKey(),
                history.size());
        return new SealedDossierContent(plotId, producerId, campaignYear, facts, settlement, history, sealedAt);
    }
}
