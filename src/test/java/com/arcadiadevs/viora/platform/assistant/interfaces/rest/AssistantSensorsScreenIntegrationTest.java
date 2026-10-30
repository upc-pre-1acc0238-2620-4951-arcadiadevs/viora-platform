package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider.HourlyWeatherObservation;
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
import java.time.temporal.ChronoUnit;
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

/**
 * Real controllers, services, Telemetry facade and H2: the SENSORS screen (P85, P139-P141). The virtual node fills
 * the last hours of the plot from the mocked hourly weather when its telemetry is read.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantsensors;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant SENSORS screen")
class AssistantSensorsScreenIntegrationTest {

    private static final String QUESTIONS = "/api/v1/assistant/questions";
    private static final String ANSWERS = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    @MockitoBean HourlyWeatherProvider hourlyWeatherProvider;
    private MockMvc mvc;
    private final String producer = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        authenticateAs(producer);
        when(subscriptionContextFacade.findEntitledQuotaHectares(anyString())).thenReturn(Optional.of(BigDecimal.TEN));
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
    @DisplayName("Two virtual nodes reading: what_say gives 2 nodes and 27,4 °C as simulated; why_paused disabled")
    void nodesReading() throws Exception {
        var plotId = createPlot();
        registerNode(plotId, "MICROCLIMATE", null);
        registerNode(plotId, "SOIL_PROBE", 30);
        mvc.perform(get("/api/v1/plots/{plotId}/telemetries", plotId)).andExpect(status().isOk());

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("SENSORS"))
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(jsonPath("$.questions[0].questionId").value("sensors.what_say"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Qué dicen mis sensores?"))
                .andExpect(jsonPath("$.questions[0].enabled").value(true))
                .andExpect(jsonPath("$.questions[1].questionId").value("sensors.why_paused"))
                .andExpect(jsonPath("$.questions[1].enabled").value(false))
                .andExpect(jsonPath("$.questions[1].disabledReason").value("Ningún nodo de este lote está pausado"))
                .andExpect(jsonPath("$.questions[2].questionId").value("sensors.reliability"))
                .andExpect(jsonPath("$.questions[2].enabled").value(true))
                .andExpect(jsonPath("$.questions[3].questionId").value("sensors.how_link"))
                .andExpect(jsonPath("$.questions[3].enabled").value(true));
        catalog(plotId, "en")
                .andExpect(jsonPath("$.questions[1].disabledReason").value("No node of this plot is paused"));

        answer("sensors.what_say", plotId, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("La última lectura de tu lote marca 27,4 °C, 38 % de humedad "
                        + "del aire y 21,5 % en el suelo: dato simulado. Sale del clima observado en tu zona, no de un "
                        + "sensor en el lote."))
                .andExpect(jsonPath("$.facts[0].value").value("27,4 °C"))
                .andExpect(jsonPath("$.facts[0].pill").value("dato simulado"))
                .andExpect(jsonPath("$.sources[0].label").value("Nodo virtual · dato simulado"));
        answer("sensors.why_paused", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ningún nodo de este lote está pausado"));
        answer("sensors.reliability", plotId, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text", startsWith("Hoy tus lecturas son simuladas: salen del clima observado "
                        + "en tu zona, no de un sensor en el lote, así que úsalas como referencia. En los últimos 7 "
                        + "días hubo lecturas en ")))
                .andExpect(jsonPath("$.facts[0].value", containsString("/7")))
                .andExpect(jsonPath("$.facts[0].label").value("días con lecturas"))
                .andExpect(jsonPath("$.facts[1].value").value("Simulado"))
                .andExpect(jsonPath("$.facts[1].pill").value("dato simulado"));
        answer("sensors.how_link", plotId, "en")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text", startsWith("Your plot has 2 linked nodes. To link a node, register it "
                        + "on the plot with a name no other node uses and its type: ")))
                .andExpect(jsonPath("$.facts[0].label").value("microclimate node"))
                .andExpect(jsonPath("$.facts[1].label").value("soil probe"))
                .andExpect(jsonPath("$.actions.length()").value(0));
    }

    @Test
    @DisplayName("A plot without nodes: only how_link is offered and it says there are none")
    void noNodes() throws Exception {
        var plotId = createPlot();

        catalog(plotId, "es")
                .andExpect(jsonPath("$.questions[0].disabledReason")
                        .value("Vincula un nodo al lote para ver sus lecturas"))
                .andExpect(jsonPath("$.questions[1].enabled").value(false))
                .andExpect(jsonPath("$.questions[2].disabledReason")
                        .value("Vincula un nodo al lote para ver sus lecturas"))
                .andExpect(jsonPath("$.questions[3].enabled").value(true));
        answer("sensors.how_link", plotId, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text", startsWith("Tu lote aún no tiene nodos vinculados. Para vincular un "
                        + "nodo, regístralo en el lote ")))
                .andExpect(jsonPath("$.facts[0].value").value("0"))
                .andExpect(jsonPath("$.facts[0].label").value("nodos de microclima"));
        answer("sensors.reliability", plotId, "es")
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("SENSORS requires its plot; a plot of another producer answers 403")
    void requiresOwnPlot() throws Exception {
        mvc.perform(get(QUESTIONS).param("screen", "SENSORS").header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Indica el lote de esta pantalla"));
        authenticateAs(UUID.randomUUID().toString());
        var foreign = createPlot();
        authenticateAs(producer);
        catalog(foreign, "es").andExpect(status().isForbidden());
        answer("sensors.how_link", foreign, "es").andExpect(status().isForbidden());
    }

    private ResultActions catalog(String plotId, String language) throws Exception {
        return mvc.perform(get(QUESTIONS).param("screen", "SENSORS").param("plotId", plotId)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private ResultActions answer(String questionId, String plotId, String language) throws Exception {
        var body = "{\"questionId\":\"%s\",\"plotId\":\"%s\",\"clientRequestId\":\"%s\"}"
                .formatted(questionId, plotId, UUID.randomUUID());
        return mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private void registerNode(String plotId, String type, Integer depthCm) throws Exception {
        var depth = depthCm == null ? "" : ",\"depthCm\":" + depthCm;
        mvc.perform(post("/api/v1/plots/{plotId}/iot-devices", plotId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nodo %s\",\"type\":\"%s\"%s}"
                                .formatted(UUID.randomUUID().toString().substring(0, 8), type, depth)))
                .andExpect(status().isCreated());
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
