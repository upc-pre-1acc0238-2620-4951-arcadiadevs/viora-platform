package com.arcadiadevs.viora.platform.settlement.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.settlement.application.commandservices.HarvestSettlementCommandService;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SettleCampaignHarvestCommand;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.ThinningExecutionConfirmedEvent;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CommercialSizeScale;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.CaliberCalibrationObservationRepository;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;


import static com.arcadiadevs.viora.platform.thinning.ThinningExecutionFixtures.prescribed;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real controllers, services, event handlers and H2 persistence across Orchard, Phenology, Thinning and Settlement. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:harvestsettlement;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class HarvestSettlementControllerIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired AgronomicReportRepository reportRepository;
    @Autowired FruitThinningPrescriptionRepository prescriptionRepository;
    @Autowired CaliberCalibrationObservationRepository observationRepository;
    @Autowired HarvestSettlementCommandService service;
    @Autowired ApplicationEventPublisher publisher;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    private MockMvc mvc;
    private TransactionTemplate transactions;
    private final LocalDate windowClosesOn = LocalDate.of(2026, 1, 10);
    /** Weighing date the bodies carry: yesterday, so it is never in the future whatever the day the suite runs. */
    private static final LocalDate WEIGHED_ON = LocalDate.now(ZoneOffset.UTC).minusDays(1);
    /**
     * Campaign years the receipt tests settle. Receipt numbers are numbered per producer and campaign, so a test
     * that asserts an exact number needs a campaign nothing else in this class settles.
     */
    private static final int RECEIPT_YEAR = 2030;
    private static final int REPLAY_YEAR = 2031;
    private static final int CONFLICT_YEAR = 2032;
    private static final int REUSED_KEY_YEAR = 2033;
    private static final int GETS_YEAR = 2034;
    private static final int BLANK_TICKET_YEAR = 2035;
    private static final int SEEDED_COUNTER_YEAR = 2036;
    private static final int REJECTED_YEAR = 2037;
    private static final int RACE_YEAR = 2038;
    private static final int SHARED_KEY_RACE_YEAR = 2039;
    private static final int TRIMMED_TICKET_YEAR = 2040;
    /** Caliber of the settled tests, and the IOC grade it maps to, computed by the scale that owns it. */
    private static final double CALIBER = 105.0;
    private static final String CALIBER_GRADE = CommercialSizeScale.gradeOf(CALIBER);
    private static final String MILL_TICKET = "MT-88213";
    /** Shape of every receipt number the API can return. */
    private static final String RECEIPT_NUMBER = "VR-\\d{2}-\\d{4,6}";

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        transactions = new TransactionTemplate(transactionManager);
    }

    @Test
    void settlesCampaignsAndEvaluatesStabilizationAgainstThePhenologyBaseline() throws Exception {
        var plotId = createPlot("CRIOLLA");
        recordHistory(plotId, 2022, 10000, 2000, 9000, 3000);

        settle(plotId, 2026, 7000, 0, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.reportId").isNotEmpty())
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.campaignYear").value(2026))
                .andExpect(jsonPath("$.totalYieldKg").value(7000.0))
                .andExpect(jsonPath("$.status").value("SETTLED"))
                .andExpect(jsonPath("$.settledAt").isNotEmpty())
                .andExpect(jsonPath("$.thinningBalance.status").value("NOT_RECORDED"))
                .andExpect(jsonPath("$.stabilization.status").value("INSUFFICIENT_SETTLEMENTS"))
                .andExpect(jsonPath("$.stabilization.baselineCampaigns").value(4))
                .andExpect(jsonPath("$.stabilization.baselineYieldKg").value(6000.0))
                .andExpect(jsonPath("$.stabilization.requiredConsecutivePairs").value(2));
        settle(plotId, 2027, 0, 5000, null).andExpect(status().isCreated());
        settle(plotId, 2028, 4000, 2500, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalYieldKg").value(6500.0))
                .andExpect(jsonPath("$.stabilization.status").value("EVALUATED"))
                .andExpect(jsonPath("$.stabilization.settledCampaigns").value(3))
                .andExpect(jsonPath("$.stabilization.amplitudeReductionRate").value(0.7528))
                .andExpect(jsonPath("$.stabilization.targetAchieved").value(true));

        settle(plotId, 2027, 1, 1, null).andExpect(status().isConflict());

        var report = transactions.execute(tx -> reportRepository.findByPlotId(new PlotId(plotId)).orElseThrow());
        var first = report.settlementOf(new CampaignYear(2026)).orElseThrow();
        assertEquals(3, report.snapshot().settlements().size());
        assertEquals(StabilizationStatus.INSUFFICIENT_SETTLEMENTS, first.trendCurve().status());
        assertEquals(5000.0, report.settlementOf(new CampaignYear(2027)).orElseThrow().totalHarvestWeight().kilograms());
        assertEquals(StabilizationStatus.EVALUATED, report.trendCurve().orElseThrow().status());
    }

    @Test
    void freezesTheThinningOfTheSameCampaignAndFeedsTheCaliberModel() throws Exception {
        var plotId = createPlot("SEVILLANA");
        confirmThinning(plotId, windowClosesOn);

        settle(plotId, 2026, 8200, 6050, 103.0)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commercialFruitsPerKg").value(103.0))
                .andExpect(jsonPath("$.thinningBalance.status").value("EXECUTED_ON_TIME"))
                .andExpect(jsonPath("$.thinningBalance.executedDate").value("2026-01-10"))
                .andExpect(jsonPath("$.thinningBalance.prescribedRemovalPercentage").value(25.0))
                .andExpect(jsonPath("$.thinningBalance.actualRemovalPercentage").value(25.0))
                .andExpect(jsonPath("$.thinningBalance.deviationPercentagePoints").value(0.0));
        settle(plotId, 2027, 5000, 0, 110.0)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.thinningBalance.status").value("NOT_RECORDED"));

        var observations = observationRepository.findByVariety("SEVILLANA").stream()
                .filter(o -> o.plotId().plotId().equals(plotId)).toList();
        assertEquals(1, observations.size());
        assertEquals(2026, observations.getFirst().campaignYear().value());
        assertEquals(6.3, observations.getFirst().residualFruitsPerShoot(), 1e-9);
        assertEquals(103.0, observations.getFirst().commercialFruitsPerKg());
    }

    @Test
    void lateThinningIsFrozenAsLateAndNeverCalibratesTheCaliberModel() throws Exception {
        var plotId = createPlot("MANZANILLA");
        confirmThinning(plotId, windowClosesOn.plusDays(5));
        settle(plotId, 2026, 8000, 0, 120.0)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.thinningBalance.status").value("EXECUTED_LATE"));
        assertTrue(observationRepository.findByVariety("MANZANILLA").isEmpty());
    }

    @Test
    void ignoresARepeatedThinningEvent() {
        var plotId = UUID.randomUUID().toString();
        var event = new ThinningExecutionConfirmedEvent(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), plotId, 2026, windowClosesOn, 25.0, 420.0, 4, "OPTIMAL",
                Instant.now(), 30.0);
        transactions.executeWithoutResult(tx -> publisher.publishEvent(event));
        transactions.executeWithoutResult(tx -> publisher.publishEvent(event));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from settlement_thinning_executions where plot_id = ?", Integer.class,
                UUID.fromString(plotId)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"campaignYear\":2026,\"greenOlivesKg\":-1,\"blackOlivesKg\":10}",
            "{\"campaignYear\":2026,\"greenOlivesKg\":0,\"blackOlivesKg\":0}",
            "{\"campaignYear\":1999,\"greenOlivesKg\":10,\"blackOlivesKg\":10}",
            "{\"campaignYear\":2101,\"greenOlivesKg\":10,\"blackOlivesKg\":10}",
            "{\"campaignYear\":2026,\"greenOlivesKg\":10}",
            "{\"campaignYear\":2026,\"greenOlivesKg\":10,\"blackOlivesKg\":10,\"commercialFruitsPerKg\":0}",
            "{\"campaignYear\":\"x\",\"greenOlivesKg\":10,\"blackOlivesKg\":10}"
    })
    void rejectsInvalidBodiesWithoutOpeningAReport(String payload) throws Exception {
        var plotId = createPlot("ARBEQUINA");
        mvc.perform(post(route(plotId)).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
        assertTrue(transactions.execute(tx -> reportRepository.findByPlotId(new PlotId(plotId))).isEmpty());
    }

    @Test
    void rejectsOverlongNotesAndMalformedOrUnknownPlots() throws Exception {
        var plotId = createPlot("ARBEQUINA");
        mvc.perform(post(route(plotId)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(2026, 10, 10, null).replace("}", ",\"notes\":\"" + "x".repeat(1001) + "\"}")))
                .andExpect(status().isBadRequest());
        settle(UUID.randomUUID().toString(), 2026, 10, 10, null).andExpect(status().isNotFound());
        settle("not-a-uuid", 2026, 10, 10, null).andExpect(status().isBadRequest());
    }

    @Test
    void forbidsSettlingAPlotOfAnotherProducer() throws Exception {
        var plotId = createPlot("CRIOLLA");
        var result = service.handle(new SettleCampaignHarvestCommand(plotId, UUID.randomUUID().toString(), 2026,
                10.0, 10.0, null, null, WEIGHED_ON, null, null));
        assertEquals("PLOT_FORBIDDEN", result.failure().orElseThrow().code());
    }

    @Test
    void concurrentSettlementsOfTheSameCampaignStoreExactlyOne() throws Exception {
        var plotId = createPlot("CRIOLLA");
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<Integer> request = () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return settle(plotId, 2026, 100, 100, null).andReturn().getResponse().getStatus();
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(request);
            var second = executor.submit(request);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var statuses = new ArrayList<>(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)));
            Collections.sort(statuses);
            assertEquals(List.of(201, 409), statuses);
        }
        assertEquals(1, jdbc.queryForObject("select count(*) from harvest_settlements s join agronomic_reports r "
                + "on s.report_id = r.id where r.plot_id = ?", Integer.class, UUID.fromString(plotId)));
    }

    @Test
    void listsTheSettlementsOfAPlotNewestCampaignFirst() throws Exception {
        var plotId = createPlot("CRIOLLA");
        settle(plotId, 2025, 5000, 500, null).andExpect(status().isCreated());
        settle(plotId, 2026, 7000, 0, null).andExpect(status().isCreated());

        mvc.perform(get(route(plotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].reportId").isNotEmpty())
                .andExpect(jsonPath("$[0].plotId").value(plotId))
                .andExpect(jsonPath("$[0].campaignYear").value(2026))
                .andExpect(jsonPath("$[0].totalYieldKg").value(7000.0))
                .andExpect(jsonPath("$[0].status").value("SETTLED"))
                .andExpect(jsonPath("$[0].settledAt").isNotEmpty())
                .andExpect(jsonPath("$[0].thinningBalance.status").value("NOT_RECORDED"))
                .andExpect(jsonPath("$[0].stabilization.status").isNotEmpty())
                .andExpect(jsonPath("$[1].campaignYear").value(2025))
                .andExpect(jsonPath("$[1].totalYieldKg").value(5500.0))
                .andExpect(jsonPath("$[0].receiptNumber").value(matchesPattern(RECEIPT_NUMBER)))
                .andExpect(jsonPath("$[0].weighedOn").value(WEIGHED_ON.toString()))
                .andExpect(jsonPath("$[1].receiptNumber").value(matchesPattern(RECEIPT_NUMBER)))
                .andExpect(jsonPath("$[1].weighedOn").value(WEIGHED_ON.toString()))
                // No caliber and no mill ticket on these two settlements.
                .andExpect(jsonPath("$[0].commercialSizeGrade").value(nullValue()))
                .andExpect(jsonPath("$[0].millTicketNumber").value(nullValue()))
                .andExpect(jsonPath("$[1].commercialSizeGrade").value(nullValue()))
                .andExpect(jsonPath("$[1].millTicketNumber").value(nullValue()));

        List<String> listed = JsonPath.read(
                mvc.perform(get(route(plotId))).andReturn().getResponse().getContentAsString(), "$[*].receiptNumber");
        assertNotEquals(listed.get(0), listed.get(1), "each settlement carries its own receipt number");
    }

    @Test
    void listsNoSettlementForAPlotThatHasNotClosedACampaign() throws Exception {
        mvc.perform(get(route(createPlot("ARBEQUINA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void returnsTheSettlementOfOneSettledCampaignAndNotOfTheOthers() throws Exception {
        var plotId = createPlot("MANZANILLA");
        settle(plotId, 2026, 7000, 0, null).andExpect(status().isCreated());

        mvc.perform(get(route(plotId) + "/2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.campaignYear").value(2026))
                .andExpect(jsonPath("$.greenOlivesKg").value(7000.0))
                .andExpect(jsonPath("$.totalYieldKg").value(7000.0))
                .andExpect(jsonPath("$.status").value("SETTLED"))
                .andExpect(jsonPath("$.thinningBalance.status").value("NOT_RECORDED"))
                .andExpect(jsonPath("$.stabilization.status").isNotEmpty())
                .andExpect(jsonPath("$.receiptNumber").value(matchesPattern(RECEIPT_NUMBER)))
                .andExpect(jsonPath("$.weighedOn").value(WEIGHED_ON.toString()))
                .andExpect(jsonPath("$.millTicketNumber").value(nullValue()))
                .andExpect(jsonPath("$.commercialSizeGrade").value(nullValue()));
        mvc.perform(get(route(plotId) + "/2029")).andExpect(status().isNotFound());
    }

    @Test
    void rejectsMalformedRoutesAndUnknownPlotsWhenConsultingSettlements() throws Exception {
        var plotId = createPlot("CRIOLLA");
        settle(plotId, 2026, 10, 10, null).andExpect(status().isCreated());
        mvc.perform(get(route("not-a-uuid"))).andExpect(status().isBadRequest());
        mvc.perform(get(route(plotId) + "/x")).andExpect(status().isBadRequest());
        mvc.perform(get(route(plotId) + "/1999")).andExpect(status().isBadRequest());
        mvc.perform(get(route(UUID.randomUUID().toString()))).andExpect(status().isNotFound());
        mvc.perform(get(route(UUID.randomUUID().toString()) + "/2026")).andExpect(status().isNotFound());
    }

    // --- the receipt, weighing and idempotency contract of POST ---

    @Test
    void settlesACampaignWithItsReceiptWeighingDateMillTicketAndSizeGrade() throws Exception {
        var plotId = createPlot("CRIOLLA");

        settleWith(plotId, null, RECEIPT_YEAR, CALIBER, MILL_TICKET)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignYear").value(RECEIPT_YEAR))
                .andExpect(jsonPath("$.receiptNumber").value("VR-30-0001"))
                .andExpect(jsonPath("$.weighedOn").value(WEIGHED_ON.toString()))
                .andExpect(jsonPath("$.millTicketNumber").value(MILL_TICKET))
                .andExpect(jsonPath("$.commercialFruitsPerKg").value(CALIBER))
                .andExpect(jsonPath("$.commercialSizeGrade").value(CALIBER_GRADE));

        var row = settlementRow(plotId, RECEIPT_YEAR);
        assertEquals("VR-30-0001", row.get("receipt_number"));
        assertEquals(WEIGHED_ON.toString(), row.get("weighed_on").toString());
        assertEquals(MILL_TICKET, row.get("mill_ticket_number"));
    }

    @Test
    void replaysTheSameKeyInsteadOfSettlingTheCampaignTwice() throws Exception {
        var plotId = createPlot("SEVILLANA");
        var body = settleBody(REPLAY_YEAR, CALIBER, MILL_TICKET);

        var created = postSettlement(plotId, "replay-key", body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var replayed = postSettlement(plotId, "replay-key", body).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(JsonPath.<String>read(created, "$.id"), JsonPath.<String>read(replayed, "$.id"));
        assertEquals("VR-31-0001", JsonPath.<String>read(replayed, "$.receiptNumber"));
        // Everything but the instant is the very same voucher. The instant names the same moment, kept with the
        // microsecond precision the database stores, which is what a reload can promise.
        assertEquals(voucherWithoutInstant(created), voucherWithoutInstant(replayed));
        Instant settledAt = Instant.parse(JsonPath.read(created, "$.settledAt"));
        Instant replayedAt = Instant.parse(JsonPath.read(replayed, "$.settledAt"));
        assertTrue(Duration.between(replayedAt, settledAt).abs().compareTo(MICROSECOND) <= 0,
                "the replay names the same instant the first call settled at");
        assertEquals(1, settlementsOf(plotId), "the replay stored nothing");
        assertEquals(1, lastSequenceOf(REPLAY_YEAR), "the replay consumed no receipt number");
    }

    @Test
    void rejectsACampaignWhoseWeighingDateIsInTheFuture() throws Exception {
        var plotId = createPlot("MANZANILLA");

        postSettlement(plotId, null, "{\"campaignYear\":%d,\"greenOlivesKg\":7000,\"blackOlivesKg\":0,"
                        .formatted(RECEIPT_YEAR + 20) + "\"weighedOn\":\""
                        + LocalDate.now(ZoneOffset.UTC).plusDays(1) + "\"}")
                .andExpect(status().isBadRequest())
                // The client is told the weighing date is the problem, in the language it asked for, not the key.
                .andExpect(jsonPath("$.detail").value(containsString(message("settlement.weighed_on.future"))))
                .andExpect(jsonPath("$.detail").value(not(containsString("settlement.weighed_on.future"))));
        assertEquals(0, settlementsOf(plotId));
    }

    @Test
    void rejectsAnOverlongMillTicketAndAnOverlongIdempotencyKey() throws Exception {
        var plotId = createPlot("ARBEQUINA");
        var year = RECEIPT_YEAR + 21;

        postSettlement(plotId, null, settleBody(year, null, "x".repeat(31)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(message("settlement.mill_ticket.too_long"))))
                .andExpect(jsonPath("$.detail").value(not(containsString("{"))));
        postSettlement(plotId, "k".repeat(65), settleBody(year, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(message("settlement.idempotency_key.too_long"))))
                .andExpect(jsonPath("$.detail").value(not(containsString("{"))));
        assertEquals(0, settlementsOf(plotId));
    }

    @Test
    void theConflictOfAnAlreadySettledCampaignCarriesTheExistingSettlement() throws Exception {
        var plotId = createPlot("CRIOLLA");
        var created = postSettlement(plotId, "first-key", settleBody(CONFLICT_YEAR, CALIBER, MILL_TICKET))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        // Another key on the campaign that is already settled is not a replay: it is a conflict.
        postSettlement(plotId, "second-key", settleBody(CONFLICT_YEAR, CALIBER, MILL_TICKET))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HARVESTSETTLEMENT_CONFLICT"))
                .andExpect(jsonPath("$.existingSettlement.campaignYear").value(CONFLICT_YEAR))
                .andExpect(jsonPath("$.existingSettlement.totalYieldKg").value(7000.0))
                .andExpect(jsonPath("$.existingSettlement.receiptNumber").value("VR-32-0001"))
                .andExpect(jsonPath("$.existingSettlement.weighedOn").value(WEIGHED_ON.toString()));
        assertEquals("VR-32-0001", JsonPath.<String>read(created, "$.receiptNumber"));
        assertEquals(1, settlementsOf(plotId));
        assertEquals(1, lastSequenceOf(CONFLICT_YEAR), "the conflict consumed no receipt number");
    }

    @Test
    void rejectsAnIdempotencyKeyReusedForAnotherPlotOfTheSameProducer() throws Exception {
        var first = createPlot("CRIOLLA");
        var second = createPlot("SEVILLANA");
        var body = settleBody(REUSED_KEY_YEAR, null, null);
        postSettlement(first, "shared-key", body).andExpect(status().isCreated());

        postSettlement(second, "shared-key", body)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.detail").value(containsString(message("settlement.idempotency_key.reused"))));
        assertEquals(1, settlementsOf(first));
        assertEquals(0, settlementsOf(second), "the rejected replay settled nothing");
        assertEquals(1, lastSequenceOf(REUSED_KEY_YEAR), "the rejected replay consumed no receipt number");
    }

    @Test
    void bothGetEndpointsExposeTheReceiptWeighingAndMillTicket() throws Exception {
        var plotId = createPlot("MANZANILLA");
        settleWith(plotId, null, GETS_YEAR, CALIBER, MILL_TICKET).andExpect(status().isCreated());

        mvc.perform(get(route(plotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].receiptNumber").value("VR-34-0001"))
                .andExpect(jsonPath("$[0].weighedOn").value(WEIGHED_ON.toString()))
                .andExpect(jsonPath("$[0].millTicketNumber").value(MILL_TICKET))
                .andExpect(jsonPath("$[0].commercialSizeGrade").value(CALIBER_GRADE));
        mvc.perform(get(route(plotId) + "/" + GETS_YEAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value("VR-34-0001"))
                .andExpect(jsonPath("$.weighedOn").value(WEIGHED_ON.toString()))
                .andExpect(jsonPath("$.millTicketNumber").value(MILL_TICKET))
                .andExpect(jsonPath("$.commercialSizeGrade").value(CALIBER_GRADE));
    }

    @Test
    void aBlankMillTicketIsStoredAsAbsentAndAnAbsentCaliberHasNoSizeGrade() throws Exception {
        var plotId = createPlot("ARBEQUINA");

        settleWith(plotId, null, BLANK_TICKET_YEAR, null, "   ").andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").value("VR-35-0001"))
                .andExpect(jsonPath("$.millTicketNumber").value(nullValue()))
                .andExpect(jsonPath("$.commercialSizeGrade").value(nullValue()));

        var row = settlementRow(plotId, BLANK_TICKET_YEAR);
        assertNull(row.get("mill_ticket_number"), "a blank ticket is not stored as an empty string");
        assertEquals("VR-35-0001", row.get("receipt_number"));
    }

    @Test
    void aMissingOrEmptyCounterContinuesAfterTheReceiptNumbersAlreadyIssued() throws Exception {
        var first = createPlot("CRIOLLA");
        var second = createPlot("SEVILLANA");
        var third = createPlot("MANZANILLA");
        var fourth = createPlot("ARBEQUINA");
        settleWith(first, null, SEEDED_COUNTER_YEAR, null, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").value("VR-36-0001"));
        settleWith(second, null, SEEDED_COUNTER_YEAR, null, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").value("VR-36-0002"));

        // Backfilled rows: numbers exist on the settlements, but the counter is gone.
        jdbc.update("delete from harvest_receipt_counters where campaign_year = ?", SEEDED_COUNTER_YEAR);
        settleWith(third, null, SEEDED_COUNTER_YEAR, null, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").value("VR-36-0003"));

        // Or opened empty after the numbers were written.
        jdbc.update("update harvest_receipt_counters set last_sequence = 0 where campaign_year = ?",
                SEEDED_COUNTER_YEAR);
        settleWith(fourth, null, SEEDED_COUNTER_YEAR, null, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").value("VR-36-0004"));
        assertEquals(4, lastSequenceOf(SEEDED_COUNTER_YEAR));
    }

    @Test
    void aRejectedRequestDoesNotConsumeAReceiptNumber() throws Exception {
        var plotId = createPlot("CRIOLLA");
        var future = LocalDate.now(ZoneOffset.UTC).plusDays(1);

        postSettlement(plotId, null, "{\"campaignYear\":%d,\"greenOlivesKg\":0,\"blackOlivesKg\":0,\"weighedOn\":\"%s\"}"
                .formatted(REJECTED_YEAR, WEIGHED_ON)).andExpect(status().isBadRequest());
        settleWith(plotId, null, REJECTED_YEAR, -1.0, null).andExpect(status().isBadRequest());
        postSettlement(plotId, null, "{\"campaignYear\":%d,\"greenOlivesKg\":7000,\"blackOlivesKg\":0,\"notes\":\"%s\",\"weighedOn\":\"%s\"}"
                .formatted(REJECTED_YEAR, "n".repeat(1001), WEIGHED_ON)).andExpect(status().isBadRequest());
        settleWith(plotId, null, REJECTED_YEAR, null, "x".repeat(31)).andExpect(status().isBadRequest());
        postSettlement(plotId, null, "{\"campaignYear\":%d,\"greenOlivesKg\":7000,\"blackOlivesKg\":0,\"weighedOn\":\"%s\"}"
                .formatted(REJECTED_YEAR, future)).andExpect(status().isBadRequest());
        assertEquals(0, settlementsOf(plotId));

        settleWith(plotId, null, REJECTED_YEAR, null, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").value("VR-37-0001"));
        assertEquals(1, lastSequenceOf(REJECTED_YEAR));
    }

    @Test
    void theLoserOfTwoConcurrentRequestsWithTheSameKeyReplaysTheWinner() throws Exception {
        var plotId = createPlot("CRIOLLA");
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<org.springframework.mock.web.MockHttpServletResponse> request = () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return postSettlement(plotId, "race-key", settleBody(RACE_YEAR, null, null)).andReturn().getResponse();
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(request);
            var second = executor.submit(request);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var responses = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            var statuses = new ArrayList<>(responses.stream().map(r -> r.getStatus()).toList());
            Collections.sort(statuses);
            assertEquals(List.of(200, 201), statuses);
            assertEquals(JsonPath.<String>read(responses.get(0).getContentAsString(), "$.id"),
                    JsonPath.<String>read(responses.get(1).getContentAsString(), "$.id"));
        }
        assertEquals(1, settlementsOf(plotId));
        assertEquals(1, lastSequenceOf(RACE_YEAR), "only the winner consumed a receipt number");
    }

    @Test
    void theSameKeyOnTwoPlotsAtOnceSettlesOneAndRejectsTheOtherWithAnUnprocessableKey() throws Exception {
        var plots = List.of(createPlot("CRIOLLA"), createPlot("SEVILLANA"));
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = plots.stream().map(plot -> executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return postSettlement(plot, "shared-race-key", settleBody(SHARED_KEY_RACE_YEAR, null, null))
                        .andReturn().getResponse().getStatus();
            })).toList();
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var statuses = new ArrayList<Integer>();
            for (var future : futures) {
                statuses.add(future.get(20, TimeUnit.SECONDS));
            }
            Collections.sort(statuses);
            assertEquals(List.of(201, 422), statuses);
        }
        assertEquals(1, settlementsOf(plots.get(0)) + settlementsOf(plots.get(1)));
        assertEquals(1, lastSequenceOf(SHARED_KEY_RACE_YEAR));
    }

    // --- helpers of the receipt contract ---

    private ResultActions settleWith(String plotId, String idempotencyKey, int year, Double fruitsPerKg,
            String millTicket) throws Exception {
        return postSettlement(plotId, idempotencyKey, settleBody(year, fruitsPerKg, millTicket));
    }

    private ResultActions postSettlement(String plotId, String idempotencyKey, String json) throws Exception {
        var request = post(route(plotId)).contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(idempotencyKey == null ? request : request.header("Idempotency-Key", idempotencyKey));
    }

    private static String settleBody(int year, Double fruitsPerKg, String millTicket) {
        return "{\"campaignYear\":%d,\"greenOlivesKg\":7000,\"blackOlivesKg\":0%s%s,\"weighedOn\":\"%s\"}"
                .formatted(year, fruitsPerKg == null ? "" : ",\"commercialFruitsPerKg\":" + fruitsPerKg,
                        millTicket == null ? "" : ",\"millTicketNumber\":\"" + millTicket + "\"", WEIGHED_ON);
    }

    private static final Duration MICROSECOND = Duration.of(1, ChronoUnit.MICROS);

    /** Every field of a response but its instant, as an order-independent map. */
    private static Map<String, Object> voucherWithoutInstant(String json) {
        var fields = new TreeMap<>((Map<String, Object>) JsonPath.read(json, "$"));
        fields.remove("settledAt");
        return fields;
    }

    /** The settled voucher of one campaign of a plot, as the database kept it. */
    private Map<String, Object> settlementRow(String plotId, int campaignYear) {
        return jdbc.queryForList("select receipt_number, weighed_on, mill_ticket_number from harvest_settlements s "
                + "join agronomic_reports r on s.report_id = r.id where r.plot_id = ? and s.campaign_year = ?",
                UUID.fromString(plotId), campaignYear).getFirst();
    }

    private int settlementsOf(String plotId) {
        return jdbc.queryForObject("select count(*) from harvest_settlements s join agronomic_reports r "
                + "on s.report_id = r.id where r.plot_id = ?", Integer.class, UUID.fromString(plotId));
    }

    /** Every settlement of this class belongs to the one mock producer, so its counters hold the campaign. */
    private int lastSequenceOf(int campaignYear) {
        return jdbc.queryForObject("select last_sequence from harvest_receipt_counters where campaign_year = ?",
                Integer.class, campaignYear);
    }

    private static String message(String key) {
        return ResourceBundle.getBundle("messages", Locale.getDefault()).getString(key);
    }

    private ResultActions settle(String plotId, int year, double green, double black, Double fruitsPerKg)
            throws Exception {
        return mvc.perform(post(route(plotId)).contentType(MediaType.APPLICATION_JSON)
                .content(body(year, green, black, fruitsPerKg)));
    }

    private static String body(int year, double green, double black, Double fruitsPerKg) {
        return "{\"campaignYear\":%d,\"greenOlivesKg\":%s,\"blackOlivesKg\":%s%s,\"weighedOn\":\"%s\"}"
                .formatted(year, green, black, fruitsPerKg == null ? "" : ",\"commercialFruitsPerKg\":" + fruitsPerKg,
                        WEIGHED_ON);
    }

    private static String route(String plotId) {
        return "/api/v1/plots/" + plotId + "/harvest-settlements";
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

    /** Seeds the 2026 prescription of the plot (window closing 2026-01-10) and confirms it through its endpoint. */
    private void confirmThinning(String plotId, LocalDate executedDate) throws Exception {
        var prescriptionId = transactions.execute(tx ->
                prescriptionRepository.save(prescribed(windowClosesOn, plotId)).snapshot().id().prescriptionId());
        mvc.perform(post("/api/v1/thinning-prescriptions/" + prescriptionId + "/execution-confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"executedDate\":\"%s\",\"actualRemovalPercentage\":25.0,\"removedKg\":420.0,\"laborCrewSize\":4}"
                                .formatted(executedDate)))
                .andExpect(status().isCreated());
    }
}
