package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real controller, services, facades and H2 persistence: the question catalog of the assistant (P114, P120). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantquestions;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant question catalog (GET /assistant/questions)")
class AssistantQuestionControllerIntegrationTest {

    private static final String ROUTE = "/api/v1/assistant/questions";
    private static final int NEXT_YEAR = Year.now(ProducerTimeZone.ZONE).getValue() + 1;

    @Autowired WebApplicationContext context;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    private MockMvc mvc;
    private final String producer = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        authenticateAs(producer);
        when(subscriptionContextFacade.findEntitledQuotaHectares(anyString())).thenReturn(Optional.of(BigDecimal.TEN));
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("ALTERNATION lists its four questions in order, the index in the label and next year disabled")
    void listsAlternationQuestions() throws Exception {
        var plotId = createPlotWithTwoHarvests();

        ask("ALTERNATION", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("ALTERNATION"))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(jsonPath("$.questions[0].questionId").value("alternation.why_index"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Por qué mi índice es 0,50?"))
                .andExpect(jsonPath("$.questions[0].icon").value("calendar"))
                .andExpect(jsonPath("$.questions[0].enabled").value(true))
                .andExpect(jsonPath("$.questions[0].disabledReason").value(nullValue()))
                .andExpect(jsonPath("$.questions[1].questionId").value("alternation.break_cycle"))
                .andExpect(jsonPath("$.questions[1].icon").value("pruning"))
                .andExpect(jsonPath("$.questions[1].enabled").value(true))
                .andExpect(jsonPath("$.questions[2].questionId").value("alternation.vs_campaigns"))
                .andExpect(jsonPath("$.questions[2].icon").value("history"))
                .andExpect(jsonPath("$.questions[2].enabled").value(true))
                .andExpect(jsonPath("$.questions[3].questionId").value("alternation.next_year"))
                .andExpect(jsonPath("$.questions[3].label").value("¿Cuánto cosecharé en " + NEXT_YEAR + "?"))
                .andExpect(jsonPath("$.questions[3].icon").value("harvest"))
                .andExpect(jsonPath("$.questions[3].enabled").value(false))
                .andExpect(jsonPath("$.questions[3].disabledReason")
                        .value("Faltan datos de calibre · registra un muestreo"));
    }

    @Test
    @DisplayName("Accept-Language changes the labels and the number format")
    void localizesByAcceptLanguage() throws Exception {
        var plotId = createPlotWithTwoHarvests();

        ask("ALTERNATION", plotId, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].label").value("Why is my index 0.50?"))
                .andExpect(jsonPath("$.questions[3].disabledReason").value("Caliber data is missing · record a sampling"));
        ask("ALTERNATION", plotId, "es")
                .andExpect(jsonPath("$.questions[0].label").value("¿Por qué mi índice es 0,50?"));
    }

    @Test
    @DisplayName("PLAN without a prescription disables its four questions")
    void listsPlanQuestionsWithoutPrescription() throws Exception {
        var plotId = createPlot();

        ask("PLAN", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("PLAN"))
                .andExpect(jsonPath("$.questions[0].questionId").value("plan.why_percent"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Por qué ese porcentaje de aclareo?"))
                .andExpect(jsonPath("$.questions[0].icon").value("prio"))
                .andExpect(jsonPath("$.questions[1].questionId").value("plan.if_less"))
                .andExpect(jsonPath("$.questions[1].label").value("¿Y si aclaro menos?"))
                .andExpect(jsonPath("$.questions[2].questionId").value("plan.if_none"))
                .andExpect(jsonPath("$.questions[3].questionId").value("plan.best_time"))
                .andExpect(jsonPath("$.questions[0].enabled").value(false))
                .andExpect(jsonPath("$.questions[1].enabled").value(false))
                .andExpect(jsonPath("$.questions[2].enabled").value(false))
                .andExpect(jsonPath("$.questions[3].enabled").value(false))
                .andExpect(jsonPath("$.questions[3].disabledReason").value("Sin plan de aclareo activo"));
    }

    @Test
    @DisplayName("An unknown or missing screen answers 400")
    void rejectsUnknownScreen() throws Exception {
        var plotId = createPlot();

        ask("WEATHER", plotId, "es")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("La pantalla del asistente no es válida"));
        mvc.perform(get(ROUTE).param("plotId", plotId).header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("La pantalla del asistente no es válida"));
    }

    @Test
    @DisplayName("A plot screen without plotId, or with a malformed one, answers 400")
    void rejectsMissingOrMalformedPlot() throws Exception {
        mvc.perform(get(ROUTE).param("screen", "PLAN").header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("Indica el lote de esta pantalla"));
        ask("PLAN", "not-a-uuid", "es")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("El identificador del lote no es válido"));
    }

    @Test
    @DisplayName("An unknown plot answers 404")
    void rejectsUnknownPlot() throws Exception {
        ask("ALTERNATION", UUID.randomUUID().toString(), "es")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLOT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Another producer's plot answers 403")
    void rejectsOtherProducersPlot() throws Exception {
        var plotId = createPlot();
        authenticateAs(UUID.randomUUID().toString());

        ask("PLAN", plotId, "es")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
    }

    @Test
    @DisplayName("Without an active plan answers 403")
    void rejectsWithoutActivePlan() throws Exception {
        var plotId = createPlot();
        when(subscriptionContextFacade.findEntitledQuotaHectares(producer)).thenReturn(Optional.empty());

        ask("PLAN", plotId, "es")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ASSISTANT_FORBIDDEN"))
                .andExpect(jsonPath("$.detail").value("Necesitas un plan activo para usar el asistente"));
    }

    private ResultActions ask(String screen, String plotId, String language) throws Exception {
        return mvc.perform(get(ROUTE).param("screen", screen).param("plotId", plotId)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private static void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    /** Two campaigns of 1000 and 3000 kg give a Hoblyn index of |3000 - 1000| / 4000 = 0.50. */
    private String createPlotWithTwoHarvests() throws Exception {
        var plotId = createPlot();
        int year = Year.now(ProducerTimeZone.ZONE).getValue();
        recordHarvest(plotId, year - 2, 1000.0);
        recordHarvest(plotId, year - 1, 3000.0);
        return plotId;
    }

    private void recordHarvest(String plotId, int campaignYear, double totalKg) throws Exception {
        mvc.perform(post("/api/v1/plots/" + plotId + "/harvest-records").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campaignYear\":%d,\"totalYieldKg\":%s}".formatted(campaignYear, totalKg)))
                .andExpect(status().isCreated());
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
