package com.arcadiadevs.viora.platform.settlement.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.settlement.application.commandservices.HarvestSettlementCommandService;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SettleCampaignHarvestCommand;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.ThinningExecutionConfirmedEvent;
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
import java.util.*;
import java.util.concurrent.*;

import static com.arcadiadevs.viora.platform.thinning.ThinningExecutionFixtures.prescribed;
import static org.junit.jupiter.api.Assertions.*;
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
                10.0, 10.0, null, null));
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

    private ResultActions settle(String plotId, int year, double green, double black, Double fruitsPerKg)
            throws Exception {
        return mvc.perform(post(route(plotId)).contentType(MediaType.APPLICATION_JSON)
                .content(body(year, green, black, fruitsPerKg)));
    }

    private static String body(int year, double green, double black, Double fruitsPerKg) {
        return "{\"campaignYear\":%d,\"greenOlivesKg\":%s,\"blackOlivesKg\":%s%s}".formatted(year, green, black,
                fruitsPerKg == null ? "" : ",\"commercialFruitsPerKg\":" + fruitsPerKg);
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
