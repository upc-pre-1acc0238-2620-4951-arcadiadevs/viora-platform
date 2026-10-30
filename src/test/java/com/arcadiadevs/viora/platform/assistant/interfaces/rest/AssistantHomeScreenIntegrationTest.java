package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import com.arcadiadevs.viora.platform.telemetry.application.commandservices.AgroclimaticIncidentCommandService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.WeatherForecastProvider;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastDay;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RaiseAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.AmbientTemperature;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.Percentage;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.WindSpeed;
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
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real controllers, services, facades and H2: the HOME screen (P110-P113), about one plot or, without plotId, the
 * producer's whole field. Every other screen still requires its plot.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistanthome;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant HOME screen (plot or whole field)")
class AssistantHomeScreenIntegrationTest {

    private static final String QUESTIONS = "/api/v1/assistant/questions";
    private static final String ANSWERS = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
    @Autowired AgroclimaticIncidentCommandService incidentCommandService;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    @MockitoBean WeatherForecastProvider weatherForecastProvider;
    private MockMvc mvc;
    private final String producer = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        authenticateAs(producer);
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
    @DisplayName("Without plotId HOME answers for every active plot of the producer, stored and cached without plot")
    void wholeField() throws Exception {
        int year = Year.now(clock.withZone(ProducerTimeZone.ZONE)).getValue();
        var withBoth = createPlot();
        recordHarvest(withBoth, year - 2, 1000.0);
        recordHarvest(withBoth, year - 1, 3000.0);
        var withLast = createPlot();
        recordHarvest(withLast, year - 1, 2000.0);
        raiseHeat(withBoth);
        // Another producer's plot and alert are never read
        authenticateAs(UUID.randomUUID().toString());
        var foreign = createPlot();
        raiseHeat(foreign);
        authenticateAs(producer);

        catalog(null, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("HOME"))
                .andExpect(jsonPath("$.plotId").value(nullValue()))
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(jsonPath("$.questions[0].questionId").value("home.week_plan"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Qué hago esta semana?"))
                .andExpect(jsonPath("$.questions[0].icon").value("calendar"))
                .andExpect(jsonPath("$.questions[0].enabled").value(true))
                .andExpect(jsonPath("$.questions[1].questionId").value("home.campaign_vs_previous"))
                .andExpect(jsonPath("$.questions[1].icon").value("history"))
                .andExpect(jsonPath("$.questions[1].enabled").value(true))
                .andExpect(jsonPath("$.questions[2].questionId").value("home.alerts_affecting"))
                .andExpect(jsonPath("$.questions[2].label").value("¿Qué alertas afectan a mis lotes?"))
                .andExpect(jsonPath("$.questions[2].icon").value("alert"))
                .andExpect(jsonPath("$.questions[2].enabled").value(true))
                .andExpect(jsonPath("$.questions[3].questionId").value("home.harvest_estimate"))
                .andExpect(jsonPath("$.questions[3].icon").value("harvest"))
                .andExpect(jsonPath("$.questions[3].enabled").value(false))
                .andExpect(jsonPath("$.questions[3].disabledReason")
                        .value("Faltan datos de calibre · registra un muestreo"));

        var key = UUID.randomUUID().toString();
        var created = answer("home.alerts_affecting", null, key, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plotId").value(nullValue()))
                .andExpect(jsonPath("$.text").value("Tienes 1 alerta activa en 1 de tus 2 lotes y la más grave es "
                        + "de golpe de calor en nivel aviso: 34,2 °C frente al umbral de 32 °C. Ábrela para seguir "
                        + "sus pasos de mitigación."))
                .andExpect(jsonPath("$.facts[0].value").value("34,2 °C"))
                .andExpect(jsonPath("$.facts[1].value").value("1"))
                .andExpect(jsonPath("$.facts[1].pill").value("en 1 lote"))
                .andExpect(jsonPath("$.sources[0].label").value("Basado en las alertas abiertas de tus lotes"))
                .andReturn().getResponse().getContentAsString();
        String answerId = JsonPath.read(created, "$.answerId");
        assertThat(jdbc.queryForObject("select plot_id from assistant_answers where id = ?", String.class,
                UUID.fromString(answerId))).isNull();
        assertThat(jdbc.queryForObject("select producer_id from assistant_answers where id = ?", String.class,
                UUID.fromString(answerId))).isEqualTo(producer);

        // Same key: the same answer; another key with the same facts: served from the cache
        answer("home.alerts_affecting", null, key, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(answerId));
        answer("home.alerts_affecting", null, UUID.randomUUID().toString(), "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(answerId));
        // The key of a field answer cannot be reused for a plot
        answer("home.alerts_affecting", withBoth, key, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_REQUEST_CONFLICT"));

        answer("home.week_plan", null, UUID.randomUUID().toString(), "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plotId").value(nullValue()))
                .andExpect(jsonPath("$.text", startsWith("Esta semana ninguno de tus 2 lotes tiene ventana de "
                        + "aclareo abierta ni muestreo a medias. La máxima de la semana, 35,1 °C el ")))
                .andExpect(jsonPath("$.text", endsWith(", alcanza el umbral de calor de 32 °C. Tienes 1 alerta "
                        + "activa en 1 lote, la más grave de golpe de calor en nivel aviso.")))
                .andExpect(jsonPath("$.facts[0].value").value("35,1 °C"))
                .andExpect(jsonPath("$.facts[1].value").value("1"));
        answer("home.campaign_vs_previous", null, UUID.randomUUID().toString(), "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value(("En el lote con las dos cosechas, la campaña %d cerró con "
                        + "3.000 kg, un 200 %% más que la de %d (1.000 kg). Queda fuera 1 lote sin las dos cosechas.")
                        .formatted(year - 1, year - 2)))
                .andExpect(jsonPath("$.facts[0].value").value("3.000 kg"))
                .andExpect(jsonPath("$.facts[1].value").value("+200 %"))
                .andExpect(jsonPath("$.facts[1].pill").value("1 lote comparado"));
        var latestField = answer("home.campaign_vs_previous", null, UUID.randomUUID().toString(), "en")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text", startsWith("On the plot with both harvests, the %d campaign closed "
                        .formatted(year - 1))))
                .andReturn().getResponse().getContentAsString();
        answer("home.harvest_estimate", null, UUID.randomUUID().toString(), "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Faltan datos de calibre · registra un muestreo"));

        // The entry opens on the latest field answer; a plot of HOME has none yet
        mvc.perform(get(ANSWERS + "/latest").param("screen", "HOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value((String) JsonPath.read(latestField, "$.answerId")))
                .andExpect(jsonPath("$.plotId").value(nullValue()));
        mvc.perform(get(ANSWERS + "/latest").param("screen", "HOME").param("plotId", withBoth))
                .andExpect(status().isNoContent());
        mvc.perform(get(ANSWERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].plotId").value(nullValue()))
                .andExpect(jsonPath("$[3].answerId").value(answerId))
                .andExpect(jsonPath("$[3].freshness").value("UP_TO_DATE"));
        mvc.perform(get(ANSWERS + "/" + answerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plotId").value(nullValue()))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"));
    }

    @Test
    @DisplayName("With plotId HOME answers for that plot only, apart from the field answers")
    void onePlot() throws Exception {
        int year = Year.now(clock.withZone(ProducerTimeZone.ZONE)).getValue();
        var plotId = createPlot();
        recordHarvest(plotId, year - 2, 1000.0);
        recordHarvest(plotId, year - 1, 3000.0);
        createPlot();
        raiseHeat(plotId);
        var fieldAnswer = JsonPath.<String>read(answer("home.alerts_affecting", null, UUID.randomUUID().toString(),
                "es").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.answerId");

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.questions[2].label").value("¿Qué alertas afectan a este lote?"))
                .andExpect(jsonPath("$.questions[3].enabled").value(false));
        var plotAnswer = answer("home.alerts_affecting", plotId, UUID.randomUUID().toString(), "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.text").value("Tu lote tiene 1 alerta activa y la más grave es de golpe de "
                        + "calor en nivel aviso: 34,2 °C frente al umbral de 32 °C. Ábrela para seguir sus pasos de "
                        + "mitigación."))
                .andExpect(jsonPath("$.sources[0].label", startsWith("Basado en la alerta del ")))
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(plotAnswer, "$.answerId")).isNotEqualTo(fieldAnswer);
        answer("home.campaign_vs_previous", plotId, UUID.randomUUID().toString(), "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text", startsWith(("Tu campaña %d cerró con 3.000 kg, un 200 %% más que la "
                        + "de %d (1.000 kg). Tu índice de vecería es 0,50: ").formatted(year - 1, year - 2))))
                .andExpect(jsonPath("$.facts[1].pill").value("2 campañas"));
        answer("home.week_plan", plotId, UUID.randomUUID().toString(), "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text", startsWith("Esta semana tu lote no tiene ventana de aclareo abierta ni "
                        + "muestreo a medias. ")))
                .andExpect(jsonPath("$.text", endsWith(" Tienes 1 alerta activa, la más grave de golpe de calor en "
                        + "nivel aviso.")))
                .andExpect(jsonPath("$.facts[1].pill").value("en este lote"));

        mvc.perform(get(ANSWERS + "/latest").param("screen", "HOME").param("plotId", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.questionId").value("home.week_plan"));
        mvc.perform(get(ANSWERS + "/latest").param("screen", "HOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(fieldAnswer));

        // Unknown plot: 404; a plot of another producer: 403
        catalog(UUID.randomUUID().toString(), "es").andExpect(status().isNotFound());
        authenticateAs(UUID.randomUUID().toString());
        var foreign = createPlot();
        authenticateAs(producer);
        catalog(foreign, "es").andExpect(status().isForbidden());
        answer("home.alerts_affecting", foreign, UUID.randomUUID().toString(), "es")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A producer without active plots: the week plan is disabled, the alerts answer says there are none")
    void noActivePlots() throws Exception {
        catalog(null, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].enabled").value(false))
                .andExpect(jsonPath("$.questions[0].disabledReason").value("Sin lotes activos · registra un lote"))
                .andExpect(jsonPath("$.questions[1].disabledReason").value("Registra al menos 2 campañas"))
                .andExpect(jsonPath("$.questions[2].enabled").value(true));
        answer("home.week_plan", null, UUID.randomUUID().toString(), "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Sin lotes activos · registra un lote"));
        answer("home.alerts_affecting", null, UUID.randomUUID().toString(), "en")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Your plots have no open heat, frost or water stress alerts. "
                        + "Check the forecast of the week on the climate screen."))
                .andExpect(jsonPath("$.facts[0].value").value("0"))
                .andExpect(jsonPath("$.facts[1].label").value("active plots"));
        mvc.perform(get(ANSWERS + "/latest").param("screen", "ALTERNATION").param("plotId", createPlot()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Every other screen still answers 400 assistant.plot.required without plotId")
    void plotScreensStillRequireTheirPlot() throws Exception {
        var plotId = createPlot();
        for (var questionId : List.of("alternation.why_index", "plan.why_percent", "alert.what_to_do",
                "climate.heat_ends")) {
            answer(questionId, null, UUID.randomUUID().toString(), "es")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.detail").value("Indica el lote de esta pantalla"));
        }
        for (var screen : List.of("ALTERNATION", "PLAN", "ALERT", "CLIMATE")) {
            mvc.perform(get(QUESTIONS).param("screen", screen).header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Indica el lote de esta pantalla"));
            mvc.perform(get(ANSWERS + "/latest").param("screen", screen).header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Indica el lote de esta pantalla"));
        }
        // With their plot they keep working as before
        mvc.perform(get(QUESTIONS).param("screen", "PLAN").param("plotId", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plotId").value(plotId));
        assertThat(jdbc.queryForObject("select count(*) from assistant_answers where producer_id = ?",
                Integer.class, producer)).isZero();
    }

    private ResultActions catalog(String plotId, String language) throws Exception {
        var request = get(QUESTIONS).param("screen", "HOME").header(HttpHeaders.ACCEPT_LANGUAGE, language);
        return mvc.perform(plotId == null ? request : request.param("plotId", plotId));
    }

    private ResultActions answer(String questionId, String plotId, String clientRequestId, String language)
            throws Exception {
        var body = plotId == null
                ? "{\"questionId\":\"%s\",\"clientRequestId\":\"%s\"}".formatted(questionId, clientRequestId)
                : "{\"questionId\":\"%s\",\"plotId\":\"%s\",\"clientRequestId\":\"%s\"}"
                        .formatted(questionId, plotId, clientRequestId);
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

    private static List<WeatherForecastDay> forecast(double... maxima) {
        var today = LocalDate.now(ProducerTimeZone.ZONE);
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
