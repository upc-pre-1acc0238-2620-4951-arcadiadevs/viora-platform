package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.WeatherForecastProvider;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastDay;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.AmbientTemperature;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.Percentage;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.WindSpeed;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.ThinningOutcome;
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
import static org.hamcrest.Matchers.not;
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
 * Real controllers, services and H2: the THINNING_DONE screen (P63, P145-P147). The thinning plan of the plot is
 * stubbed at the Thinning facade, the forecast at its provider.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantthinningdone;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant THINNING_DONE screen")
class AssistantThinningDoneScreenIntegrationTest {

    private static final String QUESTIONS = "/api/v1/assistant/questions";
    private static final String ANSWERS = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
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
                .thenReturn(forecast(30.0, 29.0, 28.0, 27.0, 26.0, 25.0, 24.0));
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Thinning recorded: the yield against without thinning, the demo target, the next sampling")
    void thinningRecorded() throws Exception {
        int year = today.getYear();
        var plotId = createPlot();
        var executedOn = today.minusDays(2);
        stubPlan(plotId, new ThinningPlanDetail(true, 30.0, 0.5714, 0.4, today.minusDays(10), today.plusDays(11), 5,
                today.minusDays(3), executedOn, "ESTIMATED", 180.4, "171/190", 8200.0), 0);
        when(thinningContextFacade.findThinningOutcome(eq(plotId), anyInt())).thenReturn(Optional.of(
                new ThinningOutcome(executedOn, 30.0, "OPTIMAL", 0.5714, 0.4, 0.4, "BALANCED", 11700.0, 8200.0,
                        "FRUIT_COUNT_ONLY", 3, "SYNTHETIC_DEMO", 14, 49)));

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("THINNING_DONE"))
                .andExpect(jsonPath("$.questions.length()").value(3))
                .andExpect(jsonPath("$.questions[0].questionId").value("thinning.harvest_change"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Cómo cambia mi cosecha?"))
                .andExpect(jsonPath("$.questions[1].questionId").value("thinning.why_target"))
                .andExpect(jsonPath("$.questions[1].label").value("¿Por qué el objetivo es 0,40?"))
                .andExpect(jsonPath("$.questions[2].questionId").value("thinning.next_sampling"))
                .andExpect(jsonPath("$.questions[0].enabled").value(true))
                .andExpect(jsonPath("$.questions[1].enabled").value(true))
                .andExpect(jsonPath("$.questions[2].enabled").value(true));

        answer("thinning.harvest_change", plotId, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Con la carga que dejó tu aclareo, Viora estima 8.200 kg por "
                        + "hectárea para esta campaña. Sin aclarear la cifra llegaría a 11.700 kg, pero es un techo: "
                        + "con más fruta el calibre baja y Viora aún no lo mide para tu variedad. La carga quedó en "
                        + "0,40 frutos por brote, dentro del objetivo de 0,40."))
                .andExpect(jsonPath("$.text", not(containsString(String.valueOf(year + 1)))))
                .andExpect(jsonPath("$.facts[0].value").value("8.200"))
                .andExpect(jsonPath("$.facts[0].pill").value("por número de frutos"))
                .andExpect(jsonPath("$.facts[1].label").value("frutos por brote tras el aclareo"));
        answer("thinning.why_target", plotId, "en")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("The target of 0.40 fruits per shoot is the load the tree can "
                        + "sustain according to the thinning profile of your variety. That profile is a demonstration "
                        + "one and no agronomist validated it. Your sampling measured 0.57, you removed 30 % of the "
                        + "fruits and the load ended at 0.40."))
                .andExpect(jsonPath("$.facts[0].pill").value("demonstration"))
                .andExpect(jsonPath("$.facts[1].pill").value("you removed 30 %"));
        answer("thinning.next_sampling", plotId, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text", startsWith("Ya registraste el aclareo el ")))
                .andExpect(jsonPath("$.text", endsWith(("así que esta campaña no pide más muestreos. En la campaña %d, "
                        + "registra la plena floración de tu lote y muestrea antes de que abra la ventana de aclareo, "
                        + "que en el perfil de tu variedad va de 14 a 49 días después de la floración.")
                        .formatted(year + 1))))
                .andExpect(jsonPath("$.facts[0].value").value(String.valueOf(year + 1)))
                .andExpect(jsonPath("$.facts[1].value").value("14–49"));
    }

    @Test
    @DisplayName("Before the thinning is recorded every question is disabled")
    void notRecorded() throws Exception {
        var plotId = createPlot();
        stubPlan(plotId, new ThinningPlanDetail(true, 30.0, 0.5714, 0.4, today.minusDays(10), today.plusDays(11), 5,
                today.minusDays(3), null, null, null, null, null), 0);

        catalog(plotId, "es")
                .andExpect(jsonPath("$.questions[0].enabled").value(false))
                .andExpect(jsonPath("$.questions[0].disabledReason").value("Sin aclareo registrado en esta campaña"))
                .andExpect(jsonPath("$.questions[1].label").value("¿Por qué ese objetivo de carga?"))
                .andExpect(jsonPath("$.questions[2].enabled").value(false));
        answer("thinning.next_sampling", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Sin aclareo registrado en esta campaña"));
    }

    @Test
    @DisplayName("THINNING_DONE requires its plot; a plot of another producer answers 403")
    void requiresOwnPlot() throws Exception {
        mvc.perform(get(QUESTIONS).param("screen", "THINNING_DONE").header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Indica el lote de esta pantalla"));
        authenticateAs(UUID.randomUUID().toString());
        var foreign = createPlot();
        authenticateAs(producer);
        catalog(foreign, "es").andExpect(status().isForbidden());
        answer("thinning.harvest_change", foreign, "es").andExpect(status().isForbidden());
    }

    private void stubPlan(String plotId, ThinningPlanDetail plan, int treesNeeded) {
        when(thinningContextFacade.findThinningPlanDetail(eq(plotId), anyInt())).thenReturn(Optional.of(plan));
        when(thinningContextFacade.findPrescriptionAvailability(eq(plotId), anyInt()))
                .thenReturn(Optional.of(new PrescriptionAvailability(plan.issued(), plan.percentageToRemove(),
                        plan.sampledTrees(), treesNeeded, plan.caliberProjectionStatus())));
    }

    private ResultActions catalog(String plotId, String language) throws Exception {
        return mvc.perform(get(QUESTIONS).param("screen", "THINNING_DONE").param("plotId", plotId)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private ResultActions answer(String questionId, String plotId, String language) throws Exception {
        var body = "{\"questionId\":\"%s\",\"plotId\":\"%s\",\"clientRequestId\":\"%s\"}"
                .formatted(questionId, plotId, UUID.randomUUID());
        return mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
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
