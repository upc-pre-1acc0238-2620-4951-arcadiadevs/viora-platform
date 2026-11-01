package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
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
 * Real controllers, services, Phenology and Telemetry facades and H2: the HOME screen (P110-P113) about one plot or,
 * without plotId, about the producer's whole field; every other screen still requires its plot. The thinning plan of the plot is stubbed at the Thinning facade, the forecast at its provider. The model is
 * disabled in tests, so every answer is its template.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistanthome;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant HOME screen")
class AssistantHomeScreenIntegrationTest {

    private static final String QUESTIONS = "/api/v1/assistant/questions";
    private static final String ANSWERS = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
    @Autowired AgroclimaticIncidentCommandService incidentCommandService;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
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
    @DisplayName("one plot: the catalog, the week and the campaign answered from its figures and saved for the plot")
    void onePlot() throws Exception {
        int year = today.getYear();
        var plotId = createPlot();
        recordHarvest(plotId, year - 2, 1000.0);
        recordHarvest(plotId, year - 1, 3000.0);
        raiseHeat(plotId);
        stubPlan(plotId, new ThinningPlanDetail(true, 30.0, 0.5714, 0.4, today.minusDays(10), today.plusDays(11), 5,
                today.minusDays(1), null, null, null, null, null), 0);

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("HOME"))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(jsonPath("$.questions[0].questionId").value("home.week_plan"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Qué hago esta semana?"))
                .andExpect(jsonPath("$.questions[0].icon").value("calendar"))
                .andExpect(jsonPath("$.questions[0].enabled").value(true))
                .andExpect(jsonPath("$.questions[1].questionId").value("home.campaign_vs_previous"))
                .andExpect(jsonPath("$.questions[1].label").value("¿Cómo va mi campaña frente a la anterior?"))
                .andExpect(jsonPath("$.questions[1].icon").value("history"))
                .andExpect(jsonPath("$.questions[1].enabled").value(true))
                .andExpect(jsonPath("$.questions[2].questionId").value("home.alerts_affecting"))
                .andExpect(jsonPath("$.questions[2].label").value("¿Qué alertas afectan a este lote?"))
                .andExpect(jsonPath("$.questions[2].icon").value("alert"))
                .andExpect(jsonPath("$.questions[2].enabled").value(true))
                .andExpect(jsonPath("$.questions[3].questionId").value("home.harvest_estimate"))
                .andExpect(jsonPath("$.questions[3].label").value("¿Cuánto cosecharé este año?"))
                .andExpect(jsonPath("$.questions[3].icon").value("harvest"))
                .andExpect(jsonPath("$.questions[3].enabled").value(false))
                .andExpect(jsonPath("$.questions[3].disabledReason")
                        .value("Faltan datos de calibre · registra un muestreo"));
        catalog(plotId, "en")
                .andExpect(jsonPath("$.questions[0].label").value("What do I do this week?"))
                .andExpect(jsonPath("$.questions[2].label").value("Which alerts affect this plot?"));

        String weekId = JsonPath.read(answer("home.week_plan", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("home.week_plan"))
                .andExpect(jsonPath("$.question").value("¿Qué hago esta semana?"))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.text", startsWith("Tu ventana de aclareo está abierta hasta el ")))
                .andExpect(jsonPath("$.text", containsString(" y el plan indica quitar el 30 % de los frutos. La "
                        + "máxima de la semana, 35,1 °C el ")))
                .andExpect(jsonPath("$.text", endsWith(", alcanza el umbral de calor de 32 °C. Tienes 1 alerta activa, "
                        + "la más grave de golpe de calor en nivel aviso.")))
                .andExpect(jsonPath("$.facts", hasSize(2)))
                .andExpect(jsonPath("$.facts[0].value").value("35,1 °C"))
                .andExpect(jsonPath("$.facts[0].label").value("máxima de la semana"))
                .andExpect(jsonPath("$.facts[1].value").value("1"))
                .andExpect(jsonPath("$.facts[1].pill").value("en este lote"))
                .andExpect(jsonPath("$.sources[0].label").value("Basado en el plan de aclareo de esta campaña"))
                .andExpect(jsonPath("$.actions", hasSize(0)))
                .andExpect(jsonPath("$.model.provider").value("TEMPLATE"))
                .andReturn().getResponse().getContentAsString(), "$.answerId");
        assertThat(jdbc.queryForObject("select plot_id from assistant_answers where id = ?", String.class,
                UUID.fromString(weekId))).isEqualTo(plotId);

        answer("home.campaign_vs_previous", plotId, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("How is my campaign going against the previous one?"))
                .andExpect(jsonPath("$.text", startsWith("Your %d campaign closed with 3,000 kg, 200 %% more than the "
                        .formatted(year - 1) + "%d one (1,000 kg). Your alternate bearing index is 0.50: "
                        .formatted(year - 2))))
                .andExpect(jsonPath("$.facts[0].value").value("3,000 kg"))
                .andExpect(jsonPath("$.facts[1].value").value("+200%"))
                .andExpect(jsonPath("$.facts[1].pill").value("2 campaigns"))
                .andExpect(jsonPath("$.sources[0].label").value("Based on 2 recorded campaigns"));
        answer("home.harvest_estimate", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"));

        mvc.perform(get(ANSWERS + "/latest").param("screen", "HOME").param("plotId", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("home.campaign_vs_previous"))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"));
    }

    @Test
    @DisplayName("another producer's plot is forbidden and an unknown one not found")
    void requiresOwnPlot() throws Exception {
        authenticateAs(UUID.randomUUID().toString());
        var foreign = createPlot();
        authenticateAs(producer);
        catalog(foreign, "es").andExpect(status().isForbidden());
        answer("home.alerts_affecting", foreign, "es").andExpect(status().isForbidden());
        catalog(UUID.randomUUID().toString(), "es").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("without plotId: the producer's whole field, saved without plot and kept apart from plot answers")
    void wholeField() throws Exception {
        int year = today.getYear();
        var plotA = createPlot();
        recordHarvest(plotA, year - 2, 1000.0);
        recordHarvest(plotA, year - 1, 3000.0);
        raiseHeat(plotA);
        stubPlan(plotA, new ThinningPlanDetail(true, 30.0, 0.5714, 0.4, today.minusDays(10), today.plusDays(11), 5,
                today.minusDays(1), null, null, null, null, null), 0);
        var plotB = createPlot();
        recordHarvest(plotB, year - 2, 2000.0);
        recordHarvest(plotB, year - 1, 2000.0);

        // Another producer, with an alert on its own plot and its own answer about its field
        var other = UUID.randomUUID().toString();
        authenticateAs(other);
        raiseHeat(createPlot());
        String otherFieldId = answerId(fieldAnswer("home.alerts_affecting", "es", UUID.randomUUID())
                .andExpect(status().isOk()));
        authenticateAs(producer);

        fieldCatalog("es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("HOME"))
                .andExpect(jsonPath("$.plotId").value(nullValue()))
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(jsonPath("$.questions[0].enabled").value(true))
                .andExpect(jsonPath("$.questions[1].enabled").value(true))
                .andExpect(jsonPath("$.questions[2].label").value("¿Qué alertas afectan a mis lotes?"))
                .andExpect(jsonPath("$.questions[2].enabled").value(true))
                .andExpect(jsonPath("$.questions[3].enabled").value(false));
        fieldCatalog("en").andExpect(jsonPath("$.questions[2].label").value("Which alerts affect my plots?"));

        // Neither the other producer's field answer nor this producer's plot answers are its field's latest
        latest(null).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ANSWER_NOT_FOUND"));
        String plotAlertsId = answerId(answer("home.alerts_affecting", plotA, "es").andExpect(status().isOk()));
        latest(null).andExpect(status().isNotFound());

        String fieldAlertsId = answerId(fieldAnswer("home.alerts_affecting", "es", UUID.randomUUID())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("home.alerts_affecting"))
                .andExpect(jsonPath("$.question").value("¿Qué alertas afectan a mis lotes?"))
                .andExpect(jsonPath("$.plotId").value(nullValue()))
                .andExpect(jsonPath("$.text").value("Tienes 1 alerta activa en 1 de tus 2 lotes y la más grave es de "
                        + "golpe de calor en nivel aviso: 34,2 °C frente al umbral de 32 °C. Ábrela para seguir sus "
                        + "pasos de mitigación."))
                .andExpect(jsonPath("$.facts", hasSize(2)))
                .andExpect(jsonPath("$.facts[1].value").value("1"))
                .andExpect(jsonPath("$.facts[1].pill").value("en 1 lote"))
                .andExpect(jsonPath("$.sources[0].label").value("Basado en las alertas abiertas de tus lotes"))
                .andExpect(jsonPath("$.model.provider").value("TEMPLATE")));
        assertThat(jdbc.queryForMap("select plot_id, producer_id from assistant_answers where id = ?",
                UUID.fromString(fieldAlertsId))).containsEntry("PLOT_ID", null).containsEntry("PRODUCER_ID", producer);
        assertThat(jdbc.queryForObject("select plot_id from assistant_answers where id = ?", String.class,
                UUID.fromString(plotAlertsId))).isEqualTo(plotA);

        fieldAnswer("home.campaign_vs_previous", "en", UUID.randomUUID())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(("On the 2 plots with both harvests, the %d campaign closed with "
                        + "5,000 kg, 67 %% more than the %d one (3,000 kg). All your active plots are included.")
                        .formatted(year - 1, year - 2)))
                .andExpect(jsonPath("$.facts[1].pill").value("2 plots compared"))
                .andExpect(jsonPath("$.sources[0].label").value("Based on the recorded harvests of 2 plots"));
        String fieldWeekId = answerId(fieldAnswer("home.week_plan", "es", UUID.randomUUID())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", startsWith("En 1 de tus 2 lotes la ventana de aclareo está abierta y "
                        + "cierra el ")))
                .andExpect(jsonPath("$.sources[0].label").value("Basado en tus planes de aclareo de esta campaña")));
        fieldAnswer("home.harvest_estimate", "es", UUID.randomUUID())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"));

        // The same field question with the same data reuses the field answer; the plot one is its own answer
        fieldAnswer("home.week_plan", "es", UUID.randomUUID())
                .andExpect(status().isOk()).andExpect(jsonPath("$.answerId").value(fieldWeekId));
        String plotWeekId = answerId(answer("home.week_plan", plotA, "es").andExpect(status().isOk()));
        assertThat(plotWeekId).isNotEqualTo(fieldWeekId);

        latest(null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(fieldWeekId))
                .andExpect(jsonPath("$.plotId").value(nullValue()))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"));
        latest(plotA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(plotWeekId))
                .andExpect(jsonPath("$.plotId").value(plotA));
        mvc.perform(get(ANSWERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers", hasSize(5)))
                .andExpect(jsonPath("$.answers[0].answerId").value(plotWeekId))
                .andExpect(jsonPath("$.answers[1].answerId").value(fieldWeekId))
                .andExpect(jsonPath("$.answers[1].plotId").value(nullValue()))
                .andExpect(jsonPath("$.answers[1].freshness").value("UP_TO_DATE"));

        // A new alert on the other plot changes the figures of the field, not those of plot A
        raiseHeat(plotB);
        getAnswer(fieldAlertsId).andExpect(status().isOk()).andExpect(jsonPath("$.freshness").value("STALE"));
        getAnswer(plotAlertsId).andExpect(status().isOk()).andExpect(jsonPath("$.freshness").value("UP_TO_DATE"));

        authenticateAs(other);
        latest(null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(otherFieldId))
                .andExpect(jsonPath("$.text", startsWith("Tu lote tiene 1 alerta activa")));
        getAnswer(fieldWeekId).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a producer without active plots: the week is disabled and the alerts say there are none")
    void fieldWithoutPlots() throws Exception {
        fieldCatalog("es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].enabled").value(false))
                .andExpect(jsonPath("$.questions[0].disabledReason").value("Sin lotes activos · registra un lote"))
                .andExpect(jsonPath("$.questions[2].enabled").value(true));
        fieldAnswer("home.week_plan", "es", UUID.randomUUID())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"));
        fieldAnswer("home.alerts_affecting", "en", UUID.randomUUID())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Your plots have no open heat, frost or water stress alerts. Check "
                        + "the forecast of the week on the climate screen."))
                .andExpect(jsonPath("$.facts[1].value").value("0"))
                .andExpect(jsonPath("$.facts[1].label").value("active plots"));
    }

    @Test
    @DisplayName("every screen but HOME still answers 400 without plotId: catalog, ask and latest")
    void otherScreensRequireTheirPlot() throws Exception {
        var firstQuestion = Map.of(
                "ALTERNATION", "alternation.why_index", "PLAN", "plan.why_percent", "ALERT", "alert.what_to_do",
                "CLIMATE", "climate.irrigate", "WINTER_CHILL", "chill.missing", "PLOT_DETAIL", "plot.how_is",
                "SENSORS", "sensors.what_say", "SAMPLING_SUMMARY", "sampling.enough_trees",
                "THINNING_DONE", "thinning.harvest_change", "CAMPAIGN_CLOSED", "campaign.vs_previous");
        assertThat(firstQuestion.keySet()).containsExactlyInAnyOrderElementsOf(
                Arrays.stream(ScreenKey.values()).filter(screen -> screen != ScreenKey.HOME)
                        .map(Enum::name).toList());
        for (var entry : firstQuestion.entrySet()) {
            mvc.perform(get(QUESTIONS).param("screen", entry.getKey()).header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.detail").value("Indica el lote de esta pantalla"));
            mvc.perform(get(ANSWERS + "/latest").param("screen", entry.getKey())
                            .header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.detail").value("Indica el lote de esta pantalla"));
            fieldAnswer(entry.getValue(), "en", UUID.randomUUID())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.detail").value("Indicate the plot of this screen"));
        }
        assertThat(jdbc.queryForObject("select count(*) from assistant_answers where producer_id = ?", Integer.class,
                producer)).isZero();
    }

    private ResultActions fieldCatalog(String language) throws Exception {
        return mvc.perform(get(QUESTIONS).param("screen", "HOME").header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private ResultActions fieldAnswer(String questionId, String language, UUID clientRequestId) throws Exception {
        var body = "{\"questionId\":\"%s\",\"clientRequestId\":\"%s\",\"forceRefresh\":false}"
                .formatted(questionId, clientRequestId);
        return mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private ResultActions latest(String plotId) throws Exception {
        var request = get(ANSWERS + "/latest").param("screen", "HOME");
        return mvc.perform(plotId == null ? request : request.param("plotId", plotId));
    }

    private ResultActions getAnswer(String answerId) throws Exception {
        return mvc.perform(get(ANSWERS + "/" + answerId));
    }

    private static String answerId(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.answerId");
    }

    private void stubPlan(String plotId, ThinningPlanDetail plan, int treesNeeded) {
        when(thinningContextFacade.findThinningPlanDetail(eq(plotId), anyInt())).thenReturn(Optional.of(plan));
        when(thinningContextFacade.findPrescriptionAvailability(eq(plotId), anyInt()))
                .thenReturn(Optional.of(new PrescriptionAvailability(plan.issued(), plan.percentageToRemove(),
                        plan.sampledTrees(), treesNeeded, plan.caliberProjectionStatus())));
    }

    private ResultActions catalog(String plotId, String language) throws Exception {
        return mvc.perform(get(QUESTIONS).param("screen", "HOME").param("plotId", plotId)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private ResultActions answer(String questionId, String plotId, String language) throws Exception {
        var body = "{\"questionId\":\"%s\",\"plotId\":\"%s\",\"clientRequestId\":\"%s\",\"forceRefresh\":false}"
                .formatted(questionId, plotId, UUID.randomUUID());
        return mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private void raiseHeat(String plotId) {
        incidentCommandService.handle(new RaiseAgroclimaticIncidentCommand(plotId, "HEAT_WAVE", "WARNING",
                "AMBIENT_TEMPERATURE", 34.2, 32.0, "°C", List.of("heat_wave.step.pre_irrigation",
                "heat_wave.step.morning_irrigation", "heat_wave.step.suspend_cultural_ops")));
    }

    private void recordHarvest(String plotId, int campaignYear, double totalKg) throws Exception {
        mvc.perform(post("/api/v1/plots/" + plotId + "/harvest-records").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campaignYear\":%d,\"totalYieldKg\":%s}".formatted(campaignYear, totalKg)))
                .andExpect(status().isCreated());
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
