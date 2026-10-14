package com.arcadiadevs.viora.platform.telemetry.interfaces.rest;

import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.weather.HourlyTemperatureProvider;
import com.arcadiadevs.viora.platform.phenology.domain.model.events.WinterWarmthEvaluatedEvent;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HourlyTemperature;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The warm winter alert end to end (US23): a winter warmth evaluation from Phenology opens one WARM_WINTER incident,
 * never two, the alert shows the daily maxima the rule saw, and the end of the spell normalizes it.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:warmwinter;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Warm winter alert (US23)")
class WarmWinterAlertIntegrationTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    @Autowired WebApplicationContext context;
    @Autowired ApplicationEventPublisher publisher;
    @MockitoBean HourlyTemperatureProvider temperatureProvider;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private WinterWarmthEvaluatedEvent evaluation(String plotId, String anomaly, int streak, Double latestMax) {
        return new WinterWarmthEvaluatedEvent(plotId, 2026, anomaly, streak, latestMax, 24.0,
                LocalDate.now(LIMA).minusDays(1), Instant.now());
    }

    @Test
    @DisplayName("An active spell opens one critical warm winter alert with its steps and the week of maxima")
    void activeSpellOpensOneAlert() throws Exception {
        var plotId = createPlot();
        when(temperatureProvider.fetchHourlyTemperatures(anyDouble(), anyDouble(), any(), any())).thenAnswer(call -> {
            LocalDate from = call.getArgument(2);
            LocalDate to = call.getArgument(3);
            var hours = new ArrayList<HourlyTemperature>();
            for (var day = from; !day.isAfter(to); day = day.plusDays(1)) {
                hours.add(new HourlyTemperature(day.atTime(6, 0), 8.0));
                hours.add(new HourlyTemperature(day.atTime(14, 0), 26.5));
            }
            return Optional.of(hours);
        });

        publisher.publishEvent(evaluation(plotId, "ACTIVE", 4, 26.5));
        publisher.publishEvent(evaluation(plotId, "ACTIVE", 5, 27.0));

        var list = mvc.perform(get("/api/v1/agroclimatic-incidents").param("plotId", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents.length()").value(1))
                .andExpect(jsonPath("$.incidents[0].type").value("WARM_WINTER"))
                .andExpect(jsonPath("$.incidents[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.incidents[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.incidents[0].metricName").value("DAILY_MAX_TEMPERATURE"))
                .andExpect(jsonPath("$.incidents[0].currentValue").value(26.5))
                .andExpect(jsonPath("$.incidents[0].thresholdValue").value(24.0))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(list, "$.incidents[0].id");

        mvc.perform(get("/api/v1/agroclimatic-incidents/{id}", id).header("Accept-Language", "es"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("warm_winter.headline"))
                .andExpect(jsonPath("$.mitigationSteps.length()").value(3))
                .andExpect(jsonPath("$.mitigationSteps[0].instructionKey").value("warm_winter.step.follow_chill"))
                .andExpect(jsonPath("$.mitigationSteps[0].instruction", org.hamcrest.Matchers.startsWith("Sigue tu frío invernal")))
                .andExpect(jsonPath("$.weeklyTrend.length()").value(7))
                .andExpect(jsonPath("$.weeklyTrend[*].value", everyItem(greaterThan(24.0))))
                .andExpect(jsonPath("$.weeklyTrend[0].threshold").value(24.0));
    }

    @Test
    @DisplayName("When the spell ends the alert is normalized")
    void endOfTheSpellNormalizesTheAlert() throws Exception {
        var plotId = createPlot();
        publisher.publishEvent(evaluation(plotId, "ACTIVE", 4, 25.0));

        publisher.publishEvent(evaluation(plotId, "RECORDED", 0, 18.0));

        mvc.perform(get("/api/v1/agroclimatic-incidents").param("plotId", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents.length()").value(1))
                .andExpect(jsonPath("$.incidents[0].status").value("NORMALIZED"));

        publisher.publishEvent(evaluation(plotId, "ACTIVE", 4, 25.5));
        mvc.perform(get("/api/v1/agroclimatic-incidents").param("plotId", plotId).param("status", "ACTIVE"))
                .andExpect(jsonPath("$.incidents.length()").value(1));
    }

    @Test
    @DisplayName("A winter without a spell raises nothing")
    void noSpellRaisesNothing() throws Exception {
        var plotId = createPlot();

        publisher.publishEvent(evaluation(plotId, "NONE", 2, 25.0));

        mvc.perform(get("/api/v1/agroclimatic-incidents").param("plotId", plotId))
                .andExpect(jsonPath("$.incidents.length()").value(0));
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
