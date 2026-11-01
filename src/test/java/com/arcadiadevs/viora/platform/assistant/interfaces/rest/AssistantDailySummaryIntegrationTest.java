package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.assistant.infrastructure.scheduling.AssistantDailySummaryScheduler;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import com.arcadiadevs.viora.platform.telemetry.application.commandservices.AgroclimaticIncidentCommandService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.WeatherForecastProvider;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastDay;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RaiseAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.AmbientTemperature;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.Percentage;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.WindSpeed;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.ThinningPlanDetail;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real controllers, services, facades and H2: the daily summary of the home screen, for the whole field or one plot,
 * precomputed by the daily job and written on demand when missing or stale, in its own table. The thinning plan is
 * stubbed at the Thinning facade, the forecast at its provider; the model is disabled in tests, so every summary is
 * its template.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantdailysummary;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "viora.assistant.daily-summary.enabled=true"
})
@DisplayName("Assistant daily summary (GET /daily-summary and the daily job)")
class AssistantDailySummaryIntegrationTest {

    private static final String SUMMARY = "/api/v1/assistant/daily-summary";
    private static final String ANSWERS = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
    @Autowired AgroclimaticIncidentCommandService incidentCommandService;
    @Autowired AssistantDailySummaryScheduler scheduler;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    @MockitoBean WeatherForecastProvider weatherForecastProvider;
    @MockitoBean ThinningContextFacade thinningContextFacade;
    private MockMvc mvc;
    private LocalDate today;
    private final String producer = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        authenticateAs(producer);
        today = LocalDate.now(clock.withZone(ProducerTimeZone.ZONE));
        when(subscriptionContextFacade.findEntitledQuotaHectares(anyString())).thenReturn(Optional.of(BigDecimal.TEN));
        when(weatherForecastProvider.fetchSevenDayForecast(anyDouble(), anyDouble()))
                .thenReturn(forecast(34.2, 35.1, 33.0, 30.4, 29.8, 28.0, 27.5));
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("without plotId: the field summary with three cards, stored once in its own table and served again")
    void fieldSummary() throws Exception {
        var plotA = createPlot();
        raiseHeat(plotA);
        stubOpenPlan(plotA);
        createPlot();

        String id = summaryId(summary(null, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("home.daily_summary"))
                .andExpect(jsonPath("$.plotId").value(nullValue()))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"))
                .andExpect(jsonPath("$.dataVersion", startsWith("sha256:")))
                .andExpect(jsonPath("$.text", startsWith("En 1 de tus 2 lotes la ventana de aclareo está abierta y "
                        + "cierra el ")))
                .andExpect(jsonPath("$.facts", hasSize(3)))
                .andExpect(jsonPath("$.facts[0].value").value("12"))
                .andExpect(jsonPath("$.facts[0].label").value("días de ventana"))
                .andExpect(jsonPath("$.facts[0].pill", startsWith("cierra el ")))
                .andExpect(jsonPath("$.facts[0].tone").value("PRIMARY"))
                .andExpect(jsonPath("$.facts[1].value").value("35,1 °C"))
                .andExpect(jsonPath("$.facts[1].label").value("máxima de la semana"))
                .andExpect(jsonPath("$.facts[2].value").value("1"))
                .andExpect(jsonPath("$.facts[2].pill").value("en 1 lote"))
                .andExpect(jsonPath("$.sources[0].label").value("Basado en tus planes de aclareo de esta campaña"))
                .andExpect(jsonPath("$.model.provider").value("TEMPLATE"))
                .andExpect(jsonPath("$.model.fallback").value(false)));

        var row = jdbc.queryForMap("select producer_id, plot_id, language, provider, fallback from "
                + "assistant_daily_summaries where id = ?", UUID.fromString(id));
        assertThat(row).containsEntry("PRODUCER_ID", producer).containsEntry("PLOT_ID", null)
                .containsEntry("LANGUAGE", "es").containsEntry("PROVIDER", "TEMPLATE").containsEntry("FALLBACK", false);

        // The same day, figures and language: the stored summary, nothing new written
        summary(null, "es").andExpect(status().isOk()).andExpect(jsonPath("$.summaryId").value(id));
        assertThat(rows()).isEqualTo(1);

        // Another language is its own summary
        summary(null, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaryId").value(not(id)))
                .andExpect(jsonPath("$.text", startsWith("On 1 of your 2 plots the thinning window is open")))
                .andExpect(jsonPath("$.facts[0].label").value("days of window left"))
                .andExpect(jsonPath("$.facts[0].pill", startsWith("closes ")))
                .andExpect(jsonPath("$.facts[1].label").value("week maximum"));
        assertThat(rows()).isEqualTo(2);

        // Summaries are not answers: neither the history nor the latest answer of HOME shows them
        mvc.perform(get(ANSWERS)).andExpect(status().isOk()).andExpect(jsonPath("$.answers", hasSize(0)));
        mvc.perform(get(ANSWERS + "/latest").param("screen", "HOME")).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from assistant_answers where producer_id = ?", Integer.class,
                producer)).isZero();
    }

    @Test
    @DisplayName("new figures write a new summary instead of serving the old one")
    void writtenAgainWhenTheFiguresChange() throws Exception {
        var plotA = createPlot();
        var plotB = createPlot();
        String first = summaryId(summary(null, "es").andExpect(status().isOk())
                .andExpect(jsonPath("$.facts[2].value").value("0")));

        raiseHeat(plotB);

        String second = summaryId(summary(null, "es").andExpect(status().isOk())
                .andExpect(jsonPath("$.facts[2].value").value("1"))
                .andExpect(jsonPath("$.facts[0].value").value("Sin ventana"))
                .andExpect(jsonPath("$.facts[0].pill").value("próximos 7 días")));
        assertThat(second).isNotEqualTo(first);
        assertThat(rows()).isEqualTo(2);
        assertThat(plotA).isNotEqualTo(plotB);
    }

    @Test
    @DisplayName("with plotId: the summary of that plot, stored with its plot and apart from the field one")
    void plotSummary() throws Exception {
        var plotA = createPlot();
        stubOpenPlan(plotA);
        createPlot();

        String plotSummary = summaryId(summary(plotA, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plotId").value(plotA))
                .andExpect(jsonPath("$.text", startsWith("Tu ventana de aclareo está abierta hasta el ")))
                .andExpect(jsonPath("$.facts", hasSize(3)))
                .andExpect(jsonPath("$.facts[0].value").value("12"))
                .andExpect(jsonPath("$.facts[2].pill").value("en este lote")));
        assertThat(jdbc.queryForObject("select plot_id from assistant_daily_summaries where id = ?", String.class,
                UUID.fromString(plotSummary))).isEqualTo(plotA);

        String fieldSummary = summaryId(summary(null, "es").andExpect(status().isOk())
                .andExpect(jsonPath("$.plotId").value(nullValue())));
        assertThat(fieldSummary).isNotEqualTo(plotSummary);
        summary(plotA, "es").andExpect(jsonPath("$.summaryId").value(plotSummary));
        assertThat(rows()).isEqualTo(2);
    }

    @Test
    @DisplayName("the daily job precomputes the field summary in Spanish; the next request serves it")
    void dailyJob() throws Exception {
        createPlot();

        var run = scheduler.precompute();

        assertThat(run.created()).isGreaterThanOrEqualTo(1);
        var stored = jdbc.queryForObject("select cast(id as varchar) from assistant_daily_summaries "
                + "where producer_id = ?", String.class, producer);
        assertThat(jdbc.queryForMap("select plot_id, language from assistant_daily_summaries where producer_id = ?",
                producer)).containsEntry("PLOT_ID", null).containsEntry("LANGUAGE", "es");
        summary(null, "es").andExpect(status().isOk()).andExpect(jsonPath("$.summaryId").value(stored));
        // Running it again the same day with the same figures stores nothing new
        scheduler.precompute();
        assertThat(rows()).isEqualTo(1);
    }

    @Test
    @DisplayName("errors as the other assistant routes: 400 malformed plot, 404 unknown, 403 foreign plot or no plan, "
            + "409 no plots; POST /answers does not take the summary; nothing stored")
    void errors() throws Exception {
        summary(null, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"));
        summary("not-a-uuid", "es")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        summary(UUID.randomUUID().toString(), "es").andExpect(status().isNotFound());

        authenticateAs(UUID.randomUUID().toString());
        var foreign = createPlot();
        authenticateAs(producer);
        summary(foreign, "es")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"home.daily_summary\",\"clientRequestId\":\"%s\"}"
                                .formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        createPlot();
        when(subscriptionContextFacade.findEntitledQuotaHectares(anyString())).thenReturn(Optional.empty());
        summary(null, "es")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ASSISTANT_FORBIDDEN"));
        assertThat(rows()).isZero();
    }

    @Test
    @DisplayName("deleting the account deletes its daily summaries")
    void accountDeletionRemovesTheSummaries() throws Exception {
        createPlot();
        summary(null, "es").andExpect(status().isOk());
        assertThat(rows()).isEqualTo(1);

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                events.publishEvent(new UserAccountDeletedEvent(producer, "producer@viora.test", Instant.now())));

        assertThat(rows()).isZero();
    }

    private int rows() {
        return jdbc.queryForObject("select count(*) from assistant_daily_summaries where producer_id = ?",
                Integer.class, producer);
    }

    private ResultActions summary(String plotId, String language) throws Exception {
        var request = get(SUMMARY).header(HttpHeaders.ACCEPT_LANGUAGE, language);
        return mvc.perform(plotId == null ? request : request.param("plotId", plotId));
    }

    private static String summaryId(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.summaryId");
    }

    private void stubOpenPlan(String plotId) {
        var plan = new ThinningPlanDetail(true, 30.0, 0.5714, 0.4, today.minusDays(10), today.plusDays(11), 5,
                today.minusDays(1), null, null, null, null, null);
        when(thinningContextFacade.findThinningPlanDetail(eq(plotId), anyInt())).thenReturn(Optional.of(plan));
        when(thinningContextFacade.findPrescriptionAvailability(eq(plotId), anyInt()))
                .thenReturn(Optional.of(new PrescriptionAvailability(plan.issued(), plan.percentageToRemove(),
                        plan.sampledTrees(), 0, plan.caliberProjectionStatus())));
    }

    private void raiseHeat(String plotId) {
        incidentCommandService.handle(new RaiseAgroclimaticIncidentCommand(plotId, "HEAT_WAVE", "WARNING",
                "AMBIENT_TEMPERATURE", 34.2, 32.0, "°C", List.of("heat_wave.step.pre_irrigation",
                "heat_wave.step.morning_irrigation", "heat_wave.step.suspend_cultural_ops")));
    }

    private List<WeatherForecastDay> forecast(double... maxima) {
        var days = new ArrayList<WeatherForecastDay>();
        for (int i = 0; i < maxima.length; i++) {
            days.add(WeatherForecastDay.create(today.plusDays(i), new AmbientTemperature(maxima[i]),
                    new AmbientTemperature(14.0), new Percentage(0.0), new WindSpeed(10.0), Instant.now()));
        }
        return days;
    }

    private static void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private String createPlot() throws Exception {
        var payload = """
                {"name":"Cuartel %s","variety":"SEVILLANA",
                 "polygonGeoJson":"{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                 "rowSpacingM":7.0,"treeSpacingM":5.0}
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        var response = mvc.perform(post("/api/v1/plots").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
