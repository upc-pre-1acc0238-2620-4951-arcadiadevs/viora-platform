package com.arcadiadevs.viora.platform.settlement.domain.model.aggregates;

import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;
import com.arcadiadevs.viora.platform.settlement.domain.model.events.AgronomicDossierGeneratedEvent;
import com.arcadiadevs.viora.platform.settlement.domain.model.events.CampaignHarvestSettledEvent;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.AgronomicDossierContent;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.AgronomicDossierPdfGenerator;
import com.arcadiadevs.viora.platform.settlement.domain.services.CryptographicHashService;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AgronomicReportTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC);
    private final PlotId plotId = new PlotId(UUID.randomUUID().toString());
    private final UserId producer = new UserId(UUID.randomUUID().toString());

    private HarvestSettlementSnapshot settle(AgronomicReport report, int year, double green, double black) {
        return report.settleCampaign(new CampaignYear(year), new OliveWeight(green), new OliveWeight(black),
                null, null, ThinningBalance.notRecorded(), new TreeMap<>(), CLOCK);
    }

    @Test
    void settlesACampaignWithItsTotalAndPublishesTheEvent() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        var settlement = report.settleCampaign(new CampaignYear(2026), new OliveWeight(8200.0),
                new OliveWeight(6050.0), 105.0, "Weights verified", ThinningBalance.notRecorded(), new TreeMap<>(), CLOCK);
        assertEquals(14250.0, settlement.totalHarvestWeight().kilograms());
        assertEquals(SettlementStatus.SETTLED, settlement.status());
        assertEquals(CLOCK.instant(), settlement.settledAt());
        assertEquals(report.snapshot().id(), settlement.reportId());
        assertEquals(plotId, settlement.plotId());
        assertEquals(StabilizationStatus.INSUFFICIENT_BASELINE, settlement.trendCurve().status());
        var event = (CampaignHarvestSettledEvent) report.domainEvents().iterator().next();
        assertEquals(settlement.id().settlementId(), event.settlementId());
        assertEquals(plotId.plotId(), event.plotId());
        assertEquals(2026, event.campaignYear());
        assertEquals(14250.0, event.totalYieldKg());
        assertEquals(105.0, event.commercialFruitsPerKg());
    }

    @Test
    void rejectsSettlingTheSameCampaignTwiceWithoutChangingIt() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        var first = settle(report, 2026, 8200, 6050);
        assertThrows(IllegalStateException.class, () -> settle(report, 2026, 1, 1));
        assertEquals(1, report.snapshot().settlements().size());
        assertEquals(first, report.settlementOf(new CampaignYear(2026)).orElseThrow());
        assertEquals(1, report.domainEvents().size());
    }

    @Test
    void aLaterCampaignPreservesTheFrozenEarlierOne() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        var first = settle(report, 2026, 7000, 0);
        var second = settle(report, 2027, 0, 5000);
        assertEquals(first, report.settlementOf(new CampaignYear(2026)).orElseThrow());
        assertEquals(1, first.trendCurve().settledCampaigns());
        assertEquals(2, second.trendCurve().settledCampaigns());
        assertEquals(second.trendCurve(), report.trendCurve().orElseThrow());
    }

    @Test
    void freezesTheThinningBalanceOfTheCampaign() {
        var record = new ThinningExecutionRecord(UUID.randomUUID().toString(), UUID.randomUUID().toString(), plotId,
                2026, LocalDate.of(2026, 1, 10), 30.0, 25.0, true);
        var report = AgronomicReport.createForPlot(plotId, producer);
        var settlement = report.settleCampaign(new CampaignYear(2026), new OliveWeight(8200.0), new OliveWeight(0.0),
                null, null, ThinningBalance.of(record), new TreeMap<>(), CLOCK);
        var balance = settlement.thinningBalance();
        assertEquals(ThinningComplianceStatus.EXECUTED_ON_TIME, balance.status());
        assertEquals(30.0, balance.prescribedRemovalPercentage());
        assertEquals(25.0, balance.actualRemovalPercentage());
        assertEquals(-5.0, balance.deviationPercentagePoints());
    }

    @Test
    void classifiesLateAndMissingThinning() {
        var late = new ThinningExecutionRecord("e", UUID.randomUUID().toString(), plotId, 2026,
                LocalDate.of(2026, 2, 1), null, 20.0, false);
        assertEquals(ThinningComplianceStatus.EXECUTED_LATE, ThinningBalance.of(late).status());
        assertNull(ThinningBalance.of(late).deviationPercentagePoints());
        assertEquals(ThinningComplianceStatus.NOT_RECORDED, ThinningBalance.of(null).status());
    }

    @Test
    void rejectsAZeroTotal() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        assertThrows(IllegalArgumentException.class, () -> settle(report, 2026, 0, 0));
        assertTrue(report.snapshot().settlements().isEmpty());
        assertTrue(report.domainEvents().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidWeights(double kilograms) {
        assertThrows(IllegalArgumentException.class, () -> new OliveWeight(kilograms));
    }

    @ParameterizedTest
    @ValueSource(ints = {1999, 2101})
    void rejectsCampaignsOutsideTheSettlementRange(int year) {
        assertThrows(IllegalArgumentException.class, () -> new CampaignYear(year));
    }

    @Test
    void rejectsInvalidCaliberAndOverlongNotes() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        assertThrows(IllegalArgumentException.class, () -> report.settleCampaign(new CampaignYear(2026),
                new OliveWeight(1.0), new OliveWeight(1.0), 0.0, null, ThinningBalance.notRecorded(), new TreeMap<>(), CLOCK));
        assertThrows(IllegalArgumentException.class, () -> report.settleCampaign(new CampaignYear(2026),
                new OliveWeight(1.0), new OliveWeight(1.0), null, "x".repeat(1001), ThinningBalance.notRecorded(),
                new TreeMap<>(), CLOCK));
        assertTrue(report.snapshot().settlements().isEmpty());
    }

    // --- certification ---

    private final CryptographicHashService hashService = new CryptographicHashService();
    private final List<AgronomicDossierContent> rendered = new ArrayList<>();
    private int renderCount;
    private final AgronomicDossierPdfGenerator generator = content -> {
        rendered.add(content);
        renderCount++;
        return ("%PDF-1.7 campaign " + content.campaignYear().value() + " render " + renderCount)
                .getBytes(StandardCharsets.UTF_8);
    };
    private final AuditorSignature signature = new AuditorSignature("CIP-49120-ING-AGRONOMO-SANCHEZ");
    private final CertifierIdentity certifier = new CertifierIdentity("Ing. Sanchez", "49120");
    private static final Clock CERTIFICATION_CLOCK =
            Clock.fixed(Instant.parse("2026-10-03T09:30:00.123456Z"), ZoneOffset.UTC);

    private static SortedMap<Integer, Double> history() {
        var history = new TreeMap<Integer, Double>();
        history.put(2022, 10000.0);
        history.put(2023, 2000.0);
        history.put(2024, 9000.0);
        history.put(2025, 3000.0);
        return history;
    }

    private void settleWithHistory(AgronomicReport report, int year, double kilograms) {
        report.settleCampaign(new CampaignYear(year), new OliveWeight(kilograms), new OliveWeight(0.0), null, null,
                ThinningBalance.notRecorded(), history(), CLOCK);
    }

    private DossierCertificationSnapshot certify(AgronomicReport report, int year) {
        return report.certifyCampaign(new CampaignYear(year), signature, certifier, "Verified", generator,
                hashService, CERTIFICATION_CLOCK);
    }

    @Test
    void certifiesASettledCampaignAndPublishesTheEvent() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settle(report, 2026, 8200, 6050);
        report.clearDomainEvents();

        var certification = certify(report, 2026);

        assertEquals(report.snapshot().id(), certification.reportId());
        assertEquals(plotId, certification.plotId());
        assertEquals(new CampaignYear(2026), certification.campaignYear());
        assertEquals(signature, certification.metadata().auditorSignature());
        assertEquals(certifier, certification.certifier());
        assertEquals("Verified", certification.notes());
        assertEquals(certification, report.certificationOf(new CampaignYear(2026)).orElseThrow());
        assertEquals(1, report.snapshot().certifications().size());
        assertEquals(1, report.domainEvents().size());
        var event = (AgronomicDossierGeneratedEvent) report.domainEvents().iterator().next();
        assertEquals(certification.id().certificationId(), event.certificationId());
        assertEquals(report.snapshot().id().reportId(), event.reportId());
        assertEquals(plotId.plotId(), event.plotId());
        assertEquals(2026, event.campaignYear());
        assertEquals(certification.metadata().verificationHash().value(), event.verificationHash());
        assertEquals(signature.value(), event.auditorSignature());
        assertEquals(certification.metadata().certifiedAt(), event.certifiedAt());
        assertNotNull(event.eventId());
    }

    @Test
    void hashesTheExactStoredBytesAndStampsOneClockInstant() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settle(report, 2026, 8200, 6050);

        var certification = certify(report, 2026);

        var bytes = certification.document().content();
        assertEquals(hashService.sha256(bytes), certification.metadata().verificationHash());
        assertEquals(CERTIFICATION_CLOCK.instant(), certification.metadata().certifiedAt());
        assertEquals(1, rendered.size());
        assertEquals(certification.metadata().certifiedAt(), rendered.getFirst().certifiedAt());
    }

    @Test
    void rendersTheFrozenSettlementOfThatCampaignOnly() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        var first = settle(report, 2026, 7000, 0);
        settle(report, 2027, 0, 5000);

        certify(report, 2026);

        var content = rendered.getFirst();
        assertEquals(first, content.settlement());
        assertEquals(new CampaignYear(2026), content.campaignYear());
        assertEquals(producer, content.producerId());
        assertEquals(signature, content.signature());
        assertEquals(certifier, content.certifier());
    }

    @Test
    void rejectsACampaignWithoutSettlement() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settle(report, 2026, 100, 100);
        report.clearDomainEvents();
        assertThrows(BusinessRuleException.class, () -> certify(report, 2027));
        assertTrue(report.snapshot().certifications().isEmpty());
        assertTrue(report.domainEvents().isEmpty());
        assertEquals(0, renderCount);
    }

    @Test
    void rejectsACertificationOfAReportWithoutAnySettlement() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        assertThrows(BusinessRuleException.class, () -> certify(report, 2026));
    }

    @Test
    void rejectsCertifyingTheSameCampaignTwiceWithoutChangingTheFirst() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settle(report, 2026, 100, 100);
        var first = certify(report, 2026);
        report.clearDomainEvents();

        var error = assertThrows(IllegalStateException.class, () -> certify(report, 2026));

        assertEquals("settlement.certification.already_certified", error.getMessage());
        assertEquals(List.of(first), report.snapshot().certifications());
        assertTrue(report.domainEvents().isEmpty());
        assertEquals(1, renderCount);
    }

    @Test
    void rejectsACampaignWhoseFrozenCurveHasInsufficientSettlements() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settleWithHistory(report, 2026, 7000);
        assertEquals(StabilizationStatus.INSUFFICIENT_SETTLEMENTS,
                report.settlementOf(new CampaignYear(2026)).orElseThrow().trendCurve().status());
        report.clearDomainEvents();

        var error = assertThrows(IllegalStateException.class, () -> certify(report, 2026));

        assertEquals("settlement.certification.insufficient_settlements", error.getMessage());
        assertTrue(report.snapshot().certifications().isEmpty());
        assertTrue(report.domainEvents().isEmpty());
        assertEquals(0, renderCount);
    }

    @Test
    void certifiesWhenTheBaselineIsMissingOrTheCurveIsEvaluated() {
        var withoutBaseline = AgronomicReport.createForPlot(plotId, producer);
        settle(withoutBaseline, 2026, 100, 100);
        assertEquals(StabilizationStatus.INSUFFICIENT_BASELINE,
                withoutBaseline.settlementOf(new CampaignYear(2026)).orElseThrow().trendCurve().status());
        assertDoesNotThrow(() -> certify(withoutBaseline, 2026));

        var evaluated = AgronomicReport.createForPlot(plotId, producer);
        settleWithHistory(evaluated, 2026, 7000);
        settleWithHistory(evaluated, 2027, 5000);
        settleWithHistory(evaluated, 2028, 6500);
        assertEquals(StabilizationStatus.EVALUATED,
                evaluated.settlementOf(new CampaignYear(2028)).orElseThrow().trendCurve().status());
        assertDoesNotThrow(() -> certify(evaluated, 2028));
        // the first two campaigns keep their frozen insufficient curve and stay uncertifiable
        assertThrows(IllegalStateException.class, () -> certify(evaluated, 2026));
    }

    @Test
    void certifyingALaterCampaignKeepsTheEarlierCertificationIdentical() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settle(report, 2026, 100, 100);
        var first = certify(report, 2026);
        var firstBytes = first.document().content();
        var firstHash = first.metadata().verificationHash();

        settle(report, 2027, 300, 100);
        var second = certify(report, 2027);

        var reloaded = report.certificationOf(new CampaignYear(2026)).orElseThrow();
        assertEquals(first, reloaded);
        assertArrayEquals(firstBytes, reloaded.document().content());
        assertEquals(firstHash, reloaded.metadata().verificationHash());
        assertEquals(first.metadata(), reloaded.metadata());
        assertEquals(2, report.snapshot().certifications().size());
        assertNotEquals(firstHash, second.metadata().verificationHash());
        assertEquals(second, report.certificationOf(new CampaignYear(2027)).orElseThrow());
    }

    @Test
    void settlementsAreUntouchedByCertification() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        var settlement = settle(report, 2026, 100, 100);
        certify(report, 2026);
        assertEquals(settlement, report.settlementOf(new CampaignYear(2026)).orElseThrow());
        assertEquals(1, report.snapshot().settlements().size());
    }

    @Test
    void theSnapshotExposesDefensiveCopiesOfTheDocument() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settle(report, 2026, 100, 100);
        var certification = certify(report, 2026);
        var original = certification.document().content();

        var tampered = certification.document().content();
        tampered[tampered.length - 1] ^= 0x01;

        assertArrayEquals(original, report.snapshot().certifications().getFirst().document().content());
        assertEquals(certification.metadata().verificationHash(), hashService.sha256(
                report.snapshot().certifications().getFirst().document().content()));
        assertThrows(UnsupportedOperationException.class, () -> report.snapshot().certifications().clear());
    }

    @Test
    void aFailingRendererCertifiesNothing() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settle(report, 2026, 100, 100);
        report.clearDomainEvents();
        AgronomicDossierPdfGenerator failing = content -> {
            throw new DossierRenderingException("settlement.certification.render.failed");
        };
        assertThrows(DossierRenderingException.class, () -> report.certifyCampaign(new CampaignYear(2026), signature,
                certifier, null, failing, hashService, CERTIFICATION_CLOCK));
        assertTrue(report.snapshot().certifications().isEmpty());
        assertTrue(report.domainEvents().isEmpty());
    }

    @Test
    void aRendererReturningNonPdfBytesIsATechnicalFailure() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settle(report, 2026, 100, 100);
        AgronomicDossierPdfGenerator notPdf = content -> "{\"json\":true}".getBytes(StandardCharsets.UTF_8);
        assertThrows(DossierRenderingException.class, () -> report.certifyCampaign(new CampaignYear(2026), signature,
                certifier, null, notPdf, hashService, CERTIFICATION_CLOCK));
        assertTrue(report.snapshot().certifications().isEmpty());
    }

    @Test
    void rejectsMissingArgumentsAndOverlongNotes() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settle(report, 2026, 100, 100);
        var year = new CampaignYear(2026);
        assertThrows(IllegalArgumentException.class, () ->
                report.certifyCampaign(null, signature, certifier, null, generator, hashService, CERTIFICATION_CLOCK));
        assertThrows(IllegalArgumentException.class, () ->
                report.certifyCampaign(year, null, certifier, null, generator, hashService, CERTIFICATION_CLOCK));
        assertThrows(IllegalArgumentException.class, () ->
                report.certifyCampaign(year, signature, null, null, generator, hashService, CERTIFICATION_CLOCK));
        assertThrows(IllegalArgumentException.class, () ->
                report.certifyCampaign(year, signature, certifier, "x".repeat(1001), generator, hashService,
                        CERTIFICATION_CLOCK));
        assertTrue(report.snapshot().certifications().isEmpty());
    }

    @Test
    void reconstitutionKeepsTheCertifications() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settle(report, 2026, 100, 100);
        var certification = certify(report, 2026);
        var restored = AgronomicReport.reconstitute(report.snapshot());
        assertEquals(certification, restored.certificationOf(new CampaignYear(2026)).orElseThrow());
        assertTrue(restored.domainEvents().isEmpty());
    }
}
