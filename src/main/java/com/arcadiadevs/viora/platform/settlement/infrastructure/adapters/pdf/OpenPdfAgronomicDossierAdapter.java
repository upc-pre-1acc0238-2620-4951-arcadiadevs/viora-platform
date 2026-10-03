package com.arcadiadevs.viora.platform.settlement.infrastructure.adapters.pdf;

import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.AgronomicDossierContent;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.AgronomicDossierPdfGenerator;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.StabilizationStatus;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.StabilizationTrendCurve;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ThinningBalance;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ThinningComplianceStatus;
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
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Renders the certified agronomic dossier of one campaign with OpenPDF, fully in memory.
 *
 * <p>The section order is fixed: identification, harvest settlement, thinning compliance, stabilization curve,
 * certification. The document creation and modification dates are the certification instant, never the system
 * clock. The SHA-256 of the file is not printed inside it (a document cannot contain its own digest); it is
 * computed over the final bytes and delivered separately.</p>
 *
 * <p>Text uses the standard Helvetica font with WinAnsi (Windows-1252) encoding, which covers Spanish and other
 * Western European text (accents, {@code ñ}, {@code ü}, inverted punctuation, dashes). Characters outside that
 * repertoire (for example Cyrillic, Greek or CJK) are not representable and are replaced by {@code ?}.</p>
 */
@Component
public class OpenPdfAgronomicDossierAdapter implements AgronomicDossierPdfGenerator {

    private static final String PRODUCER = "Viora Platform";
    private static final String NOT_RECORDED = "Not recorded";
    private static final Color LABEL_BACKGROUND = new Color(0xEE, 0xF2, 0xEA);
    private static final Charset WIN_ANSI = Charset.forName("windows-1252");
    /** Instants are printed in UTC with a fixed six-digit fraction, so equal moments always read the same. */
    private static final DateTimeFormatter INSTANT_TEXT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS'Z'").withZone(ZoneOffset.UTC);

    /**
     * {@inheritDoc}
     *
     * @throws DossierRenderingException wrapping any OpenPDF failure
     */
    @Override
    public byte[] renderPdf(AgronomicDossierContent content) {
        if (content == null) {
            throw new DossierRenderingException("settlement.certification.content.invalid");
        }
        var output = new ByteArrayOutputStream();
        var document = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            var writer = PdfWriter.getInstance(document, output);
            writer.setCompressionLevel(0);
            stampDates(writer, content.certifiedAt());
            document.addTitle("Certified agronomic dossier - campaign " + content.campaignYear().value());
            document.addAuthor(sanitize(content.certifier().name()));
            document.addCreator(PRODUCER);
            document.open();
            var fonts = new Fonts();
            write(document, fonts, content);
            document.close();
        } catch (IOException | RuntimeException e) {
            throw new DossierRenderingException("settlement.certification.render.failed", e);
        }
        return output.toByteArray();
    }

    private static void stampDates(PdfWriter writer, Instant certifiedAt) {
        var calendar = new GregorianCalendar(TimeZone.getTimeZone(ZoneOffset.UTC), Locale.ROOT);
        calendar.setTimeInMillis(certifiedAt.toEpochMilli());
        var date = new PdfDate(calendar);
        writer.getInfo().put(PdfName.CREATIONDATE, date);
        writer.getInfo().put(PdfName.MODDATE, date);
    }

    private void write(Document document, Fonts fonts, AgronomicDossierContent content) throws DocumentException {
        var settlement = content.settlement();
        document.add(new Paragraph("Certified Agronomic Dossier", fonts.title));
        var subtitle = new Paragraph("Campaign " + content.campaignYear().value(), fonts.subtitle);
        subtitle.setSpacingAfter(12);
        document.add(subtitle);

        section(document, fonts, "1. Identification");
        var identification = table();
        row(identification, fonts, "Report ID", content.reportId().reportId());
        row(identification, fonts, "Plot ID", content.plotId().plotId());
        row(identification, fonts, "Producer ID", content.producerId().userId());
        row(identification, fonts, "Campaign", String.valueOf(content.campaignYear().value()));
        document.add(identification);

        section(document, fonts, "2. Harvest settlement");
        document.add(settlementTable(fonts, settlement));

        section(document, fonts, "3. Thinning compliance");
        document.add(thinningTable(fonts, settlement.thinningBalance()));

        section(document, fonts, "4. Stabilization curve");
        document.add(curveTable(fonts, settlement.trendCurve()));
        var statement = new Paragraph(sanitize(stabilizationStatement(settlement.trendCurve())), fonts.body);
        statement.setSpacingBefore(6);
        document.add(statement);

        section(document, fonts, "5. Certification");
        var certification = table();
        row(certification, fonts, "Certified by", content.certifier().name());
        row(certification, fonts, "CIP number", content.certifier().cipNumber());
        row(certification, fonts, "Auditor signature", content.signature().value());
        row(certification, fonts, "Notes", content.notes() == null || content.notes().isBlank()
                ? "None" : content.notes());
        row(certification, fonts, "Certified at (UTC)", INSTANT_TEXT.format(content.certifiedAt()));
        document.add(certification);

        var footer = new Paragraph("The SHA-256 verification hash is computed over the bytes of this file and is "
                + "delivered separately; it is not printed here. The signature and CIP number are declared by the "
                + "certifying professional and are not a digital signature. This dossier contains settlement, "
                + "thinning and stabilization data only; it does not include climate or chill measurements.",
                fonts.footer);
        footer.setSpacingBefore(18);
        document.add(footer);
    }

    private PdfPTable settlementTable(Fonts fonts, HarvestSettlementSnapshot settlement) {
        var table = table();
        row(table, fonts, "Green olives (kg)", number(settlement.greenOlivesWeight().kilograms(), 2));
        row(table, fonts, "Black olives (kg)", number(settlement.blackOlivesWeight().kilograms(), 2));
        row(table, fonts, "Total harvest (kg)", number(settlement.totalHarvestWeight().kilograms(), 2));
        row(table, fonts, "Commercial caliber (fruits/kg)", settlement.commercialFruitsPerKg() == null
                ? NOT_RECORDED : number(settlement.commercialFruitsPerKg(), 2));
        row(table, fonts, "Settlement status", settlement.status().name());
        row(table, fonts, "Settled at (UTC)", INSTANT_TEXT.format(settlement.settledAt()));
        return table;
    }

    private PdfPTable thinningTable(Fonts fonts, ThinningBalance balance) {
        var table = table();
        if (balance.status() == ThinningComplianceStatus.NOT_RECORDED) {
            row(table, fonts, "Compliance", NOT_RECORDED);
            return table;
        }
        row(table, fonts, "Compliance", balance.status().name());
        row(table, fonts, "Executed on", balance.executedDate() == null
                ? NOT_RECORDED : balance.executedDate().toString());
        row(table, fonts, "Prescribed removal (%)", orNotRecorded(balance.prescribedRemovalPercentage(), 2));
        row(table, fonts, "Actual removal (%)", orNotRecorded(balance.actualRemovalPercentage(), 2));
        row(table, fonts, "Deviation (percentage points)", orNotRecorded(balance.deviationPercentagePoints(), 2));
        return table;
    }

    private PdfPTable curveTable(Fonts fonts, StabilizationTrendCurve curve) {
        var table = table();
        row(table, fonts, "Curve status", curve.status().name());
        row(table, fonts, "Baseline campaigns", String.valueOf(curve.baselineCampaigns()));
        row(table, fonts, "Settled campaigns", String.valueOf(curve.settledCampaigns()));
        if (curve.baselineAlternationIndex() != null) {
            row(table, fonts, "Baseline alternation index", number(curve.baselineAlternationIndex(), 4));
        }
        if (curve.managedAlternationIndex() != null) {
            row(table, fonts, "Managed alternation index", number(curve.managedAlternationIndex(), 4));
        }
        if (curve.status() == StabilizationStatus.EVALUATED) {
            row(table, fonts, "Amplitude reduction rate (ARR)", number(curve.amplitudeReductionRate(), 4));
            row(table, fonts, "Stabilization target (ARR >= 30%)",
                    Boolean.TRUE.equals(curve.targetAchieved()) ? "Achieved" : "Not achieved");
        }
        return table;
    }

    private static String stabilizationStatement(StabilizationTrendCurve curve) {
        return switch (curve.status()) {
            case EVALUATED -> "The stabilization was evaluated against the baseline of the plot.";
            case INSUFFICIENT_BASELINE -> "Stabilization is not determinable: the plot has fewer than two "
                    + "consecutive campaigns of baseline history before the first settlement (baseline missing).";
            case NO_BASELINE_ALTERNATION -> "Stabilization is not determinable: the baseline history shows no "
                    + "alternation, so there is no amplitude to reduce.";
            // Unreachable for certified dossiers (certification requires three consecutive settled campaigns),
            // kept so the switch stays exhaustive over every frozen curve status.
            case INSUFFICIENT_SETTLEMENTS -> "Stabilization is not determinable: fewer than three consecutive "
                    + "settled campaigns exist.";
        };
    }

    private static PdfPTable table() {
        var table = new PdfPTable(new float[]{2f, 3f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);
        return table;
    }

    private static void section(Document document, Fonts fonts, String title) throws DocumentException {
        var paragraph = new Paragraph(title, fonts.section);
        paragraph.setSpacingBefore(10);
        paragraph.setSpacingAfter(4);
        paragraph.setAlignment(Element.ALIGN_LEFT);
        document.add(paragraph);
    }

    private static void row(PdfPTable table, Fonts fonts, String label, String value) {
        var labelCell = new PdfPCell(new Phrase(sanitize(label), fonts.label));
        labelCell.setBackgroundColor(LABEL_BACKGROUND);
        labelCell.setPadding(4);
        var valueCell = new PdfPCell(new Phrase(sanitize(value), fonts.body));
        valueCell.setPadding(4);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private static String orNotRecorded(Double value, int decimals) {
        return value == null ? NOT_RECORDED : number(value, decimals);
    }

    private static String number(Double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    /** Replaces the characters that Windows-1252 cannot represent, so none is silently dropped. */
    static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        CharsetEncoder encoder = WIN_ANSI.newEncoder();
        var result = new StringBuilder(text.length());
        text.codePoints().forEach(codePoint -> {
            var chars = Character.toChars(codePoint);
            if (Character.isISOControl(codePoint) && codePoint != '\n') {
                result.append(' ');
            } else if (encoder.canEncode(new String(chars))) {
                result.append(chars);
            } else {
                result.append('?');
            }
        });
        return result.toString();
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
