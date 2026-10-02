package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.ConfirmThinningExecutionCommand;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.ThinningExecutionConfirmedEvent;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.CaliberCalibrationObservationRepository;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import com.arcadiadevs.viora.platform.thinning.application.commandservices.ConfirmThinningExecutionCommandService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static com.arcadiadevs.viora.platform.thinning.ThinningExecutionFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real controller, services, transaction boundaries and H2 persistence; no mocked application services. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:thinningexecution;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(ThinningExecutionControllerIntegrationTest.EventConfiguration.class)
class ThinningExecutionControllerIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired FruitThinningPrescriptionRepository repository;
    @Autowired CaliberCalibrationObservationRepository observationRepository;
    @Autowired ConfirmThinningExecutionCommandService service;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired CommittedEvents committedEvents;
    private MockMvc mvc;
    private TransactionTemplate transactions;
    private final LocalDate date = LocalDate.of(2026, 1, 10);

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        transactions = new TransactionTemplate(transactionManager);
        committedEvents.events.clear();
    }

    private String seed(PrescriptionStatus state, LocalDate cutoff) {
        return transactions.execute(tx -> repository.save(withStatus(state, cutoff)).snapshot().id().prescriptionId());
    }

    private String body(LocalDate executedDate) {
        return """
                {"executedDate":"%s","actualRemovalPercentage":25.0,"removedKg":420.0,
                 "laborCrewSize":4,"notes":"Aclareo registrado"}
                """.formatted(executedDate);
    }

    private String route(String id) {
        return "/api/v1/thinning-prescriptions/" + id + "/execution-confirmations";
    }

    private FruitThinningPrescriptionSnapshot reload(String id) {
        return transactions.execute(tx -> repository.findById(new PrescriptionId(id)).orElseThrow().snapshot());
    }

    @ParameterizedTest
    @CsvSource({"0,OPTIMAL,true", "1,LATE,false"})
    void persistsCompleteConfirmationAndRejectsReplay(int daysLate, String qualification, boolean opportune) throws Exception {
        var id = seed(PrescriptionStatus.PRESCRIBED, date);
        var executionDate = date.plusDays(daysLate);
        mvc.perform(post(route(id)).contentType(MediaType.APPLICATION_JSON).content(body(executionDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prescriptionId").value(id))
                .andExpect(jsonPath("$.confirmationId").isNotEmpty())
                .andExpect(jsonPath("$.confirmationStatus").value(qualification))
                .andExpect(jsonPath("$.executedDate").value(executionDate.toString()))
                .andExpect(jsonPath("$.isOpportune").value(opportune))
                .andExpect(jsonPath("$.removedKg").value(420.0))
                .andExpect(jsonPath("$.actualRemovalPercentage").value(25.0))
                .andExpect(jsonPath("$.laborCrewSize").value(4))
                .andExpect(jsonPath("$.notes").value("Aclareo registrado"))
                .andExpect(jsonPath("$.recordedAt").isNotEmpty())
                .andExpect(jsonPath("$.loadBalance.preThinningFruitsPerMeter").value(42.0))
                .andExpect(jsonPath("$.loadBalance.residualFruitsPerMeter").value(31.5))
                .andExpect(jsonPath("$.loadBalance.targetFruitsPerMeter").value(30.0))
                .andExpect(jsonPath("$.loadBalance.deltaFruitsPerMeter").value(1.5))
                .andExpect(jsonPath("$.loadBalance.loadRatio").value(1.05))
                .andExpect(jsonPath("$.loadBalance.loadState").value("MODERATE_OVERLOAD"))
                .andExpect(jsonPath("$.caliberProjection.status")
                        .value(opportune ? "NOT_CALIBRATED" : "NOT_ESTIMATED_LATE"))
                .andExpect(jsonPath("$.caliberProjection.mostLikelyFruitsPerKg").doesNotExist())
                .andExpect(jsonPath("$.caliberProjection.mostLikelySizeGrade").doesNotExist())
                .andExpect(jsonPath("$.caliberProjection.calibrationObservations").value(0))
                .andExpect(jsonPath("$.caliberProjection.requiredObservations").value(8))
                .andExpect(jsonPath("$.caliberProjection.model").value("LOAD_RESPONSE_V1"));
        var saved = reload(id);
        var evidence = saved.executionConfirmation();
        assertEquals(LoadBalance.of(42.0, 25.0, 30.0), evidence.loadBalance());
        assertEquals(opportune ? CaliberProjectionStatus.NOT_CALIBRATED : CaliberProjectionStatus.NOT_ESTIMATED_LATE,
                evidence.caliberProjection().status());
        assertEquals(PrescriptionStatus.EXECUTED, saved.status());
        assertEquals(executionDate, evidence.executionDate());
        assertEquals(420.0, evidence.removedKg());
        assertEquals(25.0, evidence.actualRemovalPercentage());
        assertEquals(4, evidence.laborCrewSize());
        assertEquals("Aclareo registrado", evidence.notes());
        assertEquals(qualification, evidence.timeliness().name());
        assertEquals(1, committedEvents.events.size());
        assertEquals(evidence.id().confirmationId(), committedEvents.events.getFirst().confirmationId());
        mvc.perform(post(route(id)).contentType(MediaType.APPLICATION_JSON).content(body(executionDate)))
                .andExpect(status().isConflict());
        assertEquals(evidence, reload(id).executionConfirmation());
        assertEquals(1, committedEvents.events.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"executedDate\":\"2026-01-10\",\"actualRemovalPercentage\":-1,\"removedKg\":1,\"laborCrewSize\":1}",
            "{\"executedDate\":\"2026-01-10\",\"actualRemovalPercentage\":101,\"removedKg\":1,\"laborCrewSize\":1}",
            "{\"executedDate\":\"2026-01-10\",\"actualRemovalPercentage\":25,\"removedKg\":-1,\"laborCrewSize\":1}",
            "{\"executedDate\":\"2026-01-10\",\"actualRemovalPercentage\":25,\"removedKg\":1,\"laborCrewSize\":0}",
            "{\"executedDate\":\"2026-01-10\",\"actualRemovalPercentage\":25,\"removedKg\":1,\"laborCrewSize\":-1}",
            "{\"executedDate\":\"bad-date\",\"actualRemovalPercentage\":25,\"removedKg\":1,\"laborCrewSize\":1}",
            "{\"executionDate\":\"2026-01-10\",\"actualRemovalPercentage\":25,\"removedKg\":1,\"laborCrewSize\":1}"
    })
    void rejectsInvalidBodiesWithoutChangingState(String payload) throws Exception {
        var id = seed(PrescriptionStatus.PRESCRIBED, date);
        mvc.perform(post(route(id)).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertEquals(PrescriptionStatus.PRESCRIBED, reload(id).status());
        assertNull(reload(id).executionConfirmation());
        assertTrue(committedEvents.events.isEmpty());
    }

    @Test
    void rejectsFutureDateAndOverlongNotes() throws Exception {
        var id = seed(PrescriptionStatus.PRESCRIBED, date);
        mvc.perform(post(route(id)).contentType(MediaType.APPLICATION_JSON)
                .content(body(LocalDate.now(ZoneOffset.UTC).plusDays(1)))).andExpect(status().isBadRequest());
        mvc.perform(post(route(id)).contentType(MediaType.APPLICATION_JSON)
                .content(body(date).replace("Aclareo registrado", "x".repeat(2001))))
                .andExpect(status().isBadRequest());
        assertNull(reload(id).executionConfirmation());
    }

    @Test
    void lateExecutionIncludesLocalizedReducedEffectivenessWarning() throws Exception {
        var id = seed(PrescriptionStatus.PRESCRIBED, date);
        mvc.perform(post(route(id)).contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "es").content(body(date.plusDays(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warning").value(org.hamcrest.Matchers.containsString("eficacia")));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 100})
    void acceptsZeroAndFullRemoval(double percentage) throws Exception {
        var id = seed(PrescriptionStatus.PRESCRIBED, date);
        mvc.perform(post(route(id)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(date).replace("25.0", Double.toString(percentage))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actualRemovalPercentage").value(percentage));
        assertEquals(percentage, reload(id).executionConfirmation().actualRemovalPercentage());
    }

    @Test
    void projectsCaliberForACalibratedVarietyEndToEnd() throws Exception {
        var plotId = createPlot("SEVILLANA");
        exactObservations("SEVILLANA").forEach(observationRepository::save);
        var id = transactions.execute(tx -> repository.save(prescribed(date, plotId)).snapshot().id().prescriptionId());
        // Residual 31.5 fruits/m with the calibrated curve W = 10 x (L/30)^-0.6 -> 103.0 fruits/kg
        mvc.perform(post(route(id)).contentType(MediaType.APPLICATION_JSON).content(body(date)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caliberProjection.status").value("ESTIMATED"))
                .andExpect(jsonPath("$.caliberProjection.mostLikelyFruitsPerKg").value(103.0))
                .andExpect(jsonPath("$.caliberProjection.mostLikelySizeGrade").value("101/110"))
                .andExpect(jsonPath("$.caliberProjection.sizeGradeLow").value("101/110"))
                .andExpect(jsonPath("$.caliberProjection.sizeGradeHigh").value("101/110"))
                .andExpect(jsonPath("$.caliberProjection.confidenceLevel").value(0.8))
                .andExpect(jsonPath("$.caliberProjection.calibrationObservations").value(8));
        var projection = reload(id).executionConfirmation().caliberProjection();
        assertEquals(CaliberProjectionStatus.ESTIMATED, projection.status());
        assertEquals(103.0, projection.mostLikelyFruitsPerKg(), 0.05);
    }

    @Test
    void storesOneCalibrationObservationPerPlotAndCampaign() {
        var first = exactObservations("ARBEQUINA").getFirst();
        observationRepository.save(first);
        observationRepository.save(new CaliberCalibrationObservation(first.plotId(), first.campaignYear(),
                "arbequina", first.residualFruitsPerMeter(), 350.0));
        var stored = observationRepository.findByVariety("ARBEQUINA");
        assertEquals(1, stored.size());
        assertEquals(350.0, stored.getFirst().commercialFruitsPerKg());
        assertTrue(observationRepository.findByVariety("MANZANILLA").isEmpty());
    }

    private String createPlot(String variety) throws Exception {
        var payload = """
                {"name":"Cuartel Calibre","variety":"%s",
                 "polygonGeoJson":"{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                 "rowSpacingM":7.0,"treeSpacingM":5.0}
                """.formatted(variety);
        var response = mvc.perform(post("/api/v1/plots").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    @Test
    void returnsNotFoundAndRejectsMalformedId() throws Exception {
        mvc.perform(post(route(UUID.randomUUID().toString())).contentType(MediaType.APPLICATION_JSON).content(body(date)))
                .andExpect(status().isNotFound());
        mvc.perform(post(route("invalid-uuid")).contentType(MediaType.APPLICATION_JSON).content(body(date)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = PrescriptionStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PRESCRIBED")
    void rejectsUnconfirmableStates(PrescriptionStatus state) throws Exception {
        var id = seed(state, date);
        mvc.perform(post(route(id)).contentType(MediaType.APPLICATION_JSON).content(body(date)))
                .andExpect(status().isConflict());
        assertNull(reload(id).executionConfirmation());
        assertTrue(committedEvents.events.isEmpty());
    }

    @Test
    void rejectsMissingInterventionWindow() throws Exception {
        var id = seed(PrescriptionStatus.PRESCRIBED, null);
        mvc.perform(post(route(id)).contentType(MediaType.APPLICATION_JSON).content(body(date)))
                .andExpect(status().isConflict());
    }

    @Test
    void concurrentRequestsCreateExactlyOneConfirmation() throws Exception {
        var id = seed(PrescriptionStatus.PRESCRIBED, date);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<Integer> request = () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return mvc.perform(post(route(id)).contentType(MediaType.APPLICATION_JSON).content(body(date)))
                    .andReturn().getResponse().getStatus();
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
        assertEquals(1, jdbc.queryForObject("select count(*) from execution_confirmations where prescription_id = ?",
                Integer.class, UUID.fromString(id)));
        assertEquals(1, committedEvents.events.size());
    }

    @Test
    void rollbackRemovesEvidenceAndDoesNotDeliverAfterCommitEvent() {
        var id = seed(PrescriptionStatus.PRESCRIBED, date);
        transactions.executeWithoutResult(tx -> {
            assertTrue(service.handle(new ConfirmThinningExecutionCommand(id, date, 25.0, 420.0, 4, null)).isSuccess());
            tx.setRollbackOnly();
        });
        assertEquals(PrescriptionStatus.PRESCRIBED, reload(id).status());
        assertNull(reload(id).executionConfirmation());
        assertTrue(committedEvents.events.isEmpty());
    }

    @TestConfiguration
    static class EventConfiguration {
        @Bean CommittedEvents committedEvents() { return new CommittedEvents(); }
    }

    static class CommittedEvents {
        final List<ThinningExecutionConfirmedEvent> events = new CopyOnWriteArrayList<>();
        @TransactionalEventListener
        public void onConfirmation(ThinningExecutionConfirmedEvent event) { events.add(event); }
    }
}
