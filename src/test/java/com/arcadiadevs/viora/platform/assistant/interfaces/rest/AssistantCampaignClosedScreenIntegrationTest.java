package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.WeatherForecastProvider;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.ThinningOutcome;
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
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real controllers, services and H2: the CAMPAIGN_CLOSED screen (P73, P148-P150). The harvests are recorded through
 * the Phenology API and the plot area comes from its polygon; only the thinning outcome of the closed campaign is
 * stubbed at the Thinning facade.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantcampaignclosed;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant CAMPAIGN_CLOSED screen")
class AssistantCampaignClosedScreenIntegrationTest {

    private static final String QUESTIONS = "/api/v1/assistant/questions";
    private static final String ANSWERS = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
    @Autowired Clock clock;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    @MockitoBean WeatherForecastProvider weatherForecastProvider;
    @MockitoBean ThinningContextFacade thinningContextFacade;
    private MockMvc mvc;
    private int year;
    private final String producer = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        authenticateAs(producer);
        year = LocalDate.now(clock.withZone(ProducerTimeZone.ZONE)).getYear();
        when(subscriptionContextFacade.findEntitledQuotaHectares(anyString())).thenReturn(Optional.of(BigDecimal.TEN));
        when(weatherForecastProvider.fetchSevenDayForecast(anyDouble(), anyDouble())).thenReturn(List.of());
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Closed campaign with history: 8.3 t/ha, +36 % over the average; the plan for next campaign")
    void closedWithHistory() throws Exception {
        int closed = year - 1;
        var plot = createPlot();
        double area = plot.areaHa();
        // 5.8, 6.1 and 6.4 t/ha before, 8.3 t/ha in the closed campaign, over the area of the polygon
        recordHarvest(plot.id(), closed - 3, 5800.0 * area);
        recordHarvest(plot.id(), closed - 2, 6100.0 * area);
        recordHarvest(plot.id(), closed - 1, 6400.0 * area);
        recordHarvest(plot.id(), closed, 8300.0 * area);
        var executedOn = LocalDate.of(closed, 11, 3);
        when(thinningContextFacade.findThinningOutcome(eq(plot.id()), eq(closed))).thenReturn(Optional.of(
                new ThinningOutcome(executedOn, 30.0, "OPTIMAL", 0.57, 0.4, 0.4, "BALANCED", 9100.0, 8200.0,
                        "CALIBER_ADJUSTED", 3, "PROVISIONAL", 14, 49)));
        long lastKg = Math.round(8300.0 * area);

        catalog(plot.id(), "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("CAMPAIGN_CLOSED"))
                .andExpect(jsonPath("$.questions.length()").value(3))
                .andExpect(jsonPath("$.questions[0].questionId").value("campaign.vs_previous"))
                .andExpect(jsonPath("$.questions[0].label")
                        .value("¿Cómo fue la campaña %d frente a las anteriores?".formatted(closed)))
                .andExpect(jsonPath("$.questions[0].enabled").value(true))
                .andExpect(jsonPath("$.questions[1].questionId").value("campaign.caliber_change"))
                .andExpect(jsonPath("$.questions[1].label").value("¿Cambió el calibre frente a otras campañas?"))
                .andExpect(jsonPath("$.questions[1].enabled").value(false))
                .andExpect(jsonPath("$.questions[1].disabledReason")
                        .value("El calibre de cada campaña aún no llega al asistente"))
                .andExpect(jsonPath("$.questions[2].questionId").value("campaign.plan_next"))
                .andExpect(jsonPath("$.questions[2].label").value("¿Qué planifico para la campaña %d?".formatted(year)))
                .andExpect(jsonPath("$.questions[2].enabled").value(true));
        catalog(plot.id(), "en")
                .andExpect(jsonPath("$.questions[0].label")
                        .value("How did the %d campaign go against the earlier ones?".formatted(closed)))
                .andExpect(jsonPath("$.questions[1].disabledReason")
                        .value("The caliber of each campaign does not reach the assistant yet"))
                .andExpect(jsonPath("$.questions[2].label").value("What do I plan for the %d campaign?".formatted(year)));

        answer("campaign.vs_previous", plot.id(), "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value(("Tu campaña %d cerró con 8,3 t/ha (%s kg), un 36 %% más que el "
                        + "promedio de tus 3 campañas anteriores (6,1 t/ha).")
                        .formatted(closed, kg(lastKg, "es"))))
                .andExpect(jsonPath("$.facts[0].value").value("8,3 t/ha"))
                .andExpect(jsonPath("$.facts[0].pill").value("promedio 6,1 t/ha"))
                .andExpect(jsonPath("$.facts[1].value").value("+36 %"))
                .andExpect(jsonPath("$.facts[1].label").value("frente al promedio"));
        answer("campaign.vs_previous", plot.id(), "en")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value(("Your %d campaign closed at 8.3 t/ha (%s kg), 36%% above the "
                        + "average of your 3 previous campaigns (6.1 t/ha).").formatted(closed, kg(lastKg, "en"))));
        answer("campaign.plan_next", plot.id(), "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value(("Tu índice de vecería es 0,06 (producción regular). En la campaña "
                        + "%d registraste el aclareo el 3 de noviembre, dentro de la ventana. Para la campaña %d, "
                        + "registra la plena floración de tu lote, muestrea tus árboles y aclarea dentro de la ventana, "
                        + "que en el perfil de tu variedad va de 14 a 49 días después de la floración.")
                        .formatted(closed, year)))
                .andExpect(jsonPath("$.text", not(containsString("kg"))))
                .andExpect(jsonPath("$.facts[0].value").value(String.valueOf(year)))
                .andExpect(jsonPath("$.facts[1].value").value("0,06"))
                .andExpect(jsonPath("$.facts[1].pill").value("producción regular"));
        answer("campaign.plan_next", plot.id(), "en")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value(("Your alternate bearing index is 0.06 (regular bearing). In the %d "
                        + "campaign you recorded the thinning on November 3, within the window. For the %d campaign, "
                        + "record the full bloom of your plot, sample your trees and thin within the window, which in "
                        + "the profile of your variety runs from 14 to 49 days after bloom.").formatted(closed, year)));
        answer("campaign.caliber_change", plot.id(), "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El calibre de cada campaña aún no llega al asistente"));
    }

    @Test
    @DisplayName("One harvest: no comparison, the plan without index nor thinning; no harvest: everything waits")
    void oneOrNoHarvest() throws Exception {
        var plot = createPlot();
        recordHarvest(plot.id(), year - 1, 9000.0);

        catalog(plot.id(), "es")
                .andExpect(jsonPath("$.questions[0].enabled").value(false))
                .andExpect(jsonPath("$.questions[0].disabledReason").value("Registra al menos 2 campañas"))
                .andExpect(jsonPath("$.questions[2].enabled").value(true));
        answer("campaign.vs_previous", plot.id(), "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Registra al menos 2 campañas"));
        answer("campaign.plan_next", plot.id(), "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value(("En la campaña %d no registraste aclareo. Para la campaña %d, "
                        + "registra la plena floración de tu lote y muestrea tus árboles para que Viora calcule el plan "
                        + "de aclareo.").formatted(year - 1, year)))
                .andExpect(jsonPath("$.facts[1].value").value("Sin aclareo"))
                .andExpect(jsonPath("$.facts[1].label").value("aclareo " + (year - 1)));

        var empty = createPlot();
        catalog(empty.id(), "es")
                .andExpect(jsonPath("$.questions[0].label").value("¿Cómo fue mi campaña frente a las anteriores?"))
                .andExpect(jsonPath("$.questions[0].disabledReason")
                        .value("Sin cosecha registrada · registra la cosecha de la campaña"))
                .andExpect(jsonPath("$.questions[1].enabled").value(false))
                .andExpect(jsonPath("$.questions[2].label").value("¿Qué planifico para la próxima campaña?"))
                .andExpect(jsonPath("$.questions[2].enabled").value(false));
        catalog(empty.id(), "en")
                .andExpect(jsonPath("$.questions[2].disabledReason")
                        .value("No harvest recorded · record the harvest of the campaign"));
        answer("campaign.plan_next", empty.id(), "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Sin cosecha registrada · registra la cosecha de la campaña"));
    }

    @Test
    @DisplayName("CAMPAIGN_CLOSED requires its plot; a plot of another producer answers 403")
    void requiresOwnPlot() throws Exception {
        mvc.perform(get(QUESTIONS).param("screen", "CAMPAIGN_CLOSED").header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Indica el lote de esta pantalla"));
        authenticateAs(UUID.randomUUID().toString());
        var foreign = createPlot();
        authenticateAs(producer);
        catalog(foreign.id(), "es").andExpect(status().isForbidden());
        answer("campaign.plan_next", foreign.id(), "es").andExpect(status().isForbidden());
    }

    private static String kg(long kilograms, String language) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag(language)).format(kilograms);
    }

    private void recordHarvest(String plotId, int campaignYear, double totalKg) throws Exception {
        mvc.perform(post("/api/v1/plots/" + plotId + "/harvest-records").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campaignYear\":%d,\"totalYieldKg\":%s}".formatted(campaignYear, totalKg)))
                .andExpect(status().isCreated());
    }

    private ResultActions catalog(String plotId, String language) throws Exception {
        return mvc.perform(get(QUESTIONS).param("screen", "CAMPAIGN_CLOSED").param("plotId", plotId)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private ResultActions answer(String questionId, String plotId, String language) throws Exception {
        var body = "{\"questionId\":\"%s\",\"plotId\":\"%s\",\"clientRequestId\":\"%s\"}"
                .formatted(questionId, plotId, UUID.randomUUID());
        return mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private static void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private record CreatedPlot(String id, double areaHa) {
    }

    private CreatedPlot createPlot() throws Exception {
        var payload = """
                {"name":"Cuartel %s","variety":"SEVILLANA",
                 "polygonGeoJson":"{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                 "rowSpacingM":7.0,"treeSpacingM":5.0}
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        var response = mvc.perform(post("/api/v1/plots").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new CreatedPlot(JsonPath.read(response, "$.id"), ((Number) JsonPath.read(response, "$.areaHa"))
                .doubleValue());
    }
}
