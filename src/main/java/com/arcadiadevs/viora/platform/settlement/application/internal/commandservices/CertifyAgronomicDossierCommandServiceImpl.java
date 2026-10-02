package com.arcadiadevs.viora.platform.settlement.application.internal.commandservices;

import com.arcadiadevs.viora.platform.settlement.application.commandservices.CertifyAgronomicDossierCommandService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.CertifiedDossier;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.DossierCertificationSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.CertifyAgronomicDossierCommand;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.AgronomicDossierPdfGenerator;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.AuditorSignature;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CertificationNotes;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CertifierIdentity;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.CertifiedDossierDocumentRepository;
import com.arcadiadevs.viora.platform.settlement.domain.services.CryptographicHashService;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceConflictException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Certifies one settled campaign: verifies the plot, locks the plot report, lets the aggregate render, hash and
 * append the certification, then persists the report and the immutable document in the same transaction and
 * publishes {@code AgronomicDossierGeneratedEvent}.
 *
 * <p>Error mapping: all user input is validated by value objects before the domain is called (invalid input is a
 * 400, anything the domain then throws as {@link IllegalArgumentException} or {@link IllegalStateException} is a
 * programming or infrastructure fault and propagates to the global handler as a 500); missing or inactive plot is
 * a not-found; a campaign without settlement (including a plot
 * without report) is a business-rule violation (422); an already certified campaign or a campaign whose frozen
 * curve lacks at least three consecutive settled campaigns (two consecutive pairs, whatever the baseline) is a
 * {@link ResourceConflictException} (409); a rendering failure is an unexpected error (500) and
 * leaves nothing stored and nothing published. Concurrent certifications are serialized by the report lock and
 * guarded by the unique constraint on report and campaign.</p>
 */
@Service
@Transactional
public class CertifyAgronomicDossierCommandServiceImpl implements CertifyAgronomicDossierCommandService {
    private static final String RESOURCE = "DossierCertification";

    private final AgronomicReportRepository reportRepository;
    private final CertifiedDossierDocumentRepository documentRepository;
    private final ExternalOrchardService externalOrchardService;
    private final AgronomicDossierPdfGenerator pdfGenerator;
    private final CryptographicHashService hashService;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public CertifyAgronomicDossierCommandServiceImpl(AgronomicReportRepository reportRepository,
            CertifiedDossierDocumentRepository documentRepository, ExternalOrchardService externalOrchardService,
            AgronomicDossierPdfGenerator pdfGenerator,
            CryptographicHashService hashService, ApplicationEventPublisher publisher, Clock clock) {
        this.reportRepository = reportRepository;
        this.documentRepository = documentRepository;
        this.externalOrchardService = externalOrchardService;
        this.pdfGenerator = pdfGenerator;
        this.hashService = hashService;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public Result<DossierCertificationSnapshot, ApplicationError> handle(CertifyAgronomicDossierCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "settlement.command.null"));
        }
        PlotId plotId;
        CampaignYear campaignYear;
        AuditorSignature signature;
        CertifierIdentity certifier;
        CertificationNotes notes;
        try {
            plotId = new PlotId(command.plotId());
            campaignYear = new CampaignYear(command.campaignYear());
            signature = new AuditorSignature(command.auditorSignature());
            certifier = new CertifierIdentity(command.certifiedBy(), command.cipNumber());
            notes = new CertificationNotes(command.notes());
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError("certification", exception.getMessage()));
        }
        if (externalOrchardService.findActivePlotOwner(plotId).isEmpty()) {
            return Result.failure(ApplicationError.notFound("Plot", plotId.plotId()));
        }
        var report = reportRepository.findByPlotIdForUpdate(plotId).orElse(null);
        if (report == null) {
            return Result.failure(ApplicationError.businessRuleViolation(RESOURCE,
                    "settlement.certification.campaign_not_settled"));
        }

        CertifiedDossier certified;
        try {
            certified = report.certifyCampaign(campaignYear, signature, certifier, notes, pdfGenerator,
                    hashService, clock);
        } catch (BusinessRuleException exception) {
            return Result.failure(ApplicationError.businessRuleViolation(RESOURCE, exception.getMessage()));
        } catch (ResourceConflictException exception) {
            // Pending decision (ADR-002 section 8), to be taken with the Settlement owner (Victor): both
            // conflicts (already certified, insufficient settlement history) map to the same ProblemDetail code,
            // DOSSIERCERTIFICATION_CONFLICT, so clients only tell them apart by the localized detail. Decide
            // (a) whether insufficient history gets its own conflict code so clients can react programmatically,
            // and (b) whether it stays 409 (ZIP/audit 21 policy) or moves to 422 like the other unmet campaign
            // precondition (TS40 uses 422 for "no settlement"), since it is a precondition, not a state conflict.
            return Result.failure(ApplicationError.conflict(RESOURCE, exception.getMessage()));
        } catch (DossierRenderingException exception) {
            return Result.failure(ApplicationError.unexpected(RESOURCE, "settlement.certification.render.failed"));
        }
        reportRepository.save(report);
        documentRepository.save(certified.certification().id(), certified.document());
        report.domainEvents().forEach(publisher::publishEvent);
        report.clearDomainEvents();
        return Result.success(certified.certification());
    }
}
