package com.arcadiadevs.viora.platform.settlement.infrastructure.adapters.pdf;

import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.SealedDossier;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.SealedDossierContent;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierFacts;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.OliveWeight;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ReceiptNumber;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ThinningBalance;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ThinningComplianceStatus;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.WeighingDate;
import com.arcadiadevs.viora.platform.settlement.domain.services.CryptographicHashService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Sealed dossier PDF (US30)")
class OpenPdfSealedDossierAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-02T12:00:00Z"), ZoneOffset.UTC);
    private static final Instant SEALED_AT = Instant.parse("2026-06-03T15:40:00Z");
    private final OpenPdfSealedDossierAdapter adapter = new OpenPdfSealedDossierAdapter();
    private final PlotId plotId = new PlotId(UUID.randomUUID().toString());
    private final UserId producer = new UserId(UUID.randomUUID().toString());

    private SealedDossierContent content(DossierFacts facts, ThinningBalance balance) {
        var history = new TreeMap<Integer, Double>();
        history.put(2024, 24000.0);
        history.put(2025, 7750.0);
        var report = AgronomicReport.createForPlot(plotId, producer);
        var settlement = report.settleCampaign(new CampaignYear(2026), new OliveWeight(12500.0), new OliveWeight(8300.0),
                101.0, null, balance, history, ReceiptNumber.of(new CampaignYear(2026), 142),
                WeighingDate.of(LocalDate.of(2026, 5, 14), CLOCK), null, null, CLOCK);
        history.put(2026, 20800.0);
        return new SealedDossierContent(plotId, producer, new CampaignYear(2026), facts, settlement, history, SEALED_AT);
    }

    private static DossierFacts fullFacts() {
        return new DossierFacts("La Yarada 02", "SEVILLANA", 2.5, 0.57, 0.40, 30.0, LocalDate.of(2026, 11, 3),
                LocalDate.of(2026, 11, 30), 30.0, LocalDate.of(2026, 11, 14), "EXECUTED_ON_TIME", 20800.0, 12500.0,
                8300.0, "VR-26-0142", LocalDate.of(2026, 5, 14), 2, 182, 2024, 2026, 3, "Teodoro Mamani");
    }

    private static String text(byte[] pdf) throws IOException {
        try (var document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    @Test
    @DisplayName("States every section in Spanish with the units the producer reads")
    void writesEverySectionInSpanish() throws IOException {
        var balance = new ThinningBalance(ThinningComplianceStatus.EXECUTED_ON_TIME, LocalDate.of(2026, 11, 14), 30.0,
                30.0, 0.0);

        var rendered = adapter.render(content(fullFacts(), balance));
        var text = text(rendered.content());

        assertTrue(rendered.pageCount() >= 1);
        assertTrue(text.contains("Expediente agronómico"), text);
        assertTrue(text.contains("La Yarada 02 · Campaña 2026"), text);
        assertTrue(text.contains("Titular Teodoro Mamani"), text);
        assertTrue(text.contains("2,50 ha"), text);
        assertTrue(text.contains("0,57 frutos por brote"), text);
        assertTrue(text.contains("+43 %"), text);
        assertTrue(text.contains("Aclarear 30 % de los frutos"), text);
        assertTrue(text.contains("3 de noviembre de 2026 al 30 de noviembre de 2026"), text);
        assertTrue(text.contains("Dentro de la ventana"), text);
        assertTrue(text.contains("20 800 kg"), text);
        assertTrue(text.contains("8,3 t/ha"), text);
        assertTrue(text.contains("101 frutos por kg"), text);
        assertTrue(text.contains("VR-26-0142"), text);
        assertTrue(text.contains("2026 (asentada)"), text);
        assertTrue(text.contains("182 (del 1 de enero al asiento de la cosecha)"), text);
        assertTrue(text.contains("no lleva firma colegiada"), text);
    }

    @Test
    @DisplayName("Writes what was never recorded as such instead of estimating it")
    void writesMissingDataAsMissing() throws IOException {
        var facts = new DossierFacts("Lote Norte", null, null, null, null, null, null, null, null, null,
                "NOT_RECORDED", 20800.0, 12500.0, 8300.0, null, null, 0, 0, 2024, 2026, 3, null);

        var text = text(adapter.render(content(facts, ThinningBalance.notRecorded())).content());

        assertTrue(text.contains("Carga antes de aclarear Sin registro"), text);
        assertTrue(text.contains("Sin aclareo registrado"), text);
        assertTrue(text.contains("Superficie Sin registro"), text);
        assertTrue(text.contains("Rendimiento Sin registro"), text);
    }

    @Test
    @DisplayName("The seal hashes the exact bytes it stores")
    void sealHashesTheStoredBytes() {
        var hashService = new CryptographicHashService();

        var sealed = SealedDossier.seal(content(fullFacts(), ThinningBalance.notRecorded()), adapter, hashService);

        var snapshot = sealed.dossier().snapshot();
        assertEquals(hashService.sha256(sealed.document().content()), snapshot.verificationHash());
        assertEquals(sealed.document().size(), snapshot.sizeBytes());
        assertEquals(SEALED_AT, snapshot.sealedAt());
        assertEquals(42.5, snapshot.facts().overTargetPercentage(), 1e-9);
    }

    @Test
    @DisplayName("Rejects a missing content")
    void rejectsMissingContent() {
        assertThrows(DossierRenderingException.class, () -> adapter.render(null));
    }
}
