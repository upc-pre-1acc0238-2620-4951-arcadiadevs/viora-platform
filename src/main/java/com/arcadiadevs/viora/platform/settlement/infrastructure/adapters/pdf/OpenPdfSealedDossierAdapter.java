package com.arcadiadevs.viora.platform.settlement.infrastructure.adapters.pdf;

import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.SealedDossierContent;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.SealedDossierPdfGenerator;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierFacts;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.StabilizationStatus;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.StabilizationTrendCurve;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfDate;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Renders, in Spanish, the dossier a producer seals for one settled campaign (US30), fully in memory.
 *
 * <p>Sections: identification, productive history, crop load and thinning, settled harvest, climate data and
 * stabilization. Missing data is written as such ("Sin registro"), never estimated. The SHA-256 is not printed: a
 * file cannot contain its own digest, so it is delivered and verified apart. Text uses Helvetica with WinAnsi
 * encoding, like the certification dossier of ADR-002.</p>
 */
@Component
public class OpenPdfSealedDossierAdapter implements SealedDossierPdfGenerator {

    private static final String CREATOR = "Viora";
    private static final String NOT_RECORDED = "Sin registro";
    private static final Color LABEL_BACKGROUND = new Color(0xEE, 0xF2, 0xEA);
    private static final Locale SPANISH = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' uuuu", SPANISH);
    private static final DateTimeFormatter INSTANT = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' uuuu, HH:mm 'UTC'",
            SPANISH).withZone(ZoneOffset.UTC);

    @Override
    public RenderedDossier render(SealedDossierContent content) {
        if (content == null) {
            throw new DossierRenderingException("settlement.dossier.content.invalid");
        }
        var output = new ByteArrayOutputStream();
        var document = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            var writer = PdfWriter.getInstance(document, output);
            stampDates(writer, content.sealedAt());
            document.addTitle("Expediente agronómico - " + content.facts().plotName() + " - campaña "
                    + content.campaignYear().value());
            document.addCreator(CREATOR);
            document.open();
            write(document, new Fonts(), content);
            document.close();
            byte[] bytes = output.toByteArray();
            int pages = new PdfReader(bytes).getNumberOfPages();
            return new RenderedDossier(bytes, pages);
        } catch (IOException | RuntimeException e) {
            throw new DossierRenderingException("settlement.dossier.render.failed", e);
        }
    }

    private static void stampDates(PdfWriter writer, Instant sealedAt) {
        var calendar = new GregorianCalendar(TimeZone.getTimeZone(ZoneOffset.UTC), Locale.ROOT);
        calendar.setTimeInMillis(sealedAt.toEpochMilli());
        var date = new PdfDate(calendar);
        writer.getInfo().put(PdfName.CREATIONDATE, date);
        writer.getInfo().put(PdfName.MODDATE, date);
    }

    private void write(Document document, Fonts fonts, SealedDossierContent content) throws DocumentException {
        var facts = content.facts();
        document.add(new Paragraph("Expediente agronómico", fonts.title));
        var subtitle = new Paragraph(sanitize(text(facts.plotName()) + " · Campaña " + content.campaignYear().value()),
                fonts.subtitle);
        subtitle.setSpacingAfter(12);
        document.add(subtitle);

        section(document, fonts, "1. Identificación");
        var identification = table();
        row(identification, fonts, "Lote", text(facts.plotName()));
        row(identification, fonts, "Variedad", variety(facts.variety()));
        row(identification, fonts, "Superficie", facts.areaHectares() == null ? NOT_RECORDED
                : decimal(facts.areaHectares(), 2) + " ha");
        if (facts.holderName() != null && !facts.holderName().isBlank()) {
            row(identification, fonts, "Titular", facts.holderName());
        } else {
            row(identification, fonts, "ID de productor", content.producerId().userId());
        }
        row(identification, fonts, "Campaña", String.valueOf(content.campaignYear().value()));
        row(identification, fonts, "Sellado", INSTANT.format(content.sealedAt()));
        document.add(identification);

        section(document, fonts, "2. Historial productivo");
        document.add(historyTable(fonts, content.harvestHistoryKg(), facts.areaHectares(),
                content.campaignYear().value()));

        section(document, fonts, "3. Carga frutal y aclareo");
        var thinning = table();
        row(thinning, fonts, "Carga antes de aclarear", load(facts.preThinningFruitsPerShoot()));
        row(thinning, fonts, "Carga objetivo sostenible", load(facts.targetFruitsPerShoot()));
        row(thinning, fonts, "Exceso sobre el objetivo", facts.overTargetPercentage() == null ? NOT_RECORDED
                : signedPercent(facts.overTargetPercentage()));
        row(thinning, fonts, "Prescripción", facts.prescribedRemovalPercentage() == null ? NOT_RECORDED
                : "Aclarear " + decimal(facts.prescribedRemovalPercentage(), 0) + " % de los frutos");
        row(thinning, fonts, "Ventana de aclareo", facts.windowOpensOn() == null || facts.windowClosesOn() == null
                ? NOT_RECORDED : date(facts.windowOpensOn()) + " al " + date(facts.windowClosesOn()));
        row(thinning, fonts, "Aclareo registrado", facts.actualRemovalPercentage() == null ? NOT_RECORDED
                : decimal(facts.actualRemovalPercentage(), 0) + " % · " + date(facts.thinnedOn()));
        row(thinning, fonts, "Oportunidad", switch (facts.thinningCompliance()) {
            case "EXECUTED_ON_TIME" -> "Dentro de la ventana";
            case "EXECUTED_LATE" -> "Fuera de la ventana (tardío)";
            default -> "Sin aclareo registrado";
        });
        document.add(thinning);

        section(document, fonts, "4. Cosecha asentada");
        var settlement = content.settlement();
        var harvest = table();
        row(harvest, fonts, "Aceituna verde", kilos(facts.greenKg()));
        row(harvest, fonts, "Aceituna negra", kilos(facts.blackKg()));
        row(harvest, fonts, "Total", kilos(facts.totalKg()));
        row(harvest, fonts, "Rendimiento", facts.kgPerHectare() == null ? NOT_RECORDED
                : decimal(facts.kgPerHectare() / 1000.0, 1) + " t/ha");
        row(harvest, fonts, "Calibre de venta", settlement.commercialFruitsPerKg() == null ? NOT_RECORDED
                : decimal(settlement.commercialFruitsPerKg(), 0) + " frutos por kg");
        row(harvest, fonts, "Comprobante", text(facts.receiptNumber()));
        row(harvest, fonts, "Boleta de molino", settlement.millTicketNumber() == null ? NOT_RECORDED
                : settlement.millTicketNumber().value());
        row(harvest, fonts, "Fecha de pesaje", facts.weighedOn() == null ? NOT_RECORDED : date(facts.weighedOn()));
        row(harvest, fonts, "Asentada", INSTANT.format(settlement.settledAt()));
        document.add(harvest);

        section(document, fonts, "5. Clima y telemetría");
        var climate = table();
        row(climate, fonts, "Nodos vinculados", String.valueOf(facts.linkedNodes()));
        row(climate, fonts, "Días con lecturas", facts.daysWithReadings() + " (del 1 de enero al asiento de la cosecha)");
        document.add(climate);

        section(document, fonts, "6. Estabilización de la vecería");
        document.add(curveTable(fonts, settlement.trendCurve()));
        var statement = new Paragraph(sanitize(stabilizationStatement(settlement.trendCurve())), fonts.body);
        statement.setSpacingBefore(6);
        document.add(statement);

        var footer = new Paragraph("Expediente sellado por el productor en Viora. El código de verificación SHA-256 "
                + "se calcula sobre los bytes de este archivo y se entrega por separado: un archivo no puede contener "
                + "su propio código. Si alguien cambia un dato, el código deja de coincidir con el registro de Viora. "
                + "Este expediente no lleva firma colegiada; la certificación de un agrónomo es un trámite aparte.",
                fonts.footer);
        footer.setSpacingBefore(18);
        document.add(footer);
    }

    private PdfPTable historyTable(Fonts fonts, Map<Integer, Double> history, Double areaHectares, int campaign) {
        var table = new PdfPTable(new float[]{1.2f, 2f, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);
        header(table, fonts, "Campaña");
        header(table, fonts, "Cosecha");
        header(table, fonts, "t/ha");
        if (history.isEmpty()) {
            var empty = new PdfPCell(new Phrase(NOT_RECORDED, fonts.body));
            empty.setColspan(3);
            empty.setPadding(4);
            table.addCell(empty);
            return table;
        }
        history.forEach((year, kg) -> {
            cell(table, fonts, year == campaign ? year + " (asentada)" : String.valueOf(year));
            cell(table, fonts, kilos(kg));
            cell(table, fonts, areaHectares == null || areaHectares <= 0.0 ? "—"
                    : decimal(kg / 1000.0 / areaHectares, 1));
        });
        return table;
    }

    private PdfPTable curveTable(Fonts fonts, StabilizationTrendCurve curve) {
        var table = table();
        row(table, fonts, "Campañas de referencia", String.valueOf(curve.baselineCampaigns()));
        row(table, fonts, "Campañas asentadas", String.valueOf(curve.settledCampaigns()));
        if (curve.baselineAlternationIndex() != null) {
            row(table, fonts, "Índice de vecería de referencia", decimal(curve.baselineAlternationIndex(), 2));
        }
        if (curve.managedAlternationIndex() != null) {
            row(table, fonts, "Índice de vecería con Viora", decimal(curve.managedAlternationIndex(), 2));
        }
        if (curve.status() == StabilizationStatus.EVALUATED) {
            row(table, fonts, "Reducción de la amplitud", decimal(curve.amplitudeReductionRate() * 100.0, 0) + " %");
            row(table, fonts, "Meta (reducir 30 % o más)",
                    Boolean.TRUE.equals(curve.targetAchieved()) ? "Cumplida" : "No cumplida");
        }
        return table;
    }

    private static String stabilizationStatement(StabilizationTrendCurve curve) {
        return switch (curve.status()) {
            case EVALUATED -> "La estabilización se evaluó contra el historial del lote antes de Viora.";
            case INSUFFICIENT_BASELINE -> "Aún no se puede medir la estabilización: faltan al menos dos campañas "
                    + "seguidas de historial antes de la primera cosecha asentada.";
            case NO_BASELINE_ALTERNATION -> "No hay vecería que reducir: el historial previo no alterna.";
            case INSUFFICIENT_SETTLEMENTS -> "Aún no se puede medir la estabilización: se necesitan tres campañas "
                    + "asentadas seguidas en Viora.";
        };
    }

    private static PdfPTable table() {
        var table = new PdfPTable(new float[]{2f, 3f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);
        // A short table never splits across pages
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
        cell(table, fonts, value);
    }

    private static void header(PdfPTable table, Fonts fonts, String text) {
        var cell = new PdfPCell(new Phrase(sanitize(text), fonts.label));
        cell.setBackgroundColor(LABEL_BACKGROUND);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private static void cell(PdfPTable table, Fonts fonts, String value) {
        var cell = new PdfPCell(new Phrase(sanitize(value), fonts.body));
        cell.setPadding(4);
        table.addCell(cell);
    }

    /** "SEVILLANA" reads "Sevillana", as the app names varieties. */
    private static String variety(String value) {
        if (value == null || value.isBlank()) {
            return NOT_RECORDED;
        }
        String lower = value.trim().toLowerCase(SPANISH);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? NOT_RECORDED : value;
    }

    private static String load(Double fruitsPerShoot) {
        return fruitsPerShoot == null ? NOT_RECORDED : decimal(fruitsPerShoot, 2) + " frutos por brote";
    }

    private static String kilos(Double kilograms) {
        return kilograms == null ? NOT_RECORDED : grouped(Math.round(kilograms)) + " kg";
    }

    private static String date(LocalDate date) {
        return date == null ? NOT_RECORDED : DATE.format(date);
    }

    private static String signedPercent(double value) {
        return (value > 0 ? "+" : "") + decimal(value, 0) + " %";
    }

    /**
     * Spanish decimal comma, as the app shows numbers. Floating point noise is dropped first (0,57 / 0,40 is
     * 1,4249999... in binary), so a half is always rounded up.
     */
    static String decimal(double value, int decimals) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).setScale(decimals, RoundingMode.HALF_UP)
                .toPlainString().replace('.', ',');
    }

    /** Thousands grouped with a space ("20 800"), as the app shows them. */
    static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }

    private static String sanitize(String text) {
        return OpenPdfAgronomicDossierAdapter.sanitize(text);
    }

    /** Standard Helvetica fonts, not embedded, WinAnsi encoded. */
    private static final class Fonts {
        final Font title;
        final Font subtitle;
        final Font section;
        final Font label;
        final Font body;
        final Font footer;

        Fonts() throws DocumentException, IOException {
            var regular = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            var bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            title = new Font(bold, 20);
            subtitle = new Font(regular, 14);
            section = new Font(bold, 12);
            label = new Font(bold, 10);
            body = new Font(regular, 10);
            footer = new Font(regular, 8);
        }
    }
}
