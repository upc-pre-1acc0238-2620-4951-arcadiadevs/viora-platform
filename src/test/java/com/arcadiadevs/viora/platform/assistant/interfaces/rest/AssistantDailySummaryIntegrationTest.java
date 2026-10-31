package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.assistant.infrastructure.scheduling.AssistantDailySummaryScheduler;
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
import java.time.Instant;
import java.time.LocalDate;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real controllers, services, facades and H2: the daily summary of Inicio (contract section 4.4), for the whole field
 * or one plot, precomputed by the daily job and generated on demand when missing or stale.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantdailysummary;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant daily summary (GET /daily-summary and the daily job)")
class AssistantDailySummaryIntegrationTest {

    private static final String SUMMARY = "/api/v1/assistant/daily-summary";
    private static final String ANSWERS = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
    @Autowired AgroclimaticIncidentCommandService incidentCommandService;
    @Autowired AssistantDailySummaryScheduler scheduler;
    @Autowired JdbcTemplate jdbc;
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
    @DisplayName("Without plotId: the field summary with three facts, stored once and served again while current")
    void fieldSummary() throws Exception {
        var withAlert = createPlot();
        createPlot();
        raiseHeat(withAlert);

        var created = summary(null, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("home.daily_summary"))
                .andExpect(jsonPath("$.plotId").value(nullValue()))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"))
                .andExpect(jsonPath("$.dataVersion", startsWith("sha256:")))
                .andExpect(jsonPath("$.text", startsWith("Esta semana ninguno de tus 2 lotes tiene ventana de "
                        + "aclareo abierta ni muestreo a medias. La máxima de la semana, 35,1 °C el ")))
                .andExpect(jsonPath("$.text", endsWith(", alcanza el umbral de calor de 32 °C. Tienes 1 alerta "
                        + "activa en 1 lote, la más grave de golpe de calor en nivel aviso.")))
                .andExpect(jsonPath("$.facts.length()").value(3))
                .andExpect(jsonPath("$.facts[0].value").value("Sin ventana"))
                .andExpect(jsonPath("$.facts[0].label").value("ventana de aclareo"))
                .andExpect(jsonPath("$.facts[0].pill").value("próximos 7 días"))
                .andExpect(jsonPath("$.facts[0].tone").value("PRIMARY"))
                .andExpect(jsonPath("$.facts[1].value").value("35,1 °C"))
                .andExpect(jsonPath("$.facts[1].label").value("máxima de la semana"))
                .andExpect(jsonPath("$.facts[1].tone").value("PRIMARY"))
                .andExpect(jsonPath("$.facts[2].value").value("1"))
                .andExpect(jsonPath("$.facts[2].label").value("alerta activa"))
                .andExpect(jsonPath("$.facts[2].pill").value("en 1 lote"))
                .andExpect(jsonPath("$.facts[2].tone").value("HIGHLIGHT"))
                .andExpect(jsonPath("$.sources.length()").value(2))
                .andExpect(jsonPath("$.actions.length()").value(0))
                .andExpect(jsonPath("$.model.provider").value("template"))
                .andExpect(jsonPath("$.model.fallback").value(true))
                .andReturn().getResponse().getContentAsString();
        String answerId = JsonPath.read(created, "$.answerId");
        assertThat(rows()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select question_id from assistant_answers where id = ?", String.class,
                UUID.fromString(answerId))).isEqualTo("home.daily_summary");
        assertThat(jdbc.queryForObject("select plot_id from assistant_answers where id = ?", String.class,
                UUID.fromString(answerId))).isNull();
        assertThat(jdbc.queryForObject("select locale from assistant_answers where id = ?", String.class,
                UUID.fromString(answerId))).isEqualTo("es");

        // Same data: the stored summary, not a new one
        summary(null, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(answerId))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"));
        assertThat(rows()).isEqualTo(1);
        // English is another summary
        summary(null, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facts[0].value").value("No window"))
                .andExpect(jsonPath("$.facts[0].label").value("thinning window"))
                .andExpect(jsonPath("$.facts[0].pill").value("next 7 days"))
                .andExpect(jsonPath("$.facts[1].label").value("week maximum"));
        assertThat(rows()).isEqualTo(2);

        // The summary is not a question the producer asked: not in Recientes nor where the HOME entry opens
        mvc.perform(get(ANSWERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(ANSWERS + "/latest").param("screen", "HOME"))
                .andExpect(status().isNoContent());
        mvc.perform(get(ANSWERS + "/" + answerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("home.daily_summary"))
                .andExpect(jsonPath("$.facts.length()").value(3))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"));
        // A question asked on HOME is still listed and opened as before
        var asked = answer("home.week_plan", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.facts.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get(ANSWERS))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].questionId").value("home.week_plan"));
        mvc.perform(get(ANSWERS + "/latest").param("screen", "HOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value((String) JsonPath.read(asked, "$.answerId")));
    }

    @Test
    @DisplayName("A stored summary whose data changed is regenerated; the old one reads as stale")
    void regeneratedWhenStale() throws Exception {
        var first = createPlot();
        var second = createPlot();
        raiseHeat(first);
        String old = JsonPath.read(summary(null, "es").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.answerId");

        raiseHeat(second);

        var fresh = summary(null, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"))
                .andExpect(jsonPath("$.facts[2].value").value("2"))
                .andExpect(jsonPath("$.facts[2].pill").value("en 2 lotes"))
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(fresh, "$.answerId")).isNotEqualTo(old);
        assertThat(rows()).isEqualTo(2);
        mvc.perform(get(ANSWERS + "/" + old))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freshness").value("STALE"));
    }

    @Test
    @DisplayName("With plotId: the summary of that plot, stored apart from the field one")
    void plotSummary() throws Exception {
        var plotId = createPlot();
        createPlot();
        raiseHeat(plotId);
        String field = JsonPath.read(summary(null, "es").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.answerId");

        var plot = summary(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("home.daily_summary"))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.text", startsWith("Esta semana tu lote no tiene ventana de aclareo abierta "
                        + "ni muestreo a medias. ")))
                .andExpect(jsonPath("$.facts.length()").value(3))
                .andExpect(jsonPath("$.facts[2].pill").value("en este lote"))
                .andReturn().getResponse().getContentAsString();
        String plotAnswer = JsonPath.read(plot, "$.answerId");
        assertThat(plotAnswer).isNotEqualTo(field);
        assertThat(jdbc.queryForObject("select plot_id from assistant_answers where id = ?", String.class,
                UUID.fromString(plotAnswer))).isEqualTo(plotId);
        summary(plotId, "es").andExpect(jsonPath("$.answerId").value(plotAnswer));
        summary(null, "es").andExpect(jsonPath("$.answerId").value(field));
        assertThat(rows()).isEqualTo(2);
    }

    @Test
    @DisplayName("The daily job precomputes the field summary in Spanish; the next GET serves it")
    void dailyJob() throws Exception {
        createPlot();
        var run = scheduler.precompute();

        assertThat(run.created()).isGreaterThanOrEqualTo(1);
        var stored = jdbc.queryForObject("select cast(id as varchar) from assistant_answers where producer_id = ? "
                + "and question_id = 'home.daily_summary'", String.class, producer);
        assertThat(jdbc.queryForObject("select plot_id from assistant_answers where producer_id = ?", String.class,
                producer)).isNull();
        summary(null, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(stored));
        assertThat(rows()).isEqualTo(1);
        // Running it again the same day with the same data stores nothing new
        scheduler.precompute();
        assertThat(rows()).isEqualTo(1);
    }

    @Test
    @DisplayName("Errors: 400 malformed plotId, 404 unknown plot, 403 foreign plot or inactive plan, 409 no plots, "
            + "401 without session; POST /answers does not take the summary")
    void errors() throws Exception {
        summary(null, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Sin lotes activos · registra un lote"));
        summary("not-a-uuid", "es")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        summary(UUID.randomUUID().toString(), "es").andExpect(status().isNotFound());
        authenticateAs(UUID.randomUUID().toString());
        var foreign = createPlot();
        authenticateAs(producer);
        summary(foreign, "es").andExpect(status().isForbidden());
        answer("home.daily_summary", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        when(subscriptionContextFacade.findEntitledQuotaHectares(anyString())).thenReturn(Optional.empty());
        summary(null, "es")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ASSISTANT_FORBIDDEN"));
        assertThat(rows()).isZero();

        SecurityContextHolder.clearContext();
        summary(null, "es")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private int rows() {
        return jdbc.queryForObject("select count(*) from assistant_answers where producer_id = ?", Integer.class,
                producer);
    }

    private ResultActions summary(String plotId, String language) throws Exception {
        var request = get(SUMMARY).header(HttpHeaders.ACCEPT_LANGUAGE, language);
        return mvc.perform(plotId == null ? request : request.param("plotId", plotId));
    }

    private ResultActions answer(String questionId, String plotId) throws Exception {
        var body = plotId == null
                ? "{\"questionId\":\"%s\",\"clientRequestId\":\"%s\"}".formatted(questionId, UUID.randomUUID())
                : "{\"questionId\":\"%s\",\"plotId\":\"%s\",\"clientRequestId\":\"%s\"}"
                        .formatted(questionId, plotId, UUID.randomUUID());
        return mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "es"));
    }

    private void raiseHeat(String plotId) {
        incidentCommandService.handle(new RaiseAgroclimaticIncidentCommand(plotId, "HEAT_WAVE", "WARNING",
                "AMBIENT_TEMPERATURE", 34.2, 32.0, "°C", List.of("heat_wave.step.pre_irrigation",
                "heat_wave.step.morning_irrigation", "heat_wave.step.suspend_cultural_ops")));
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
