package com.arcadiadevs.viora.platform.settlement.application.internal.commandservices;

import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.CertifyAgronomicDossierCommand;
import com.arcadiadevs.viora.platform.settlement.domain.model.events.AgronomicDossierGeneratedEvent;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.AgronomicDossierPdfGenerator;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.CertifiedDossierDocumentRepository;
import com.arcadiadevs.viora.platform.settlement.domain.services.CryptographicHashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CertifyAgronomicDossierCommandServiceImplTest {
    private static final Clock SETTLEMENT_CLOCK = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC);
    private static final Clock CERTIFICATION_CLOCK = Clock.fixed(Instant.parse("2026-10-03T09:30:00Z"), ZoneOffset.UTC);
    private final AgronomicReportRepository reports = mock(AgronomicReportRepository.class);
    private final CertifiedDossierDocumentRepository documents = mock(CertifiedDossierDocumentRepository.class);
    private final ExternalOrchardService orchard = mock(ExternalOrchardService.class);
    private final AgronomicDossierPdfGenerator pdf = mock(AgronomicDossierPdfGenerator.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final CertifyAgronomicDossierCommandServiceImpl service = new CertifyAgronomicDossierCommandServiceImpl(
            reports, documents, orchard, pdf, new CryptographicHashService(), publisher, CERTIFICATION_CLOCK);
    private final String plotId = UUID.randomUUID().toString();
    private final String owner = UUID.randomUUID().toString();
    private CertifyAgronomicDossierCommand command;

    @BeforeEach
    void setUp() {
        command = new CertifyAgronomicDossierCommand(plotId, 2028, "CIP-49120-SANCHEZ", "Ing. Sanchez", "49120",
                "Verified");
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(owner)));
        when(reports.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pdf.renderPdf(any())).thenReturn("%PDF-1.7 dossier".getBytes(StandardCharsets.US_ASCII));
    }

    private AgronomicReport reportWithSettlement(int year, java.util.SortedMap<Integer, Double> history) {
        var report = AgronomicReport.createForPlot(new PlotId(plotId), new UserId(owner));
        report.settleCampaign(new CampaignYear(year), new OliveWeight(1000.0), new OliveWeight(0.0), null, null,
                ThinningBalance.notRecorded(), history, SETTLEMENT_CLOCK);
        report.clearDomainEvents();
        return report;
    }

    private AgronomicReport reportWithThreeConsecutiveSettlements() {
        var report = AgronomicReport.createForPlot(new PlotId(plotId), new UserId(owner));
        double[] kilograms = {1000.0, 3000.0, 1500.0};
        for (int i = 0; i < kilograms.length; i++) {
            report.settleCampaign(new CampaignYear(2026 + i), new OliveWeight(kilograms[i]), new OliveWeight(0.0), null,
                    null, ThinningBalance.notRecorded(), new TreeMap<>(), SETTLEMENT_CLOCK);
        }
        report.clearDomainEvents();
        return report;
    }

    private static TreeMap<Integer, Double> alternatingHistory() {
        var history = new TreeMap<Integer, Double>();
        history.put(2022, 10000.0);
        history.put(2023, 2000.0);
        history.put(2024, 9000.0);
        history.put(2025, 3000.0);
        return history;
    }

    @Test
    void returnsNotFoundForAMissingOrInactivePlotWithoutTouchingTheReport() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.empty());
        var error = service.handle(command).failure().orElseThrow();
        assertEquals("PLOT_NOT_FOUND", error.code());
        verifyNoInteractions(reports, documents, publisher, pdf);
    }

    @Test
    void returnsUnprocessableWhenThePlotHasNoReport() {
        when(reports.findByPlotIdForUpdate(any())).thenReturn(Optional.empty());
        var error = service.handle(command).failure().orElseThrow();
        assertEquals("BUSINESS_RULE_VIOLATION", error.code());
        assertEquals("settlement.certification.campaign_not_settled", error.details());
        verify(reports, never()).save(any());
        verifyNoInteractions(documents, publisher, pdf);
    }

    @Test
    void returnsUnprocessableWhenTheCampaignIsNotSettled() {
        when(reports.findByPlotIdForUpdate(any())).thenReturn(Optional.of(reportWithSettlement(2025, new TreeMap<>())));
        var error = service.handle(command).failure().orElseThrow();
        assertEquals("BUSINESS_RULE_VIOLATION", error.code());
        assertEquals("settlement.certification.campaign_not_settled", error.details());
        verify(reports, never()).save(any());
        verifyNoInteractions(documents, publisher, pdf);
    }

    @Test
    void returnsConflictWhenTheFrozenCurveHasInsufficientSettlements() {
        when(reports.findByPlotIdForUpdate(any())).thenReturn(
                Optional.of(reportWithSettlement(2028, alternatingHistory())));
        var error = service.handle(command).failure().orElseThrow();
        assertEquals("DOSSIERCERTIFICATION_CONFLICT", error.code());
        assertEquals("settlement.certification.insufficient_settlements", error.details());
        verify(reports, never()).save(any());
        verifyNoInteractions(documents, publisher, pdf);
    }

    @Test
    void returnsConflictWhenTheCampaignIsAlreadyCertified() {
        var report = reportWithThreeConsecutiveSettlements();
        when(reports.findByPlotIdForUpdate(any())).thenReturn(Optional.of(report));
        assertTrue(service.handle(command).isSuccess());
        clearInvocations(reports, documents, publisher, pdf);

        var error = service.handle(command).failure().orElseThrow();

        assertEquals("DOSSIERCERTIFICATION_CONFLICT", error.code());
        assertEquals("settlement.certification.already_certified", error.details());
        verify(reports, never()).save(any());
        verifyNoInteractions(documents, publisher, pdf);
    }

    @Test
    void aRenderingFailureIsAnUnexpectedErrorThatStoresAndPublishesNothing() {
        when(reports.findByPlotIdForUpdate(any())).thenReturn(Optional.of(reportWithThreeConsecutiveSettlements()));
        when(pdf.renderPdf(any())).thenThrow(new DossierRenderingException("settlement.certification.render.failed"));

        var error = service.handle(command).failure().orElseThrow();

        assertEquals("UNEXPECTED_ERROR", error.code());
        assertEquals("settlement.certification.render.failed", error.details());
        verify(reports, never()).save(any());
        verifyNoInteractions(documents, publisher);
    }

    @Test
    void savesThenPublishesExactlyOneEventAndUsesTheInjectedClock() {
        var report = reportWithThreeConsecutiveSettlements();
        when(reports.findByPlotIdForUpdate(any())).thenReturn(Optional.of(report));

        var certification = service.handle(command).success().orElseThrow();

        assertEquals(CERTIFICATION_CLOCK.instant(), certification.metadata().certifiedAt());
        assertEquals("CIP-49120-SANCHEZ", certification.metadata().auditorSignature().value());
        assertEquals("Ing. Sanchez", certification.certifier().name());
        assertEquals("49120", certification.certifier().cipNumber());
        assertEquals("Verified", certification.notes());
        var order = inOrder(reports, documents, publisher);
        order.verify(reports).save(report);
        order.verify(documents).save(eq(certification.id()), any(DossierDocument.class));
        order.verify(publisher).publishEvent(any(AgronomicDossierGeneratedEvent.class));
        verify(publisher, times(1)).publishEvent(any(Object.class));
        assertTrue(report.domainEvents().isEmpty());
    }

    @Test
    void storesTheRenderedBytesExactlyOnceAndTheirHashMatchesTheCertification() {
        var report = reportWithThreeConsecutiveSettlements();
        when(reports.findByPlotIdForUpdate(any())).thenReturn(Optional.of(report));
        var rendered = "%PDF-1.7 dossier".getBytes(StandardCharsets.US_ASCII);
        var saved = org.mockito.ArgumentCaptor.forClass(DossierDocument.class);

        var certification = service.handle(command).success().orElseThrow();

        verify(documents, times(1)).save(eq(certification.id()), saved.capture());
        assertArrayEquals(rendered, saved.getValue().content());
        assertEquals(new CryptographicHashService().sha256(rendered), certification.metadata().verificationHash());
    }

    @Test
    void aRenderingFailureStoresNeitherTheReportNorTheDocument() {
        when(reports.findByPlotIdForUpdate(any())).thenReturn(Optional.of(reportWithThreeConsecutiveSettlements()));
        when(pdf.renderPdf(any())).thenThrow(new DossierRenderingException("settlement.certification.render.failed"));

        assertTrue(service.handle(command).failure().isPresent());

        verify(reports, never()).save(any());
        verify(documents, never()).save(any(), any());
    }

    @Test
    void publishesTheHashOfTheStoredBytes() {
        var report = reportWithThreeConsecutiveSettlements();
        when(reports.findByPlotIdForUpdate(any())).thenReturn(Optional.of(report));
        var captured = org.mockito.ArgumentCaptor.forClass(Object.class);

        var certification = service.handle(command).success().orElseThrow();

        verify(publisher).publishEvent(captured.capture());
        var event = (AgronomicDossierGeneratedEvent) captured.getValue();
        assertEquals(certification.metadata().verificationHash().value(), event.verificationHash());
        assertEquals(certification.id().certificationId(), event.certificationId());
        assertEquals(new CryptographicHashService().sha256("%PDF-1.7 dossier".getBytes(StandardCharsets.US_ASCII)),
                certification.metadata().verificationHash());
    }

    @Test
    void rejectsNullAndInvalidInputWithoutReachingThePortsOrTheDatabase() {
        assertEquals("VALIDATION_ERROR", service.handle(null).failure().orElseThrow().code());
        var badPlot = new CertifyAgronomicDossierCommand("not-a-uuid", 2026, "sig", "Name", "1", null);
        assertEquals("VALIDATION_ERROR", service.handle(badPlot).failure().orElseThrow().code());
        var badYear = new CertifyAgronomicDossierCommand(plotId, 1999, "sig", "Name", "1", null);
        assertEquals("VALIDATION_ERROR", service.handle(badYear).failure().orElseThrow().code());
        var longCip = new CertifyAgronomicDossierCommand(plotId, 2026, "sig", "Name", "1".repeat(21), null);
        assertEquals("VALIDATION_ERROR", service.handle(longCip).failure().orElseThrow().code());
        var longNotes = new CertifyAgronomicDossierCommand(plotId, 2026, "sig", "Name", "1", "x".repeat(1001));
        when(reports.findByPlotIdForUpdate(any())).thenReturn(Optional.of(reportWithSettlement(2026, new TreeMap<>())));
        assertEquals("VALIDATION_ERROR", service.handle(longNotes).failure().orElseThrow().code());
        verify(reports, never()).save(any());
        verifyNoInteractions(documents, publisher, pdf);
    }

    @Test
    void theCommandRequiresItsMandatoryFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new CertifyAgronomicDossierCommand(null, 2026, "s", "n", "1", null));
        assertThrows(IllegalArgumentException.class,
                () -> new CertifyAgronomicDossierCommand(plotId, null, "s", "n", "1", null));
        assertThrows(IllegalArgumentException.class,
                () -> new CertifyAgronomicDossierCommand(plotId, 2026, " ", "n", "1", null));
        assertThrows(IllegalArgumentException.class,
                () -> new CertifyAgronomicDossierCommand(plotId, 2026, "s", null, "1", null));
        assertThrows(IllegalArgumentException.class,
                () -> new CertifyAgronomicDossierCommand(plotId, 2026, "s", "n", "", null));
        assertDoesNotThrow(() -> new CertifyAgronomicDossierCommand(plotId, 2026, "s", "n", "1", null));
    }
}
