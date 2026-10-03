package com.arcadiadevs.viora.platform.settlement.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.settlement.domain.model.events.AgronomicDossierGeneratedEvent;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.CertifiedDossierDocumentRepository;
import com.arcadiadevs.viora.platform.settlement.domain.services.CryptographicHashService;
import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real controllers, services, PDF adapter and H2 persistence: certification of settled campaigns end to end. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dossiercertification;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(AgronomicReportCertificationControllerIntegrationTest.EventCollectorConfiguration.class)
class AgronomicReportCertificationControllerIntegrationTest {

    /** Records published certification events, as a real consumer would receive them. */
    static class EventCollector {
        final List<AgronomicDossierGeneratedEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        public void on(AgronomicDossierGeneratedEvent event) {
            events.add(event);
        }
    }

    @TestConfiguration
    static class EventCollectorConfiguration {
        @Bean
        EventCollector eventCollector() {
            return new EventCollector();
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired AgronomicReportRepository reportRepository;
    @Autowired CertifiedDossierDocumentRepository documentRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired EventCollector collector;
    private final CryptographicHashService hashService = new CryptographicHashService();
    private MockMvc mvc;
    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        transactions = new TransactionTemplate(transactionManager);
        collector.events.clear();
    }

    @Test
    void certifiesASettledCampaignWithTheRealHashOfTheStoredPdf() throws Exception {
        var plotId = createPlot("CRIOLLA");
        settleThreeConsecutive(plotId);
        var before = Instant.now();

        var response = certify(plotId, 2028, "CIP-49120-ING-AGRONOMO-SANCHEZ", "Agronomist Sanchez", "49120",
                "Verificación de campaña – Sánchez")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.certificationId").isNotEmpty())
                .andExpect(jsonPath("$.reportId").isNotEmpty())
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.campaignYear").value(2028))
                .andExpect(jsonPath("$.auditorSignature").value("CIP-49120-ING-AGRONOMO-SANCHEZ"))
                .andExpect(jsonPath("$.certifiedBy").value("Agronomist Sanchez"))
                .andExpect(jsonPath("$.cipNumber").value("49120"))
                .andExpect(jsonPath("$.certifiedAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String hash = JsonPath.read(response, "$.verificationHash");
        assertTrue(hash.matches("[0-9a-f]{64}"));
        var stored = stored(plotId, 2028);
        assertEquals(hash, hashService.sha256(storedBytes(stored)).value());
        assertEquals(hash, stored.metadata().verificationHash().value());
        assertEquals(JsonPath.read(response, "$.certificationId"), stored.id().certificationId());
        assertEquals(Instant.parse(JsonPath.read(response, "$.certifiedAt")), stored.metadata().certifiedAt());
        assertFalse(stored.metadata().certifiedAt().isBefore(before.minusSeconds(1)));
        assertEquals("Verificación de campaña – Sánchez", stored.notes());
        var text = pdfText(storedBytes(stored));
        assertTrue(text.contains("Campaign 2028"));
        assertTrue(text.contains("CIP-49120-ING-AGRONOMO-SANCHEZ"));
        assertTrue(text.contains("Verificación de campaña – Sánchez"));
        assertTrue(text.contains("Total harvest (kg) 250.00"));
        assertFalse(text.contains(hash));
        assertEquals(1, eventsOf(plotId).size());
        var event = eventsOf(plotId).getFirst();
        assertEquals(hash, event.verificationHash());
        assertEquals(2028, event.campaignYear());
        assertEquals(stored.id().certificationId(), event.certificationId());
        assertEquals(stored.metadata().certifiedAt(), event.certifiedAt());
    }

    @Test
    void acceptsCertificationNotesAsAnAliasOfNotes() throws Exception {
        var plotId = createPlot("CRIOLLA");
        settleThreeConsecutive(plotId);
        mvc.perform(post(route(plotId)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campaignYear\":2028,\"auditorSignature\":\"sig\",\"certifiedBy\":\"Ing\","
                                + "\"cipNumber\":\"1\",\"certificationNotes\":\"Alias notes\"}"))
                .andExpect(status().isCreated());
        assertEquals("Alias notes", stored(plotId, 2028).notes());
    }

    @Test
    void certifiesWithoutNotesAndStatesWhyStabilizationIsNotDeterminable() throws Exception {
        var plotId = createPlot("CRIOLLA");
        settleThreeConsecutive(plotId);
        certify(plotId, 2028, "sig", "Ing", "1", null).andExpect(status().isCreated());
        var text = pdfText(storedBytes(stored(plotId, 2028)));
        assertTrue(text.contains("Notes None"));
        assertTrue(text.contains("baseline missing"));
        assertFalse(text.contains("ARR"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"auditorSignature\":\"sig\",\"certifiedBy\":\"Ing\",\"cipNumber\":\"1\"}",
            "{\"campaignYear\":2026,\"auditorSignature\":\"  \",\"certifiedBy\":\"Ing\",\"cipNumber\":\"1\"}",
            "{\"campaignYear\":2026,\"certifiedBy\":\"Ing\",\"cipNumber\":\"1\"}",
            "{\"campaignYear\":2026,\"auditorSignature\":\"sig\",\"cipNumber\":\"1\"}",
            "{\"campaignYear\":2026,\"auditorSignature\":\"sig\",\"certifiedBy\":\"Ing\"}",
            "{\"campaignYear\":1999,\"auditorSignature\":\"sig\",\"certifiedBy\":\"Ing\",\"cipNumber\":\"1\"}",
            "{\"campaignYear\":\"x\",\"auditorSignature\":\"sig\",\"certifiedBy\":\"Ing\",\"cipNumber\":\"1\"}",
            "{\"campaignYear\":2026,\"auditorSignature\":\"sig\",\"certifiedBy\":\"Ing\",\"cipNumber\":\"123456789012345678901\"}"
    })
    void rejectsInvalidBodiesWithoutStoringAnything(String payload) throws Exception {
        var plotId = createPlot("ARBEQUINA");
        settle(plotId, 2026, 100, 100).andExpect(status().isCreated());
        mvc.perform(post(route(plotId)).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
        assertEquals(0, certificationCount(plotId));
        assertTrue(eventsOf(plotId).isEmpty());
    }

    @Test
    void rejectsOverlongFieldsAndMalformedOrUnknownPlots() throws Exception {
        var plotId = createPlot("ARBEQUINA");
        settle(plotId, 2026, 100, 100).andExpect(status().isCreated());
        certify(plotId, 2026, "x".repeat(121), "Ing", "1", null).andExpect(status().isBadRequest());
        certify(plotId, 2026, "sig", "x".repeat(121), "1", null).andExpect(status().isBadRequest());
        certify(plotId, 2026, "sig", "Ing", "1", "x".repeat(1001)).andExpect(status().isBadRequest());
        certify("not-a-uuid", 2026, "sig", "Ing", "1", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        certify(UUID.randomUUID().toString(), 2026, "sig", "Ing", "1", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLOT_NOT_FOUND"));
        assertEquals(0, certificationCount(plotId));
    }

    @Test
    void aPlotWithoutAnyReportOrWithAnUnsettledCampaignIsUnprocessable() throws Exception {
        var plotId = createPlot("CRIOLLA");
        certify(plotId, 2026, "sig", "Ing", "1", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
        settle(plotId, 2026, 100, 100).andExpect(status().isCreated());
        certify(plotId, 2027, "sig", "Ing", "1", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
        assertEquals(0, certificationCount(plotId));
        assertTrue(eventsOf(plotId).isEmpty());
    }

    @Test
    void localizesTheProblemDetail() throws Exception {
        var plotId = createPlot("CRIOLLA");
        mvc.perform(post(route(plotId)).contentType(MediaType.APPLICATION_JSON).header("Accept-Language", "es")
                        .content(body(2026, "sig", "Ing", "1", null)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(
                        "La campana no tiene liquidacion oficial, por lo que no se puede certificar."));
    }

    @Test
    void certifyingTheSameCampaignAgainIsAConflictThatKeepsTheFirstUntouched() throws Exception {
        var plotId = createPlot("CRIOLLA");
        settleThreeConsecutive(plotId);
        certify(plotId, 2028, "first signature", "Ing One", "1", "first").andExpect(status().isCreated());
        var first = stored(plotId, 2028);

        certify(plotId, 2028, "second signature", "Ing Two", "2", "second")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DOSSIERCERTIFICATION_CONFLICT"));

        var after = stored(plotId, 2028);
        assertEquals(first, after);
        assertArrayEquals(storedBytes(first), storedBytes(after));
        assertEquals(1, certificationCount(plotId));
        assertEquals(1, eventsOf(plotId).size());
    }

    @Test
    void aCampaignWithInsufficientSettlementHistoryIsAConflict() throws Exception {
        var plotId = createPlot("CRIOLLA");
        recordHistory(plotId, 2022, 10000, 2000, 9000, 3000);
        settle(plotId, 2026, 7000, 0).andExpect(status().isCreated());
        settle(plotId, 2027, 0, 5000).andExpect(status().isCreated());

        certify(plotId, 2026, "sig", "Ing", "1", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOSSIERCERTIFICATION_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Certification requires at least three consecutive settled "
                        + "campaigns in the stabilization curve of this campaign; it cannot be certified yet."));
        certify(plotId, 2027, "sig", "Ing", "1", null).andExpect(status().isConflict());
        assertEquals(0, certificationCount(plotId));
        assertTrue(eventsOf(plotId).isEmpty());
    }

    @Test
    void aPlotWithoutPhenologyHistoryAndFewerThanThreeConsecutiveSettlementsIsAConflict() throws Exception {
        var plotId = createPlot("CRIOLLA");
        settle(plotId, 2026, 100, 100).andExpect(status().isCreated());

        certify(plotId, 2026, "sig", "Ing", "1", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOSSIERCERTIFICATION_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Certification requires at least three consecutive settled "
                        + "campaigns in the stabilization curve of this campaign; it cannot be certified yet."));
        settle(plotId, 2027, 300, 100).andExpect(status().isCreated());
        certify(plotId, 2027, "sig", "Ing", "1", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOSSIERCERTIFICATION_CONFLICT"));
        assertEquals(0, certificationCount(plotId));
        assertTrue(eventsOf(plotId).isEmpty());

        settle(plotId, 2028, 150, 100).andExpect(status().isCreated());
        certify(plotId, 2028, "sig", "Ing", "1", null).andExpect(status().isCreated());
        assertTrue(pdfText(storedBytes(stored(plotId, 2028))).contains("baseline missing"));
        assertEquals(1, certificationCount(plotId));
    }

    @Test
    void certifyingALaterCampaignLeavesTheEarlierCertificationByteIdentical() throws Exception {
        var plotId = createPlot("CRIOLLA");
        recordHistory(plotId, 2022, 10000, 2000, 9000, 3000);
        settle(plotId, 2026, 7000, 0).andExpect(status().isCreated());
        settle(plotId, 2027, 0, 5000).andExpect(status().isCreated());
        settle(plotId, 2028, 4000, 2500).andExpect(status().isCreated());
        var firstResponse = certify(plotId, 2028, "sig 2028", "Ing", "1", "campaign 2028")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var first = stored(plotId, 2028);
        var firstBytes = storedBytes(first);
        String firstHash = JsonPath.read(firstResponse, "$.verificationHash");
        assertEquals(firstHash, hashService.sha256(firstBytes).value());
        var firstText = pdfText(firstBytes);
        assertTrue(firstText.contains("Amplitude reduction rate (ARR) 0.7528"));

        settle(plotId, 2029, 3000, 1000).andExpect(status().isCreated());
        var secondResponse = certify(plotId, 2029, "sig 2029", "Ing", "1", "campaign 2029")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        var firstAfter = stored(plotId, 2028);
        assertArrayEquals(firstBytes, storedBytes(firstAfter));
        assertEquals(firstHash, firstAfter.metadata().verificationHash().value());
        assertEquals(first.metadata(), firstAfter.metadata());
        assertEquals("campaign 2028", firstAfter.notes());
        var second = stored(plotId, 2029);
        String secondHash = JsonPath.read(secondResponse, "$.verificationHash");
        assertEquals(secondHash, hashService.sha256(storedBytes(second)).value());
        assertNotEquals(firstHash, secondHash);
        assertTrue(pdfText(storedBytes(second)).contains("Campaign 2029"));
        assertEquals(2, certificationCount(plotId));
        assertEquals(2, eventsOf(plotId).size());
    }

    @Test
    void concurrentCertificationsOfTheSameCampaignStoreExactlyOne() throws Exception {
        var plotId = createPlot("CRIOLLA");
        settleThreeConsecutive(plotId);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<Integer> request = () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return certify(plotId, 2028, "sig", "Ing", "1", null).andReturn().getResponse().getStatus();
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(request);
            var second = executor.submit(request);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var statuses = new ArrayList<>(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)));
            Collections.sort(statuses);
            assertEquals(List.of(201, 409), statuses);
        }
        assertEquals(1, certificationCount(plotId));
        assertEquals(1, eventsOf(plotId).size());
    }

    private com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.DossierCertificationSnapshot stored(
            String plotId, int year) {
        return transactions.execute(tx -> reportRepository.findByPlotId(new PlotId(plotId)).orElseThrow()
                .certificationOf(new CampaignYear(year)).orElseThrow());
    }

    /** Reads the immutable bytes through the document port: the report itself carries none. */
    private byte[] storedBytes(
            com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.DossierCertificationSnapshot certification) {
        return transactions.execute(tx -> documentRepository.findByCertificationId(certification.id()).orElseThrow()
                .content());
    }

    private int certificationCount(String plotId) {
        return jdbc.queryForObject("select count(*) from dossier_certifications c join agronomic_reports r "
                + "on c.report_id = r.id where r.plot_id = ?", Integer.class, UUID.fromString(plotId));
    }

    private List<AgronomicDossierGeneratedEvent> eventsOf(String plotId) {
        return collector.events.stream().filter(e -> e.plotId().equals(plotId)).toList();
    }

    private static String pdfText(byte[] pdf) throws Exception {
        try (var document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    private ResultActions certify(String plotId, int year, String signature, String name, String cip, String notes)
            throws Exception {
        return mvc.perform(post(route(plotId)).contentType(MediaType.APPLICATION_JSON)
                .content(body(year, signature, name, cip, notes)));
    }

    private static String body(int year, String signature, String name, String cip, String notes) {
        return "{\"campaignYear\":%d,\"auditorSignature\":\"%s\",\"certifiedBy\":\"%s\",\"cipNumber\":\"%s\"%s}"
                .formatted(year, signature, name, cip, notes == null ? "" : ",\"notes\":\"" + notes + "\"");
    }

    private static String route(String plotId) {
        return "/api/v1/plots/" + plotId + "/certifications";
    }

    private void settleThreeConsecutive(String plotId) throws Exception {
        settle(plotId, 2026, 100, 100).andExpect(status().isCreated());
        settle(plotId, 2027, 300, 100).andExpect(status().isCreated());
        settle(plotId, 2028, 150, 100).andExpect(status().isCreated());
    }

    private ResultActions settle(String plotId, int year, double green, double black) throws Exception {
        return mvc.perform(post("/api/v1/plots/" + plotId + "/harvest-settlements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"campaignYear\":%d,\"greenOlivesKg\":%s,\"blackOlivesKg\":%s}".formatted(year, green,
                        black)));
    }

    private String createPlot(String variety) throws Exception {
        var payload = """
                {"name":"Cuartel %s","variety":"%s",
                 "polygonGeoJson":"{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                 "rowSpacingM":7.0,"treeSpacingM":5.0}
                """.formatted(UUID.randomUUID().toString().substring(0, 8), variety);
        var response = mvc.perform(post("/api/v1/plots").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private void recordHistory(String plotId, int firstYear, double... kilograms) throws Exception {
        for (int i = 0; i < kilograms.length; i++) {
            mvc.perform(post("/api/v1/plots/" + plotId + "/harvest-records").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"campaignYear\":%d,\"totalYieldKg\":%s}".formatted(firstYear + i, kilograms[i])))
                    .andExpect(status().isCreated());
        }
    }
}
