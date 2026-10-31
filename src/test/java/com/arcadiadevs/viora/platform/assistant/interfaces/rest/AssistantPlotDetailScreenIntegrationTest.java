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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
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
 * Real controllers, services, Phenology and Telemetry facades and H2: the PLOT_DETAIL screen (P26, P136-P138). The
 * thinning plan of the plot is stubbed at the Thinning facade, the forecast at its provider.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantplotdetail;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant PLOT_DETAIL screen")
class AssistantPlotDetailScreenIntegrationTest {

    private static final String QUESTIONS = "/api/v1/assistant/questions";
    private static final String ANSWERS = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
    @Autowired AgroclimaticIncidentCommandService incidentCommandService;
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
    @DisplayName("open window, two harvests and a heat alert: how_is shows the index and the 12 days left")
    void openWindow() throws Exception {
        int year = today.getYear();
        var plotId = createPlot();
        recordHarvest(plotId, year - 2, 1000.0);
        recordHarvest(plotId, year - 1, 3000.0);
        raiseHeat(plotId);
        stubPlan(plotId, new ThinningPlanDetail(true, 30.0, 0.5714, 0.4, today.minusDays(10), today.plusDays(11), 5,
                today.minusDays(1), null, null, null, null, null), 0);

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("PLOT_DETAIL"))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(jsonPath("$.questions[0].questionId").value("plot.how_is"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Cómo está este lote?"))
                .andExpect(jsonPath("$.questions[0].icon").value("lote"))
                .andExpect(jsonPath("$.questions[0].enabled").value(true))
                .andExpect(jsonPath("$.questions[1].questionId").value("plot.today"))
                .andExpect(jsonPath("$.questions[1].label").value("¿Qué hago hoy en este lote?"))
                .andExpect(jsonPath("$.questions[1].icon").value("calendar"))
                .andExpect(jsonPath("$.questions[1].enabled").value(true))
                .andExpect(jsonPath("$.questions[2].questionId").value("plot.last_sampling"))
                .andExpect(jsonPath("$.questions[2].label").value("¿Qué dijo el último muestreo?"))
                .andExpect(jsonPath("$.questions[2].icon").value("fruit"))
                .andExpect(jsonPath("$.questions[2].enabled").value(true))
                .andExpect(jsonPath("$.questions[3].questionId").value("plot.next_year_harvest"))
                .andExpect(jsonPath("$.questions[3].label").value("¿Cuánto cosecharé en %d?".formatted(year + 1)))
                .andExpect(jsonPath("$.questions[3].icon").value("harvest"))
                .andExpect(jsonPath("$.questions[3].enabled").value(false))
                .andExpect(jsonPath("$.questions[3].disabledReason")
                        .value("Faltan datos de calibre · registra un muestreo"));
        catalog(plotId, "en")
                .andExpect(jsonPath("$.questions[0].label").value("How is this plot doing?"))
                .andExpect(jsonPath("$.questions[3].label").value("How much will I harvest in %d?".formatted(year + 1)))
                .andExpect(jsonPath("$.questions[3].disabledReason")
                        .value("Caliber data is missing · record a sampling"));

        answer("plot.how_is", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("plot.how_is"))
                .andExpect(jsonPath("$.question").value("¿Cómo está este lote?"))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.text", startsWith("Tu índice de vecería es 0,50, ")))
                .andExpect(jsonPath("$.text", containsString(
                        ". La ventana de aclareo está abierta: quedan 12 días para quitar el 30 % de los frutos, ")))
                .andExpect(jsonPath("$.facts", hasSize(2)))
                .andExpect(jsonPath("$.facts[0].value").value("0,50"))
                .andExpect(jsonPath("$.facts[0].label").value("índice de vecería"))
                .andExpect(jsonPath("$.facts[0].tone").value("PRIMARY"))
                .andExpect(jsonPath("$.facts[1].value").value("12"))
                .andExpect(jsonPath("$.facts[1].label").value("días de ventana"))
                .andExpect(jsonPath("$.facts[1].pill").value("ventana abierta"))
                .andExpect(jsonPath("$.facts[1].tone").value("HIGHLIGHT"))
                .andExpect(jsonPath("$.sources[0].label").value("Basado en 2 campañas registradas"))
                .andExpect(jsonPath("$.actions", hasSize(0)))
                .andExpect(jsonPath("$.model.provider").value("TEMPLATE"));
        answer("plot.today", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", startsWith("Hoy puedes aclarear: la ventana cierra el ")))
                .andExpect(jsonPath("$.text", containsString(" el plan indica quitar el 30 % de los frutos. Hoy se "
                        + "esperan 34,2 °C de máxima, que alcanzan el umbral de calor de 32 °C. ")))
                .andExpect(jsonPath("$.text", endsWith("Tienes 1 alerta activa, la más grave de golpe de calor en "
                        + "nivel aviso.")))
                .andExpect(jsonPath("$.facts[0].value").value("34,2 °C"))
                .andExpect(jsonPath("$.facts[0].label").value("máxima de hoy"))
                .andExpect(jsonPath("$.facts[0].pill").value("mínima 14 °C"))
                .andExpect(jsonPath("$.facts[1].value").value("1"))
                .andExpect(jsonPath("$.facts[1].pill").value("en este lote"))
                .andExpect(jsonPath("$.sources[0].label").value("Basado en el plan de aclareo de esta campaña"));
        answer("plot.last_sampling", plotId, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("What did the last sampling say?"))
                .andExpect(jsonPath("$.text", startsWith("Your sampling this campaign has 5 trees, the latest on ")))
                .andExpect(jsonPath("$.text", endsWith(": it measured 0.57 fruits per shoot against a target of "
                        + "0.40, which is why the plan says to remove 30 % of the fruits.")))
                .andExpect(jsonPath("$.facts[0].value").value("5"))
                .andExpect(jsonPath("$.facts[0].label").value("trees sampled"))
                .andExpect(jsonPath("$.facts[1].value").value("0.57"))
                .andExpect(jsonPath("$.facts[1].pill").value("target 0.40"))
                .andExpect(jsonPath("$.sources[0].label", startsWith("Based on 5 trees, round of ")));
        answer("plot.next_year_harvest", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"));

        mvc.perform(get(ANSWERS + "/latest").param("screen", "PLOT_DETAIL").param("plotId", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("plot.last_sampling"))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"));
    }

    @Test
    @DisplayName("thinned plot with an estimated caliber: the harvest answers, how_is without index")
    void thinnedWithCaliber() throws Exception {
        var plotId = createPlot();
        stubPlan(plotId, new ThinningPlanDetail(true, 30.0, 0.5714, 0.4, today.minusDays(10), today.plusDays(11), 5,
                today.minusDays(3), today.minusDays(2), "ESTIMATED", 180.4, "171/190", 14250.0), 0);

        catalog(plotId, "es")
                .andExpect(jsonPath("$.questions[0].enabled").value(true))
                .andExpect(jsonPath("$.questions[3].enabled").value(true));
        answer("plot.next_year_harvest", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Con la carga que dejó tu aclareo, Viora estima 14.250 kg por "
                        + "hectárea para la cosecha de esta campaña, con un calibre más probable de 180 frutos por "
                        + "kilo (171/190). Es una proyección en calibración."))
                .andExpect(jsonPath("$.facts[0].value").value("14.250"))
                .andExpect(jsonPath("$.facts[1].value").value("180"))
                .andExpect(jsonPath("$.facts[1].pill").value("calibre 171/190"))
                .andExpect(jsonPath("$.sources[0].label", startsWith("Basado en el aclareo registrado el ")));
        answer("plot.how_is", plotId, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", startsWith("More recorded harvests are needed to calculate the "
                        + "alternate bearing index of the plot. You recorded the thinning of this campaign on ")))
                .andExpect(jsonPath("$.facts[0].value").value("No index"))
                .andExpect(jsonPath("$.facts[1].value").value("Recorded"));
    }

    @Test
    @DisplayName("a plot never sampled: how_is, last_sampling and the harvest disabled with reasons; today answers")
    void neverSampled() throws Exception {
        var plotId = createPlot();

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].enabled").value(false))
                .andExpect(jsonPath("$.questions[0].disabledReason")
                        .value("Sin índice de vecería ni muestreo para este lote todavía"))
                .andExpect(jsonPath("$.questions[1].enabled").value(true))
                .andExpect(jsonPath("$.questions[2].enabled").value(false))
                .andExpect(jsonPath("$.questions[2].disabledReason")
                        .value("Sin muestreo en esta campaña · registra un muestreo"))
                .andExpect(jsonPath("$.questions[3].enabled").value(false))
                .andExpect(jsonPath("$.questions[3].disabledReason")
                        .value("Faltan datos de calibre · registra un muestreo"));
        catalog(plotId, "en")
                .andExpect(jsonPath("$.questions[0].disabledReason")
                        .value("No bearing index or sampling for this plot yet"))
                .andExpect(jsonPath("$.questions[2].disabledReason")
                        .value("No sampling this campaign · record a sampling"));
        answer("plot.last_sampling", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Aún no hay datos suficientes para responder esta pregunta"));
        answer("plot.today", plotId, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("You have no pending thinning tasks in the plot today. Today a "
                        + "maximum of 34.2 °C is expected, reaching the heat threshold of 32 °C. You have no open "
                        + "alerts in the plot."))
                .andExpect(jsonPath("$.facts[0].value").value("34.2 °C"))
                .andExpect(jsonPath("$.facts[1].value").value("0"))
                .andExpect(jsonPath("$.facts[1].label").value("open alerts"))
                .andExpect(jsonPath("$.sources[0].label", startsWith("Open-Meteo forecast, updated on ")));
    }

    @Test
    @DisplayName("PLOT_DETAIL requires its plot; another producer's plot is forbidden and an unknown one not found")
    void requiresOwnPlot() throws Exception {
        mvc.perform(get(QUESTIONS).param("screen", "PLOT_DETAIL").header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isBadRequest());

        authenticateAs(UUID.randomUUID().toString());
        var foreign = createPlot();
        authenticateAs(producer);
        catalog(foreign, "es").andExpect(status().isForbidden());
        answer("plot.today", foreign, "es").andExpect(status().isForbidden());
        catalog(UUID.randomUUID().toString(), "es").andExpect(status().isNotFound());
    }

    private void stubPlan(String plotId, ThinningPlanDetail plan, int treesNeeded) {
        when(thinningContextFacade.findThinningPlanDetail(eq(plotId), anyInt())).thenReturn(Optional.of(plan));
        when(thinningContextFacade.findPrescriptionAvailability(eq(plotId), anyInt()))
                .thenReturn(Optional.of(new PrescriptionAvailability(plan.issued(), plan.percentageToRemove(),
                        plan.sampledTrees(), treesNeeded, plan.caliberProjectionStatus())));
    }

    private ResultActions catalog(String plotId, String language) throws Exception {
        return mvc.perform(get(QUESTIONS).param("screen", "PLOT_DETAIL").param("plotId", plotId)
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
