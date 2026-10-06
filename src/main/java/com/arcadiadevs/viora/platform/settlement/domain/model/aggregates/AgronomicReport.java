package com.arcadiadevs.viora.platform.settlement.domain.model.aggregates;

import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierAlreadyCertifiedException;
import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;
import com.arcadiadevs.viora.platform.settlement.domain.exceptions.InsufficientSettlementHistoryException;
import com.arcadiadevs.viora.platform.settlement.domain.model.entities.DossierCertification;
import com.arcadiadevs.viora.platform.settlement.domain.model.entities.HarvestSettlement;
import com.arcadiadevs.viora.platform.settlement.domain.model.events.AgronomicDossierGeneratedEvent;
import com.arcadiadevs.viora.platform.settlement.domain.model.events.CampaignHarvestSettledEvent;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.AgronomicDossierContent;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.AgronomicDossierPdfGenerator;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.services.CryptographicHashService;
import com.arcadiadevs.viora.platform.settlement.domain.services.StabilizationCurveCalculatorService;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    /**
     * Message key of the rule that a campaign can only be settled once. Exposed so a caller can rule the conflict
     * out before doing work that would have to be undone, such as consuming a receipt number.
     */
    public static final String CAMPAIGN_ALREADY_SETTLED = "settlement.campaign.already_settled";

    private final ReportId id;
    private final PlotId plotId;
    private final UserId producerId;
    private final List<HarvestSettlementSnapshot> settlements;
    private final List<DossierCertificationSnapshot> certifications;
    private final Long revision;

    private AgronomicReport(ReportId id, PlotId plotId, UserId producerId,
            List<HarvestSettlementSnapshot> settlements, List<DossierCertificationSnapshot> certifications,
            Long revision) {
        this.id = id;
        this.plotId = plotId;
        this.producerId = producerId;
        this.settlements = new ArrayList<>(settlements);
        this.certifications = new ArrayList<>(certifications);
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
        return new AgronomicReport(new ReportId(), plotId, producerId, List.of(), List.of(), null);
    }

    public static AgronomicReport reconstitute(AgronomicReportSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("settlement.report.snapshot.null");
        }
        return new AgronomicReport(snapshot.id(), snapshot.plotId(), snapshot.producerId(),
                snapshot.settlements(), snapshot.certifications(), snapshot.revision());
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
     * @param receiptNumber         official receipt number allocated to this settlement
     * @param weighedOn             date the delivered olives were weighed
     * @param millTicketNumber      optional ticket number of the receiving mill
     * @param idempotencyKey        optional key the settlement is registered with
     * @param clock                 clock stamping the settlement
     * @return the new settlement
     */
    public HarvestSettlementSnapshot settleCampaign(CampaignYear campaignYear, OliveWeight green, OliveWeight black,
            Double commercialFruitsPerKg, String notes, ThinningBalance thinningBalance,
            SortedMap<Integer, Double> historicalYields, ReceiptNumber receiptNumber, WeighingDate weighedOn,
            MillTicketNumber millTicketNumber, IdempotencyKey idempotencyKey, Clock clock) {
        if (campaignYear == null) {
            throw new IllegalArgumentException("settlement.campaign_year.null");
        }
        if (settlementOf(campaignYear).isPresent()) {
            throw new IllegalStateException(CAMPAIGN_ALREADY_SETTLED);
        }
        var total = HarvestSettlement.calculateTotalWeight(green, black);

        SortedMap<Integer, Double> settledYields = new TreeMap<>();
        settlements.forEach(s -> settledYields.put(s.campaignYear().value(), s.totalHarvestWeight().kilograms()));
        settledYields.put(campaignYear.value(), total.kilograms());
        var curve = StabilizationCurveCalculatorService.computeCurve(historicalYields, settledYields);

        var settlement = HarvestSettlement.create(id, plotId, campaignYear, green, black, commercialFruitsPerKg,
                notes, receiptNumber, weighedOn, millTicketNumber, idempotencyKey, thinningBalance, curve,
                clock.instant()).snapshot();
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

    /**
     * Certifies the dossier of one settled campaign: renders its PDF from the frozen settlement, hashes the exact
     * final bytes, appends the immutable certification metadata and registers {@link AgronomicDossierGeneratedEvent}.
     *
     * <p>Existing settlements and certifications are never touched, so certifying campaign N+1 keeps the bytes,
     * hash and metadata of campaign N. One instant is read from the clock and used for the whole operation.</p>
     *
     * @param campaignYear campaign to certify
     * @param signature    declared collegiate signature
     * @param certifier    declared certifying professional
     * @param notes        validated optional notes
     * @param pdfGenerator output port rendering the frozen content
     * @param hashService  service computing the SHA-256 of the rendered bytes
     * @param clock        clock stamping the certification
     * @return the new certification metadata together with the rendered document, which the caller must store
     *         through {@code CertifiedDossierDocumentRepository}
     * @throws IllegalArgumentException            if an argument is missing
     * @throws BusinessRuleException               if the campaign has no settlement
     * @throws DossierAlreadyCertifiedException    if the campaign is already certified
     * @throws InsufficientSettlementHistoryException if its frozen curve lacks the consecutive settled campaigns
     *                                             needed for a managed alternation index, whatever the baseline
     * @throws DossierRenderingException           if the PDF cannot be rendered; nothing is certified
     */
    public CertifiedDossier certifyCampaign(CampaignYear campaignYear, AuditorSignature signature,
            CertifierIdentity certifier, CertificationNotes notes, AgronomicDossierPdfGenerator pdfGenerator,
            CryptographicHashService hashService, Clock clock) {
        if (campaignYear == null) {
            throw new IllegalArgumentException("settlement.campaign_year.null");
        }
        if (signature == null || certifier == null || notes == null || pdfGenerator == null || hashService == null
                || clock == null) {
            throw new IllegalArgumentException("settlement.certification.reference.null");
        }
        var settlement = settlementOf(campaignYear).orElseThrow(
                () -> new BusinessRuleException("settlement.certification.campaign_not_settled"));
        if (certificationOf(campaignYear).isPresent()) {
            // Pending decision (ADR-002 section 8): this conflict and the insufficient-history one below end up
            // with the same ProblemDetail code (DOSSIERCERTIFICATION_CONFLICT), so clients can only tell them
            // apart by the localized detail. To be decided with the Settlement owner (Victor): whether
            // insufficient settlement history gets its own code so clients can react programmatically.
            throw new DossierAlreadyCertifiedException("settlement.certification.already_certified");
        }
        // Judged on the settled history itself: the curve status reports a missing baseline first and would
        // hide insufficient settlements on plots without Phenology history.
        if (settlement.trendCurve().managedAlternationIndex() == null) {
            // Pending decision (ADR-002 section 8), to be taken with the Settlement owner (Victor):
            // (a) give this failure its own ProblemDetail code instead of the shared conflict code;
            // (b) decide whether it stays 409 (ZIP/audit 21 policy) or moves to 422 like the other unmet
            //     campaign precondition ("no settlement" is 422 in TS40), since it is a precondition rather
            //     than a state conflict.
            throw new InsufficientSettlementHistoryException("settlement.certification.insufficient_settlements");
        }

        // One instant for the whole operation; microseconds are what the database keeps on reload.
        Instant certifiedAt = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        var content = new AgronomicDossierContent(id, plotId, producerId, campaignYear, settlement, certifier,
                signature, notes.value(), certifiedAt);
        DossierDocument document;
        try {
            document = new DossierDocument(pdfGenerator.renderPdf(content));
        } catch (IllegalArgumentException e) {
            throw new DossierRenderingException("settlement.certification.render.invalid_output", e);
        }
        var metadata = new DossierMetadata(hashService.sha256(document.content()), signature, certifiedAt);

        var certification = DossierCertification.create(id, plotId, campaignYear, metadata, certifier,
                notes).snapshot();
        certifications.add(certification);
        registerDomainEvent(new AgronomicDossierGeneratedEvent(UUID.randomUUID().toString(),
                certification.id().certificationId(), id.reportId(), plotId.plotId(), campaignYear.value(),
                metadata.verificationHash().value(), signature.value(), certifiedAt));
        return new CertifiedDossier(certification, document);
    }

    /**
     * Selects the certification of a campaign.
     *
     * @param campaignYear campaign to look for
     * @return the certification, if the campaign is certified
     */
    public Optional<DossierCertificationSnapshot> certificationOf(CampaignYear campaignYear) {
        return certifications.stream().filter(c -> c.campaignYear().equals(campaignYear)).findFirst();
    }

    /** Curve of the latest settled campaign, or empty when nothing is settled yet. */
    public Optional<StabilizationTrendCurve> trendCurve() {
        return settlements.stream()
                .max(Comparator.comparing(s -> s.campaignYear().value()))
                .map(HarvestSettlementSnapshot::trendCurve);
    }

    public AgronomicReportSnapshot snapshot() {
        return new AgronomicReportSnapshot(id, plotId, producerId, settlements, certifications, revision);
    }
}
