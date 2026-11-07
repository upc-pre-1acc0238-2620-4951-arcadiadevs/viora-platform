package com.arcadiadevs.viora.platform.settlement.infrastructure.adapters.pdf;

import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.ProvisionalDossierContent;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Provisional dossier PDF")
class OpenPdfProvisionalDossierAdapterTest {

    private final OpenPdfProvisionalDossierAdapter adapter = new OpenPdfProvisionalDossierAdapter();

    @Test
    @DisplayName("Says it is provisional and not certified, with its cut-off, and carries no verification code")
    void provisionalWithCutOff() throws IOException {
        var pdf = adapter.render(new ProvisionalDossierContent("La Yarada 02", "SEVILLANA", 2.5, "Teodoro Mamani",
                "Cooperativa Olivar del Sur", new CampaignYear(2026), Instant.parse("2026-11-04T14:41:00Z"), 2.0, 1.4,
                42.9, 30.0, LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 30), 5, LocalDate.of(2026, 10, 29),
                null, 2, 182));

        var text = text(pdf);
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(text).contains("Expediente provisional", "PROVISIONAL", "NO CERTIFICADO",
                "Corte: 4 de noviembre de 2026, 09:41", "La Yarada 02", "Teodoro Mamani",
                "Cooperativa Olivar del Sur", "Sevillana", "2,00 frutos por brote", "+43 %",
                "Aclarear 30 % de los frutos", "Aún no", "182 desde el 1 de enero");
        assertThat(text).doesNotContainIgnoringCase("SHA").doesNotContain("Íntegro");
    }

    @Test
    @DisplayName("What was never recorded reads «Sin registro»")
    void missingData() throws IOException {
        var pdf = adapter.render(new ProvisionalDossierContent("Lote Norte", null, null, null, null,
                new CampaignYear(2026), Instant.parse("2026-11-04T14:41:00Z"), null, null, null, null, null, null, 0,
                null, null, 0, 0));

        assertThat(text(pdf)).contains("Sin registro");
    }

    @Test
    @DisplayName("No content, no document")
    void nullContent() {
        assertThatThrownBy(() -> adapter.render(null)).isInstanceOf(DossierRenderingException.class);
    }

    private static String text(byte[] pdf) throws IOException {
        var reader = new PdfReader(pdf);
        var extractor = new PdfTextExtractor(reader);
        var text = new StringBuilder();
        for (var page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        return text.toString();
    }
}
