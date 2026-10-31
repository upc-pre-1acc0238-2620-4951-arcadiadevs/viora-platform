package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider.HourlyWeatherObservation;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.WeatherForecastProvider;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastDay;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real controllers, services, Telemetry facade and H2: the CLIMATE screen (P90, P130-P132). The forecast and the
 * hourly weather the virtual node simulates from are the only providers stubbed.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantclimate;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant CLIMATE screen")
class AssistantClimateScreenIntegrationTest {

    private static final String QUESTIONS = "/api/v1/assistant/questions";
    private static final String ANSWERS = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
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
    @DisplayName("with a virtual node: no irrigation (409), the sensors answer says \"dato simulado\"")
    void virtualNode() throws Exception {
        var plotId = createPlot();
        registerSoilProbe(plotId);
        // Reading the telemetry lets the virtual node fill the last hours, as the sensors screen does
        mvc.perform(get("/api/v1/plots/{plotId}/telemetries", plotId)).andExpect(status().isOk());

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("CLIMATE"))
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(jsonPath("$.questions[0].questionId").value("climate.irrigate"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Debo regar esta semana?"))
                .andExpect(jsonPath("$.questions[0].icon").value("water"))
                .andExpect(jsonPath("$.questions[0].enabled").value(false))
                .andExpect(jsonPath("$.questions[0].disabledReason")
                        .value("Requiere una sonda de humedad real en el lote"))
                .andExpect(jsonPath("$.questions[1].questionId").value("climate.fruitset_effect"))
                .andExpect(jsonPath("$.questions[1].enabled").value(true))
                .andExpect(jsonPath("$.questions[2].questionId").value("climate.heat_ends"))
                .andExpect(jsonPath("$.questions[2].enabled").value(true))
                .andExpect(jsonPath("$.questions[3].questionId").value("climate.sensors_say"))
                .andExpect(jsonPath("$.questions[3].icon").value("sensor"))
                .andExpect(jsonPath("$.questions[3].enabled").value(true));
        catalog(plotId, "en")
                .andExpect(jsonPath("$.questions[0].label").value("Should I irrigate this week?"))
                .andExpect(jsonPath("$.questions[0].disabledReason")
                        .value("Needs a real soil moisture probe on the plot"));

        answer("climate.irrigate", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"));
        answer("climate.sensors_say", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("¿Qué dicen mis sensores?"))
                .andExpect(jsonPath("$.text").value("La última lectura de tu lote marca 27,4 °C, 38 % de humedad "
                        + "del aire y 21,5 % en el suelo: dato simulado. Sale del clima observado en tu zona, no de un "
                        + "sensor en el lote."))
                .andExpect(jsonPath("$.facts", hasSize(2)))
                .andExpect(jsonPath("$.facts[0].pill").value("dato simulado"))
                .andExpect(jsonPath("$.sources[0].label").value("Nodo virtual · dato simulado"));
        answer("climate.sensors_say", plotId, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", containsString("simulated data")))
                .andExpect(jsonPath("$.sources[0].label").value("Virtual node · simulated data"));
        answer("climate.heat_ends", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", startsWith("El calor del pronóstico sigue hasta el ")))
                .andExpect(jsonPath("$.facts[0].value").value("3"))
                .andExpect(jsonPath("$.facts[0].pill").value("umbral 32 °C"));
        answer("climate.fruitset_effect", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", startsWith("El pronóstico llega a 35,1 °C el ")))
                .andExpect(jsonPath("$.sources[0].label", startsWith("Pronóstico de Open-Meteo, actualizado el ")));

        mvc.perform(get(ANSWERS + "/latest").param("screen", "CLIMATE").param("plotId", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId", startsWith("climate.")));
    }

    @Test
    @DisplayName("without nodes or forecast: the questions that need them are disabled with their reasons")
    void withoutData() throws Exception {
        when(weatherForecastProvider.fetchSevenDayForecast(anyDouble(), anyDouble())).thenReturn(List.of());
        var plotId = createPlot();

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].disabledReason")
                        .value("Requiere una sonda de humedad real en el lote"))
                .andExpect(jsonPath("$.questions[1].disabledReason").value("Sin pronóstico disponible para este lote"))
                .andExpect(jsonPath("$.questions[2].disabledReason").value("Sin pronóstico disponible para este lote"))
                .andExpect(jsonPath("$.questions[3].disabledReason")
                        .value("Vincula un nodo al lote para ver sus lecturas"));
        answer("climate.heat_ends", plotId, "es").andExpect(status().isConflict());
        catalog(UUID.randomUUID().toString(), "es").andExpect(status().isNotFound());
    }

    private ResultActions catalog(String plotId, String language) throws Exception {
        return mvc.perform(get(QUESTIONS).param("screen", "CLIMATE").param("plotId", plotId)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private ResultActions answer(String questionId, String plotId, String language) throws Exception {
        var body = "{\"questionId\":\"%s\",\"plotId\":\"%s\",\"clientRequestId\":\"%s\",\"forceRefresh\":false}"
                .formatted(questionId, plotId, UUID.randomUUID());
        return mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
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
