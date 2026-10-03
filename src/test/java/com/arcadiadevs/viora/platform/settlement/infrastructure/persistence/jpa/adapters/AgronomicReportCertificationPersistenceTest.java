package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.CertifiedDossier;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.CertifiedDossierDocumentRepository;
import com.arcadiadevs.viora.platform.settlement.domain.services.CryptographicHashService;
import com.arcadiadevs.viora.platform.settlement.infrastructure.adapters.pdf.OpenPdfAgronomicDossierAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real JPA mapping on H2: certified PDF bytes (stored apart from the report), hash and metadata must survive a
 * reload unchanged, and loading a report must not need any document.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:certificationpersistence;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AgronomicReportCertificationPersistenceTest {
    private static final Clock SETTLEMENT_CLOCK = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC);
    private static final Clock FIRST_CLOCK = Clock.fixed(Instant.parse("2026-10-03T09:30:00.123456Z"), ZoneOffset.UTC);
    private static final Clock SECOND_CLOCK = Clock.fixed(Instant.parse("2027-10-04T10:00:00Z"), ZoneOffset.UTC);
    @Autowired AgronomicReportRepository repository;
    @Autowired CertifiedDossierDocumentRepository documents;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    private final CryptographicHashService hashService = new CryptographicHashService();
    private final OpenPdfAgronomicDossierAdapter pdf = new OpenPdfAgronomicDossierAdapter();
    private final PlotId plotId = new PlotId(UUID.randomUUID().toString());
    private final UserId producer = new UserId(UUID.randomUUID().toString());
    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        transactions = new TransactionTemplate(transactionManager);
    }

    private void settle(AgronomicReport report, int year) {
        report.settleCampaign(new CampaignYear(year), new OliveWeight(100.0 * year), new OliveWeight(50.0), null,
                null, ThinningBalance.notRecorded(), new TreeMap<>(), SETTLEMENT_CLOCK);
    }

    private void settleThreeConsecutive(AgronomicReport report) {
        settle(report, 2026);
        settle(report, 2027);
        settle(report, 2028);
    }

    private CertifiedDossier certify(AgronomicReport report, int year, Clock clock) {
        return report.certifyCampaign(new CampaignYear(year), new AuditorSignature("CIP-49120-SÁNCHEZ"),
                new CertifierIdentity("Ing. Sánchez", "49120"), new CertificationNotes("Verificación de campaña – Sánchez"), pdf, hashService, clock);
    }

    /** Saves the report and the document of a certification the way the command service does. */
    private void persist(AgronomicReport report, CertifiedDossier... certified) {
        transactions.executeWithoutResult(tx -> {
            repository.save(report);
            for (var item : certified) {
                documents.save(item.certification().id(), item.document());
            }
        });
    }

    private byte[] storedBytes(CertifiedDossier certified) {
        return documents.findByCertificationId(certified.certification().id()).orElseThrow().content();
    }

    @Test
    void certifiedDossiersSurviveAReloadWithTheirExactBytesAndHash() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settleThreeConsecutive(report);
        var certified = certify(report, 2028, FIRST_CLOCK);
        var expected = certified.certification();
        persist(report, certified);

        var reloaded = transactions.execute(tx -> repository.findByPlotId(plotId).orElseThrow());

        var stored = reloaded.certificationOf(new CampaignYear(2028)).orElseThrow();
        assertEquals(expected.id(), stored.id());
        assertEquals(report.snapshot().id(), stored.reportId());
        assertEquals(plotId, stored.plotId());
        assertEquals(new CampaignYear(2028), stored.campaignYear());
        assertEquals("CIP-49120-SÁNCHEZ", stored.metadata().auditorSignature().value());
        assertEquals(Instant.parse("2026-10-03T09:30:00.123456Z"), stored.metadata().certifiedAt());
        assertEquals("Ing. Sánchez", stored.certifier().name());
        assertEquals("49120", stored.certifier().cipNumber());
        assertEquals("Verificación de campaña – Sánchez", stored.notes());
        assertEquals(expected, stored);
        assertArrayEquals(certified.document().content(), storedBytes(certified));
        assertEquals(stored.metadata().verificationHash(), hashService.sha256(storedBytes(certified)));
        assertEquals(1, reloaded.snapshot().certifications().size());
        assertEquals(3, reloaded.snapshot().settlements().size());
    }

    @Test
    void appendingASecondCertificationKeepsTheFirstOneByteIdentical() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settleThreeConsecutive(report);
        var first = certify(report, 2028, FIRST_CLOCK);
        persist(report, first);
        var firstBefore = transactions.execute(tx -> repository.findByPlotId(plotId).orElseThrow())
                .certificationOf(new CampaignYear(2028)).orElseThrow();
        var firstBytesBefore = storedBytes(first);

        transactions.executeWithoutResult(tx -> {
            var loaded = repository.findByPlotIdForUpdate(plotId).orElseThrow();
            settle(loaded, 2029);
            var secondCertified = certify(loaded, 2029, SECOND_CLOCK);
            repository.save(loaded);
            documents.save(secondCertified.certification().id(), secondCertified.document());
        });

        var reloaded = transactions.execute(tx -> repository.findByPlotId(plotId).orElseThrow());
        assertEquals(2, reloaded.snapshot().certifications().size());
        var firstAfter = reloaded.certificationOf(new CampaignYear(2028)).orElseThrow();
        assertEquals(firstBefore, firstAfter);
        assertArrayEquals(firstBytesBefore, storedBytes(first));
        assertEquals(firstBefore.metadata().verificationHash(), hashService.sha256(storedBytes(first)));
        assertEquals(firstBefore.metadata(), firstAfter.metadata());
        var second = reloaded.certificationOf(new CampaignYear(2029)).orElseThrow();
        assertEquals(second.metadata().verificationHash(),
                hashService.sha256(documents.findByCertificationId(second.id()).orElseThrow().content()));
        assertNotEquals(firstAfter.metadata().verificationHash(), second.metadata().verificationHash());
    }

    @Test
    void storesTheDocumentInItsOwnBinaryTableWithAUniqueReportCampaignConstraint() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settleThreeConsecutive(report);
        var certified = certify(report, 2028, FIRST_CLOCK);
        persist(report, certified);

        byte[] stored = jdbc.queryForObject("select content from dossier_documents where certification_id = ?",
                byte[].class, UUID.fromString(certified.certification().id().certificationId()));
        assertArrayEquals(certified.document().content(), stored);
        assertEquals(certified.certification().metadata().verificationHash(), hashService.sha256(stored));
        assertEquals(0, jdbc.queryForObject("select count(*) from information_schema.columns "
                + "where lower(table_name) = 'dossier_certifications' "
                + "and lower(column_name) = 'document_content'", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from information_schema.table_constraints "
                + "where lower(constraint_name) = 'uq_certification_report_campaign'", Integer.class));
    }

    @Test
    void loadingAReportCarriesCertificationMetadataOnlyAndNeverNeedsTheDocuments() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settleThreeConsecutive(report);
        var certified = certify(report, 2028, FIRST_CLOCK);
        persist(report, certified);
        // Even with the document row gone, the report loads: its state has no bytes to load.
        jdbc.update("delete from dossier_documents where certification_id = ?",
                UUID.fromString(certified.certification().id().certificationId()));

        var reloaded = transactions.execute(tx -> repository.findByPlotId(plotId).orElseThrow());

        assertEquals(certified.certification(), reloaded.certificationOf(new CampaignYear(2028)).orElseThrow());
        assertFalse(java.util.Arrays.stream(
                        reloaded.snapshot().certifications().getFirst().getClass().getRecordComponents())
                .anyMatch(component -> component.getType().equals(DossierDocument.class)));
        assertTrue(documents.findByCertificationId(certified.certification().id()).isEmpty());
    }

    @Test
    void aStoredDocumentCannotBeReplaced() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        settleThreeConsecutive(report);
        var certified = certify(report, 2028, FIRST_CLOCK);
        persist(report, certified);
        var other = new DossierDocument("%PDF-1.7 other".getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        // The primary key on certification_id rejects a second document; the stored bytes stay untouched.
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> documents.save(certified.certification().id(), other));
        assertArrayEquals(certified.document().content(), storedBytes(certified));
    }
}
