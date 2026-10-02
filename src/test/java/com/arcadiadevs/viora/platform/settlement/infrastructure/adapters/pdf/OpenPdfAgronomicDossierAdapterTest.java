package com.arcadiadevs.viora.platform.settlement.infrastructure.adapters.pdf;

import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.AgronomicDossierContent;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.services.CryptographicHashService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OpenPdfAgronomicDossierAdapterTest {
    private static final Instant CERTIFIED_AT = Instant.parse("2026-10-03T09:30:00Z");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC);
    private final OpenPdfAgronomicDossierAdapter adapter = new OpenPdfAgronomicDossierAdapter();
    private final PlotId plotId = new PlotId(UUID.randomUUID().toString());
    private final UserId producer = new UserId(UUID.randomUUID().toString());

    private static TreeMap<Integer, Double> alternatingHistory() {
        var history = new TreeMap<Integer, Double>();
        history.put(2022, 10000.0);
        history.put(2023, 2000.0);
        history.put(2024, 9000.0);
        history.put(2025, 3000.0);
        return history;
    }

    private static TreeMap<Integer, Double> flatHistory() {
        var history = new TreeMap<Integer, Double>();
        for (int year = 2022; year <= 2025; year++) {
            history.put(year, 5000.0);
        }
        return history;
    }

    /** Settles consecutive campaigns from 2026 and returns the content of the last one. */
    private AgronomicDossierContent content(TreeMap<Integer, Double> history, ThinningBalance balance,
            Double fruitsPerKg, String notes, double... kilograms) {
        var report = AgronomicReport.createForPlot(plotId, producer);
        HarvestSettlementSnapshot last = null;
        for (int i = 0; i < kilograms.length; i++) {
            last = report.settleCampaign(new CampaignYear(2026 + i), new OliveWeight(kilograms[i]),
                    new OliveWeight(0.0), fruitsPerKg, null, balance, history, CLOCK);
        }
        return new AgronomicDossierContent(report.snapshot().id(), plotId, producer, last.campaignYear(),
                last, new CertifierIdentity("Ing. Sánchez Núñez", "49120"),
                new AuditorSignature("CIP-49120-ING-AGRÓNOMO-SÁNCHEZ"), notes, CERTIFIED_AT);
    }

    private AgronomicDossierContent simple(String notes) {
        return content(new TreeMap<>(), ThinningBalance.notRecorded(), null, notes, 8200.0);
    }

    private static String text(byte[] pdf) throws IOException {
        try (var document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    @Test
    void rendersARealPdfThatParses() throws IOException {
        var bytes = adapter.renderPdf(simple("Verified"));
        assertEquals("%PDF-", new String(bytes, 0, 5, StandardCharsets.US_ASCII));
        try (var document = Loader.loadPDF(bytes)) {
            assertTrue(document.getNumberOfPages() >= 1);
        }
    }

    @Test
    void containsCampaignIdentitySignatureCipAndAccentedNotes() throws IOException {
        var content = simple("Verificación de campaña – Sánchez, ¿ñandú? ¡Güemes!");
        var text = text(adapter.renderPdf(content));
        assertTrue(text.contains("Campaign 2026"));
        assertTrue(text.contains(content.reportId().reportId()));
        assertTrue(text.contains(plotId.plotId()));
        assertTrue(text.contains(producer.userId()));
        assertTrue(text.contains("CIP-49120-ING-AGRÓNOMO-SÁNCHEZ"));
        assertTrue(text.contains("49120"));
        assertTrue(text.contains("Ing. Sánchez Núñez"));
        assertTrue(text.contains("Verificación de campaña – Sánchez, ¿ñandú? ¡Güemes!"));
        assertTrue(text.contains("2026-10-03T09:30:00Z"));
    }

    @Test
    void showsTheSettledWeightsAndStatus() throws IOException {
        var text = text(adapter.renderPdf(content(new TreeMap<>(), ThinningBalance.notRecorded(), 105.0, null,
                8200.0)));
        assertTrue(text.contains("Green olives (kg) 8200.00"));
        assertTrue(text.contains("Black olives (kg) 0.00"));
        assertTrue(text.contains("Total harvest (kg) 8200.00"));
        assertTrue(text.contains("Commercial caliber (fruits/kg) 105.00"));
        assertTrue(text.contains("Settlement status SETTLED"));
        assertTrue(text.contains("2026-10-02T12:00:00Z"));
        assertTrue(text.contains("Notes None"));
    }

    @Test
    void showsAbsentThinningAndCaliberAsNotRecorded() throws IOException {
        var text = text(adapter.renderPdf(simple(null)));
        assertTrue(text.contains("Compliance Not recorded"));
        assertTrue(text.contains("Commercial caliber (fruits/kg) Not recorded"));
        assertFalse(text.contains("Prescribed removal"));
    }

    @Test
    void showsTheRecordedThinningBalance() throws IOException {
        var record = new ThinningExecutionRecord("e", UUID.randomUUID().toString(), plotId, 2026,
                LocalDate.of(2026, 1, 10), 30.0, 25.0, true);
        var text = text(adapter.renderPdf(content(new TreeMap<>(), ThinningBalance.of(record), null, null, 8200.0)));
        assertTrue(text.contains("Compliance EXECUTED_ON_TIME"));
        assertTrue(text.contains("Executed on 2026-01-10"));
        assertTrue(text.contains("Prescribed removal (%) 30.00"));
        assertTrue(text.contains("Actual removal (%) 25.00"));
        assertTrue(text.contains("Deviation (percentage points) -5.00"));
    }

    @Test
    void printsTheReductionRateOnlyWhenTheCurveIsEvaluated() throws IOException {
        var evaluated = content(alternatingHistory(), ThinningBalance.notRecorded(), null, null, 7000, 5000, 6500);
        assertEquals(StabilizationStatus.EVALUATED, evaluated.settlement().trendCurve().status());
        var evaluatedText = text(adapter.renderPdf(evaluated));
        assertTrue(evaluatedText.contains("Amplitude reduction rate (ARR) 0.7528"));
        assertTrue(evaluatedText.contains("Stabilization target (ARR >= 30%) Achieved"));
        assertFalse(evaluatedText.contains("not determinable"));

        var missingBaseline = text(adapter.renderPdf(simple(null)));
        assertFalse(missingBaseline.contains("ARR"));
        assertTrue(missingBaseline.contains("not determinable"));
        assertTrue(missingBaseline.contains("baseline missing"));
    }

    @Test
    void explainsWhyStabilizationIsNotDeterminable() throws IOException {
        var flat = content(flatHistory(), ThinningBalance.notRecorded(), null, null, 7000, 5000, 6500);
        assertEquals(StabilizationStatus.NO_BASELINE_ALTERNATION, flat.settlement().trendCurve().status());
        var flatText = text(adapter.renderPdf(flat));
        assertFalse(flatText.contains("ARR"));
        assertTrue(flatText.contains("shows no alternation"));

        var insufficient = content(alternatingHistory(), ThinningBalance.notRecorded(), null, null, 7000);
        assertEquals(StabilizationStatus.INSUFFICIENT_SETTLEMENTS, insufficient.settlement().trendCurve().status());
        var insufficientText = text(adapter.renderPdf(insufficient));
        assertFalse(insufficientText.contains("ARR"));
        assertTrue(insufficientText.contains("fewer than two consecutive settled campaigns"));
    }

    @Test
    void neverPrintsTheVerificationHashOfTheFile() throws IOException {
        var bytes = adapter.renderPdf(simple("Verified"));
        var hash = new CryptographicHashService().sha256(bytes).value();
        var text = text(bytes);
        assertFalse(text.contains(hash));
        assertFalse(text.matches(".*\\b[0-9a-f]{64}\\b.*"));
        assertTrue(text.contains("delivered separately"));
    }

    @Test
    void usesTheCertificationInstantAsDocumentDatesAndIsStableAcrossRenders() throws IOException {
        var content = simple("Verified");
        var first = adapter.renderPdf(content);
        var second = adapter.renderPdf(content);
        try (var document = Loader.loadPDF(first)) {
            var information = document.getDocumentInformation();
            assertEquals(CERTIFIED_AT, information.getCreationDate().toInstant());
            assertEquals(CERTIFIED_AT, information.getModificationDate().toInstant());
        }
        // The library generates the trailer file identifier (/ID) from the clock; everything else is stable, which
        // is why the stored bytes (not a later re-render) are the certified artifact.
        assertEquals(withoutFileId(first), withoutFileId(second));
    }

    private static String withoutFileId(byte[] pdf) {
        return new String(pdf, StandardCharsets.ISO_8859_1).replaceAll("/ID\\s*\\[[^\\]]*\\]", "/ID[]");
    }

    @Test
    void replacesCharactersOutsideWinAnsiInsteadOfDroppingThem() throws IOException {
        var text = text(adapter.renderPdf(simple("Привет")));
        assertTrue(text.contains("??????"));
        assertEquals("a?b", OpenPdfAgronomicDossierAdapter.sanitize("a中b"));
    }

    @Test
    void wrapsRenderingFailuresInTheDomainException() {
        assertThrows(DossierRenderingException.class, () -> adapter.renderPdf(null));
    }
}
