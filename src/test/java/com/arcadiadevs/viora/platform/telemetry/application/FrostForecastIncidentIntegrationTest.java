package com.arcadiadevs.viora.platform.telemetry.application;

import com.arcadiadevs.viora.platform.telemetry.application.internal.commandservices.FrostForecastMonitor;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.WeatherForecastProvider;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastDay;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.AmbientTemperature;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.Percentage;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.WindSpeed;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A frost in the forecast ends as a stored incident, whether the producer opens the forecast or the monitor runs. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:frostforecast;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class FrostForecastIncidentIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired AgroclimaticIncidentRepository incidentRepository;
    @Autowired FrostForecastMonitor monitor;
    @MockitoBean WeatherForecastProvider forecastProvider;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("550e8400-e29b-41d4-a716-446655440000", null, List.of()));
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void openingTheForecastStoresTheFrostWarning() throws Exception {
        var plotId = createPlot();
        forecastWithMinimum(-3.0);

        mvc.perform(get("/api/v1/plots/" + plotId + "/forecasts")).andExpect(status().isOk());

        var incident = incidentRepository.findActiveByPlotIdAndType(new PlotId(plotId), IncidentType.FROST_WARNING);
        assertThat(incident).isPresent();
        assertThat(incident.get().snapshot().severity()).isEqualTo(IncidentSeverity.CRITICAL);
    }

    @Test
    void theMonitorStoresTheFrostWarningWithoutAnybodyOpeningTheForecast() throws Exception {
        var plotId = createPlot();
        forecastWithMinimum(1.2);

        monitor.checkAllPlots();

        assertThat(incidentRepository.findActiveByPlotIdAndType(new PlotId(plotId), IncidentType.FROST_WARNING))
                .isPresent();
    }

    @Test
    void withoutARealForecastNothingIsInventedAndTheAnswerIs503() throws Exception {
        var plotId = createPlot();
        when(forecastProvider.fetchSevenDayForecast(anyDouble(), anyDouble())).thenReturn(List.of());

        mvc.perform(get("/api/v1/plots/" + plotId + "/forecasts")).andExpect(status().isServiceUnavailable());
    }

    private void forecastWithMinimum(double minimum) {
        var day = WeatherForecastDay.create(LocalDate.now().plusDays(1), new AmbientTemperature(18.0),
                new AmbientTemperature(minimum), new Percentage(0.0), new WindSpeed(5.0), Instant.now());
        when(forecastProvider.fetchSevenDayForecast(anyDouble(), anyDouble())).thenReturn(List.of(day));
    }

    private String createPlot() throws Exception {
        var payload = """
                {"name":"Cuartel %s","variety":"CRIOLLA",
                 "polygonGeoJson":"{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                 "rowSpacingM":7.0,"treeSpacingM":5.0}
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        var response = mvc.perform(post("/api/v1/plots").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
