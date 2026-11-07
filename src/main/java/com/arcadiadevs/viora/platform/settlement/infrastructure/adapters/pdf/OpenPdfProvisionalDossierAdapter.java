package com.arcadiadevs.viora.platform.settlement.infrastructure.adapters.pdf;

import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.ProvisionalDossierContent;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.ProvisionalDossierPdfGenerator;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * OpenPDF adapter of the provisional dossier. Every page says it is provisional and when the data was read; there is
 * no verification code, because the same request on another day may give another document.
 */
@Component
public class OpenPdfProvisionalDossierAdapter implements ProvisionalDossierPdfGenerator {

    private static final String CREATOR = "Viora";
    private static final String NOT_RECORDED = "Sin registro";
    private static final Color LABEL_BACKGROUND = new Color(0xEE, 0xF2, 0xEA);
    private static final Color NOTICE = new Color(0x9A, 0x3B, 0x16);
    private static final Locale SPANISH = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' uuuu", SPANISH);
    private static final DateTimeFormatter CUT_OFF = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' uuuu, HH:mm",
            SPANISH).withZone(ProducerTimeZone.ZONE);

    @Override
    public byte[] render(ProvisionalDossierContent content) {
        if (content == null) {
            throw new DossierRenderingException("settlement.provisional_dossier.content.invalid");
        }
        var output = new ByteArrayOutputStream();
        var document = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(document, output);
            document.addTitle("Expediente provisional - " + text(content.plotName()) + " - campaña "
                    + content.campaignYear().value());
            document.addCreator(CREATOR);
            document.open();
            write(document, new Fonts(), content);
            document.close();
            return output.toByteArray();
        } catch (IOException | RuntimeException e) {
            throw new DossierRenderingException("settlement.dossier.render.failed", e);
        }
    }

    private void write(Document document, Fonts fonts, ProvisionalDossierContent content) throws DocumentException {
        document.add(new Paragraph("Expediente provisional", fonts.title));
        var subtitle = new Paragraph(sanitize(text(content.plotName()) + " · Campaña "
                + content.campaignYear().value() + " en curso"), fonts.subtitle);
        subtitle.setSpacingAfter(6);
        document.add(subtitle);
        var notice = new Paragraph(sanitize("PROVISIONAL · NO CERTIFICADO · Corte: "
                + CUT_OFF.format(content.cutOffAt())), fonts.notice);
        notice.setSpacingAfter(10);
        document.add(notice);

        section(document, fonts, "1. Identificación");
        var identification = table();
        row(identification, fonts, "Lote", text(content.plotName()));
        row(identification, fonts, "Variedad", variety(content.variety()));
        row(identification, fonts, "Superficie", content.areaHectares() == null ? NOT_RECORDED
                : decimal(content.areaHectares(), 2) + " ha");
        row(identification, fonts, "Productor", text(content.holderName()));
        row(identification, fonts, "Organización", text(content.organizationName()));
        row(identification, fonts, "Campaña", content.campaignYear().value() + " · en curso");
        document.add(identification);

        section(document, fonts, "2. Muestreo y carga frutal");
        var load = table();
        row(load, fonts, "Árboles muestreados", content.sampledTrees() == 0 ? NOT_RECORDED
                : content.sampledTrees() + " · " + date(content.lastSampledOn()));
        row(load, fonts, "Carga estimada", fruits(content.preThinningFruitsPerShoot()));
        row(load, fonts, "Carga objetivo sostenible", fruits(content.targetFruitsPerShoot()));
        row(load, fonts, "Exceso sobre el objetivo", content.overTargetPercentage() == null ? NOT_RECORDED
                : (content.overTargetPercentage() > 0 ? "+" : "") + decimal(content.overTargetPercentage(), 0) + " %");
        document.add(load);

        section(document, fonts, "3. Prescripción y aclareo");
        var thinning = table();
        row(thinning, fonts, "Prescripción", content.prescribedRemovalPercentage() == null ? NOT_RECORDED
                : "Aclarear " + decimal(content.prescribedRemovalPercentage(), 0) + " % de los frutos");
        row(thinning, fonts, "Ventana de aclareo", content.windowOpensOn() == null || content.windowClosesOn() == null
                ? NOT_RECORDED : date(content.windowOpensOn()) + " al " + date(content.windowClosesOn()));
        row(thinning, fonts, "Aclareo registrado", content.thinnedOn() == null ? "Aún no"
                : date(content.thinnedOn()));
        document.add(thinning);

        section(document, fonts, "4. Clima y telemetría");
        var climate = table();
        row(climate, fonts, "Nodos vinculados", String.valueOf(content.linkedNodes()));
        row(climate, fonts, "Días con lecturas", content.daysWithReadings() + " desde el 1 de enero");
        document.add(climate);

        var footer = new Paragraph(sanitize("Refleja los datos de esta fecha y puede cambiar. No es un documento "
                + "certificado y no tiene código de verificación. El expediente final lo sella el productor al "
                + "cerrar la campaña."), fonts.footer);
        footer.setSpacingBefore(14);
        document.add(footer);
    }

    private static PdfPTable table() {
        var table = new PdfPTable(new float[]{2f, 3f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);
        table.setKeepTogether(true);
        return table;
    }

    private static void section(Document document, Fonts fonts, String title) throws DocumentException {
        var paragraph = new Paragraph(sanitize(title), fonts.section);
        paragraph.setSpacingBefore(10);
        paragraph.setSpacingAfter(4);
        paragraph.setAlignment(Element.ALIGN_LEFT);
        document.add(paragraph);
    }

    private static void row(PdfPTable table, Fonts fonts, String label, String value) {
        var labelCell = new PdfPCell(new Phrase(sanitize(label), fonts.label));
        labelCell.setBackgroundColor(LABEL_BACKGROUND);
        labelCell.setPadding(4);
        table.addCell(labelCell);
        var valueCell = new PdfPCell(new Phrase(sanitize(value), fonts.body));
        valueCell.setPadding(4);
        table.addCell(valueCell);
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? NOT_RECORDED : value;
    }

    private static String variety(String value) {
        if (value == null || value.isBlank()) {
            return NOT_RECORDED;
        }
        var lower = value.trim().toLowerCase(SPANISH);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String fruits(Double fruitsPerShoot) {
        return fruitsPerShoot == null ? NOT_RECORDED : decimal(fruitsPerShoot, 2) + " frutos por brote";
    }

    private static String date(LocalDate value) {
        return value == null ? NOT_RECORDED : DATE.format(value);
    }

    private static String decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static String sanitize(String value) {
        return OpenPdfAgronomicDossierAdapter.sanitize(value);
    }

    private static final class Fonts {
        final Font title;
        final Font subtitle;
        final Font notice;
        final Font section;
        final Font label;
        final Font body;
        final Font footer;

        Fonts() throws DocumentException, IOException {
            var regular = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            var bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            title = new Font(bold, 20);
            subtitle = new Font(regular, 14);
            notice = new Font(bold, 11, Font.NORMAL, NOTICE);
            section = new Font(bold, 12);
            label = new Font(bold, 10);
            body = new Font(regular, 10);
            footer = new Font(regular, 8);
        }
    }
}
