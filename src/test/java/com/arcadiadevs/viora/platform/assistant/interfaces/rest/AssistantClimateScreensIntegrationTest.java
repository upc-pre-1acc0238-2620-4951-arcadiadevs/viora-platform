package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import com.arcadiadevs.viora.platform.telemetry.application.commandservices.AgroclimaticIncidentCommandService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider.HourlyWeatherObservation;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real controllers, services, Telemetry facade and H2: the ALERT (P123-P125) and CLIMATE (P130-P132) screens. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantclimate;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant ALERT and CLIMATE screens")
class AssistantClimateScreensIntegrationTest {

    private static final String QUESTIONS = "/api/v1/assistant/questions";
    private static final String ANSWERS = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
    @Autowired AgroclimaticIncidentCommandService incidentCommandService;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    @MockitoBean WeatherForecastProvider weatherForecastProvider;
    @MockitoBean HourlyWeatherProvider hourlyWeatherProvider;
    private MockMvc mvc;
    private final String producer = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        authenticateAs(producer);
        when(subscriptionContextFacade.findEntitledQuotaHectares(anyString())).thenReturn(Optional.of(BigDecimal.TEN));
        when(weatherForecastProvider.fetchSevenDayForecast(anyDouble(), anyDouble()))
                .thenReturn(forecast(34.2, 35.1, 33.0, 30.4, 29.8, 28.0, 27.5));
        var currentHour = Instant.now().truncatedTo(ChronoUnit.HOURS);
        when(hourlyWeatherProvider.fetchPastHours(anyDouble(), anyDouble(), anyInt())).thenReturn(List.of(
                new HourlyWeatherObservation(currentHour.minus(Duration.ofHours(2)), 25.0, 40.0, 22.0, 400.0),
                new HourlyWeatherObservation(currentHour.minus(Duration.ofHours(1)), 27.4, 38.0, 21.5, 450.0)));
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("ALERT with a heat alert: four questions enabled, the answer reads the alert and the forecast")
    void alertWithHeat() throws Exception {
        var plotId = createPlot();
        createPlot();
        raiseHeat(plotId);
        // Another producer's plot with the same alert is never counted
        authenticateAs(UUID.randomUUID().toString());
        raiseHeat(createPlot());
        authenticateAs(producer);

        catalog("ALERT", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("ALERT"))
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(jsonPath("$.questions[0].questionId").value("alert.what_to_do"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Qué hago ante este golpe de calor?"))
                .andExpect(jsonPath("$.questions[0].icon").value("alert"))
                .andExpect(jsonPath("$.questions[1].questionId").value("alert.fruitset_effect"))
                .andExpect(jsonPath("$.questions[2].questionId").value("alert.when_ends"))
                .andExpect(jsonPath("$.questions[3].questionId").value("alert.plots_at_risk"))
                .andExpect(jsonPath("$.questions[*].enabled").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(true))));
        catalog("ALERT", plotId, "en")
                .andExpect(jsonPath("$.questions[0].label").value("What do I do about this heat wave?"));

        answer("alert.what_to_do", plotId, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Tu lote tiene una alerta de golpe de calor en nivel aviso: "
                        + "34,2 °C frente al umbral de 32 °C. Sigue los pasos de mitigación de la alerta; llevas 0 de "
                        + "3 completados."))
                .andExpect(jsonPath("$.facts.length()").value(2))
                .andExpect(jsonPath("$.facts[0].value").value("34,2 °C"))
                .andExpect(jsonPath("$.facts[0].tone").value("PRIMARY"))
                .andExpect(jsonPath("$.facts[1].value").value("Aviso"))
                .andExpect(jsonPath("$.facts[1].tone").value("HIGHLIGHT"))
                .andExpect(jsonPath("$.sources[0].label", startsWith("Basado en la alerta del ")))
                .andExpect(jsonPath("$.actions.length()").value(0));
        answer("alert.when_ends", plotId, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text", startsWith("El pronóstico mantiene el riesgo de golpe de calor hasta "
                        + "el ")))
                .andExpect(jsonPath("$.sources[0].label", startsWith("Pronóstico de Open-Meteo, actualizado el ")));
        answer("alert.plots_at_risk", plotId, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text", startsWith("Solo este lote tiene una alerta de golpe de calor abierta "
                        + "entre tus 2 lotes activos.")));
    }

    @Test
    @DisplayName("ALERT without an open alert: every question disabled and a 409 with the reason")
    void alertWithoutIncident() throws Exception {
        var plotId = createPlot();

        catalog("ALERT", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].label").value("¿Qué hago ante esta alerta?"))
                .andExpect(jsonPath("$.questions[0].enabled").value(false))
                .andExpect(jsonPath("$.questions[3].disabledReason").value("Sin alertas activas en este lote"));
        answer("alert.what_to_do", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_QUESTION_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Sin alertas activas en este lote"));
        catalog("ALERT", UUID.randomUUID().toString(), "es").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CLIMATE with a virtual node: no irrigation (409), the sensors answer says \"dato simulado\"")
    void climateWithVirtualNode() throws Exception {
        var plotId = createPlot();
        registerSoilProbe(plotId);
        // Reading the telemetry lets the virtual node fill the last hours, as the sensors screen does
        mvc.perform(get("/api/v1/plots/{plotId}/telemetries", plotId)).andExpect(status().isOk());

        catalog("CLIMATE", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].questionId").value("climate.irrigate"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Debo regar esta semana?"))
                .andExpect(jsonPath("$.questions[0].icon").value("water"))
                .andExpect(jsonPath("$.questions[0].enabled").value(false))
                .andExpect(jsonPath("$.questions[0].disabledReason")
                        .value("Requiere una sonda de humedad real en el lote"))
                .andExpect(jsonPath("$.questions[1].enabled").value(true))
                .andExpect(jsonPath("$.questions[2].enabled").value(true))
                .andExpect(jsonPath("$.questions[3].questionId").value("climate.sensors_say"))
                .andExpect(jsonPath("$.questions[3].enabled").value(true));
        answer("climate.irrigate", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Requiere una sonda de humedad real en el lote"));
        answer("climate.sensors_say", plotId, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("La última lectura de tu lote marca 27,4 °C, 38 % de humedad "
                        + "del aire y 21,5 % en el suelo: dato simulado. Sale del clima observado en tu zona, no de un "
                        + "sensor en el lote."))
                .andExpect(jsonPath("$.facts[0].pill").value("dato simulado"))
                .andExpect(jsonPath("$.sources[0].label").value("Nodo virtual · dato simulado"));
        answer("climate.sensors_say", plotId, "en")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text", containsString("simulated data")))
                .andExpect(jsonPath("$.sources[0].label").value("Virtual node · simulated data"));
        answer("climate.heat_ends", plotId, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text", startsWith("El calor del pronóstico sigue hasta el ")));
    }

    @Test
    @DisplayName("CLIMATE without nodes or forecast disables the questions that need them")
    void climateWithoutData() throws Exception {
        when(weatherForecastProvider.fetchSevenDayForecast(anyDouble(), anyDouble())).thenReturn(List.of());
        var plotId = createPlot();

        catalog("CLIMATE", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[1].disabledReason").value("Sin pronóstico disponible para este lote"))
                .andExpect(jsonPath("$.questions[2].disabledReason").value("Sin pronóstico disponible para este lote"))
                .andExpect(jsonPath("$.questions[3].disabledReason")
                        .value("Vincula un nodo al lote para ver sus lecturas"));
    }

    private ResultActions catalog(String screen, String plotId, String language) throws Exception {
        return mvc.perform(get(QUESTIONS).param("screen", screen).param("plotId", plotId)
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

    private void registerSoilProbe(String plotId) throws Exception {
        mvc.perform(post("/api/v1/plots/{plotId}/iot-devices", plotId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Sonda %s","type":"SOIL_PROBE","depthCm":30,"soilTextureType":"LOAM",
                                 "calibrationMultiplier":1.0}
                                """.formatted(UUID.randomUUID().toString().substring(0, 8))))
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
