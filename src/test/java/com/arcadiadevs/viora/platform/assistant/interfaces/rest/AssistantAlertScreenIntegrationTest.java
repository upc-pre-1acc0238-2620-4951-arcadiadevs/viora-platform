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

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real controllers, services, Telemetry and Orchard facades and H2: the ALERT screen (T15, P123-P125). The alert is
 * raised through Telemetry, so its threshold and the forecast flags are the ones Telemetry computes.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantalert;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant ALERT screen")
class AssistantAlertScreenIntegrationTest {

    private static final String QUESTIONS = "/api/v1/assistant/questions";
    private static final String ANSWERS = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
    @Autowired AgroclimaticIncidentCommandService incidentCommandService;
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
    @DisplayName("with a heat alert: four questions enabled and answers from the alert, the forecast and the owner's plots")
    void heatAlert() throws Exception {
        var plotId = createPlot();
        createPlot();
        raiseHeat(plotId);
        // Another producer's plot with the same alert is never counted
        authenticateAs(UUID.randomUUID().toString());
        raiseHeat(createPlot());
        authenticateAs(producer);

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("ALERT"))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(jsonPath("$.questions[0].questionId").value("alert.what_to_do"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Qué hago ante este golpe de calor?"))
                .andExpect(jsonPath("$.questions[0].icon").value("alert"))
                .andExpect(jsonPath("$.questions[1].questionId").value("alert.fruitset_effect"))
                .andExpect(jsonPath("$.questions[1].icon").value("fruit"))
                .andExpect(jsonPath("$.questions[2].questionId").value("alert.when_ends"))
                .andExpect(jsonPath("$.questions[2].icon").value("calendar"))
                .andExpect(jsonPath("$.questions[3].questionId").value("alert.plots_at_risk"))
                .andExpect(jsonPath("$.questions[3].icon").value("lote"))
                .andExpect(jsonPath("$.questions[*].enabled").value(everyItem(is(true))));
        catalog(plotId, "en")
                .andExpect(jsonPath("$.questions[0].label").value("What do I do about this heat wave?"));

        answer("alert.what_to_do", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("alert.what_to_do"))
                .andExpect(jsonPath("$.question").value("¿Qué hago ante este golpe de calor?"))
                .andExpect(jsonPath("$.text").value("Tu lote tiene una alerta de golpe de calor en nivel aviso: "
                        + "34,2 °C frente al umbral de 32 °C. Sigue los pasos de mitigación de la alerta; llevas 0 de "
                        + "3 completados."))
                .andExpect(jsonPath("$.facts", hasSize(2)))
                .andExpect(jsonPath("$.facts[0].value").value("34,2 °C"))
                .andExpect(jsonPath("$.facts[0].pill").value("umbral 32 °C"))
                .andExpect(jsonPath("$.facts[0].tone").value("PRIMARY"))
                .andExpect(jsonPath("$.facts[1].value").value("Aviso"))
                .andExpect(jsonPath("$.facts[1].tone").value("HIGHLIGHT"))
                .andExpect(jsonPath("$.sources[0].label", startsWith("Basado en la alerta del ")))
                .andExpect(jsonPath("$.actions", hasSize(0)))
                .andExpect(jsonPath("$.model.provider").value("TEMPLATE"));
        answer("alert.when_ends", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", startsWith("El pronóstico mantiene el riesgo de golpe de calor hasta "
                        + "el ")))
                .andExpect(jsonPath("$.facts[0].value").value("3"))
                .andExpect(jsonPath("$.facts[0].pill").value("umbral 32 °C"))
                .andExpect(jsonPath("$.sources[0].label", startsWith("Pronóstico de Open-Meteo, actualizado el ")));
        answer("alert.plots_at_risk", plotId, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Only this plot has an open heat wave alert among your 2 active "
                        + "plots. The others do not show it for now."));

        mvc.perform(get(ANSWERS + "/latest").param("screen", "ALERT").param("plotId", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId", startsWith("alert.")))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"));
    }

    @Test
    @DisplayName("without an open alert: every question disabled with its reason, and answering one is a 409")
    void withoutAlert() throws Exception {
        var plotId = createPlot();

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].label").value("¿Qué hago ante esta alerta?"))
                .andExpect(jsonPath("$.questions[*].enabled").value(everyItem(is(false))))
                .andExpect(jsonPath("$.questions[3].disabledReason").value("Sin alertas activas en este lote"));
        catalog(plotId, "en")
                .andExpect(jsonPath("$.questions[0].disabledReason").value("No active alerts on this plot"));
        answer("alert.what_to_do", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Aún no hay datos suficientes para responder esta pregunta"));
        catalog(UUID.randomUUID().toString(), "es").andExpect(status().isNotFound());
    }

    private ResultActions catalog(String plotId, String language) throws Exception {
        return mvc.perform(get(QUESTIONS).param("screen", "ALERT").param("plotId", plotId)
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
