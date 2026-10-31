package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.weather.HourlyTemperatureProvider;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HourlyTemperature;
import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
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
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real controllers, services, Phenology facade and H2: the WINTER_CHILL screen (P80, P133-P135). Only the hourly
 * temperatures Phenology evaluates the Erez chill from are stubbed, on a fixed clock in the middle of the winter.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantwinterchill;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant WINTER_CHILL screen")
class AssistantWinterChillScreenIntegrationTest {

    private static final String QUESTIONS = "/api/v1/assistant/questions";
    private static final String ANSWERS = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
    @TestBean Clock clock;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    @MockitoBean HourlyTemperatureProvider temperatureProvider;
    private MockMvc mvc;
    private final String producer = UUID.randomUUID().toString();

    /** June 25, 2026 at 10:00 in Lima: the winter of 2026 has 24 whole days. */
    static Clock clock() {
        return Clock.fixed(Instant.parse("2026-06-25T15:00:00Z"), ZoneOffset.UTC);
    }

    @BeforeEach
    void setUp() {
        authenticateAs(producer);
        when(subscriptionContextFacade.findEntitledQuotaHectares(anyString())).thenReturn(Optional.of(BigDecimal.TEN));
        // A cold 2026 winter (6 °C every hour) and a 2025 winter too warm to accumulate (15 °C)
        when(temperatureProvider.fetchHourlyTemperatures(anyDouble(), anyDouble(), any(LocalDate.class),
                any(LocalDate.class))).thenAnswer(invocation -> {
                    LocalDate from = invocation.getArgument(2);
                    LocalDate to = invocation.getArgument(3);
                    double celsius = from.getYear() == 2026 ? 6.0 : 15.0;
                    return Optional.of(days(from, (int) (to.toEpochDay() - from.toEpochDay()) + 1, celsius));
                });
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("winter in progress: four questions enabled with the real threshold, answers from the Erez chill")
    void winterInProgress() throws Exception {
        var plotId = createPlot();

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("WINTER_CHILL"))
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(jsonPath("$.questions[0].questionId").value("chill.missing"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Cuánto frío me falta?"))
                .andExpect(jsonPath("$.questions[0].icon").value("temp"))
                .andExpect(jsonPath("$.questions[1].questionId").value("chill.reach_30"))
                .andExpect(jsonPath("$.questions[1].label").value("¿Cuándo llego a las 30 porciones?"))
                .andExpect(jsonPath("$.questions[1].icon").value("calendar"))
                .andExpect(jsonPath("$.questions[2].questionId").value("chill.vs_last_winter"))
                .andExpect(jsonPath("$.questions[2].icon").value("history"))
                .andExpect(jsonPath("$.questions[3].questionId").value("chill.if_incomplete"))
                .andExpect(jsonPath("$.questions[3].icon").value("alert"))
                .andExpect(jsonPath("$.questions[*].enabled").value(everyItem(is(true))));
        catalog(plotId, "en")
                .andExpect(jsonPath("$.questions[1].label").value("When will I reach 30 portions?"));

        var answer = answer("chill.missing", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("chill.missing"))
                .andExpect(jsonPath("$.question").value("¿Cuánto frío me falta?"))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.text", startsWith("Tu lote lleva ")))
                .andExpect(jsonPath("$.text", containsString(" de 30 porciones de frío este invierno: le faltan ")))
                .andExpect(jsonPath("$.text", endsWith("Están contadas hasta el 24 de junio.")))
                .andExpect(jsonPath("$.facts", hasSize(2)))
                .andExpect(jsonPath("$.facts[0].label").value("porciones de frío"))
                .andExpect(jsonPath("$.facts[0].pill").value("hasta el 24 jun"))
                .andExpect(jsonPath("$.facts[1].label").value("porciones por completar"))
                .andExpect(jsonPath("$.facts[1].tone").value("HIGHLIGHT"))
                .andExpect(jsonPath("$.sources[0].label").value(
                        "Modelo de frío de Erez con el clima horario de Open-Meteo, hasta el 24 de junio"))
                .andExpect(jsonPath("$.actions", hasSize(0)))
                .andReturn().getResponse().getContentAsString();
        String answerId = JsonPath.read(answer, "$.answerId");

        answer("chill.reach_30", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", startsWith("Al ritmo reciente, tu lote llegaría a las 30 porciones el ")))
                .andExpect(jsonPath("$.facts[0].label").value("fecha estimada"))
                .andExpect(jsonPath("$.facts[1].value").value("31 ago"))
                .andExpect(jsonPath("$.facts[1].label").value("brotación"));
        answer("chill.vs_last_winter", plotId, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", startsWith("By June 24 your plot has ")))
                .andExpect(jsonPath("$.text", containsString(
                        "last winter, on the same date, it had 0. That winter closed with 0 portions, short of 30.")));

        // The latest answer of the screen is found by the chill. prefix of its questions (the fixed clock gives the
        // three answers the same time, so any of them is the latest)
        mvc.perform(get(ANSWERS + "/latest").param("screen", "WINTER_CHILL").param("plotId", plotId)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId", startsWith("chill.")))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"));
        mvc.perform(get(ANSWERS + "/{answerId}", answerId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("chill.missing"));
    }

    @Test
    @DisplayName("weather unavailable: every question disabled with its reason, and answering one is a 409")
    void weatherUnavailable() throws Exception {
        when(temperatureProvider.fetchHourlyTemperatures(anyDouble(), anyDouble(), any(LocalDate.class),
                any(LocalDate.class))).thenReturn(Optional.empty());
        var plotId = createPlot();

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[*].enabled").value(everyItem(is(false))))
                .andExpect(jsonPath("$.questions[0].disabledReason").value("Sin datos de frío para este lote todavía"))
                .andExpect(jsonPath("$.questions[1].label").value("¿Cuándo completo el frío?"))
                .andExpect(jsonPath("$.questions[3].disabledReason").value("Sin datos de frío para este lote todavía"));
        catalog(plotId, "en")
                .andExpect(jsonPath("$.questions[1].disabledReason").value("No chill data for this plot yet"));
        answer("chill.reach_30", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"));
    }

    @Test
    @DisplayName("WINTER_CHILL is a plot screen: 400 without plotId, 404 for an unknown plot")
    void requiresPlot() throws Exception {
        mvc.perform(get(QUESTIONS).param("screen", "WINTER_CHILL").header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Indica el lote de esta pantalla"));
        mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"chill.missing\",\"clientRequestId\":\"%s\"}"
                                .formatted(UUID.randomUUID()))
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isBadRequest());
        catalog(UUID.randomUUID().toString(), "es").andExpect(status().isNotFound());
    }

    private ResultActions catalog(String plotId, String language) throws Exception {
        return mvc.perform(get(QUESTIONS).param("screen", "WINTER_CHILL").param("plotId", plotId)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private ResultActions answer(String questionId, String plotId, String language) throws Exception {
        var body = "{\"questionId\":\"%s\",\"plotId\":\"%s\",\"clientRequestId\":\"%s\",\"forceRefresh\":false}"
                .formatted(questionId, plotId, UUID.randomUUID());
        return mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private static List<HourlyTemperature> days(LocalDate from, int count, double celsius) {
        var hours = new ArrayList<HourlyTemperature>();
        for (int day = 0; day < count; day++) {
            for (int hour = 0; hour < 24; hour++) {
                hours.add(new HourlyTemperature(from.plusDays(day).atTime(hour, 0), celsius));
            }
        }
        return hours;
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
