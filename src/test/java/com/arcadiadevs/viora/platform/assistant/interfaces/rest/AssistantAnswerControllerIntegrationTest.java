package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.assistant.domain.repositories.AssistantAnswerRepository;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade.PlotBearingIndex;
import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.CampaignThinningPlan;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real controller, services, ACL, security and H2 persistence: the answers of the assistant (P115-P119, P121-P122).
 * The harvest history and the prescription are stubbed at their ACL facades, so the figures of the answers are exact.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantanswers;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant answers (/assistant/answers)")
class AssistantAnswerControllerIntegrationTest {

    private static final String ANSWERS = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
    @Autowired OrchardContextFacade orchardContextFacade;
    @Autowired AssistantAnswerRepository answers;
    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    @MockitoBean PhenologyContextFacade phenologyContextFacade;
    @MockitoBean ThinningContextFacade thinningContextFacade;
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
    @DisplayName("answers a question with the real figures of the plot, written in Spanish")
    void answersWithTheRealFigures() throws Exception {
        var plotId = createPlotWithHistory(4.0, 10.0, 5.0);

        ask("alternation.why_index", plotId, UUID.randomUUID(), "es", false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").isNotEmpty())
                .andExpect(jsonPath("$.questionId").value("alternation.why_index"))
                .andExpect(jsonPath("$.question").value("¿Por qué mi índice es 0,50?"))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"))
                .andExpect(jsonPath("$.dataVersion").value(startsWith("sha256:")))
                .andExpect(jsonPath("$.text").value("Tu índice de vecería es 0,50: vecería moderada. "
                        + "Tu mejor campaña fue de 10,0 t/ha (2024) y la más baja de 4,0 t/ha (2023)."))
                .andExpect(jsonPath("$.facts", hasSize(2)))
                .andExpect(jsonPath("$.facts[0].value").value("0,50"))
                .andExpect(jsonPath("$.facts[0].tone").value("PRIMARY"))
                .andExpect(jsonPath("$.facts[1].value").value("10,0 → 4,0"))
                .andExpect(jsonPath("$.facts[1].tone").value("HIGHLIGHT"))
                .andExpect(jsonPath("$.sources[0].label").value("Basado en tus 3 campañas registradas"))
                .andExpect(jsonPath("$.actions", hasSize(0)))
                .andExpect(jsonPath("$.model.provider").value("TEMPLATE"))
                .andExpect(jsonPath("$.model.fallback").value(false));
    }

    @Test
    @DisplayName("Accept-Language changes the whole answer, numbers included")
    void answersInEnglish() throws Exception {
        var plotId = createPlotWithHistory(4.0, 10.0, 5.0);

        ask("alternation.why_index", plotId, UUID.randomUUID(), "en", false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(startsWith("Your alternate bearing index is 0.50: moderate "
                        + "alternate bearing.")))
                .andExpect(jsonPath("$.question").value("Why is my index 0.50?"))
                .andExpect(jsonPath("$.sources[0].label").value("Based on your 3 recorded campaigns"));
    }

    @Test
    @DisplayName("answers a thinning plan question with the prescription, and opens the screen with a button")
    void answersAPlanQuestion() throws Exception {
        var plotId = createPlot();
        givenPrescription();

        ask("plan.why_percent", plotId, UUID.randomUUID(), "es", false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Tu carga está en 0,60 frutos por brote y el árbol sostiene 0,40. "
                        + "Quitar 30 % de los frutos la deja en el objetivo."))
                .andExpect(jsonPath("$.facts[1].value").value("−30 %"))
                .andExpect(jsonPath("$.facts[1].pill").value("ventana hasta el 14 nov"))
                .andExpect(jsonPath("$.sources[0].label").value("Basado en 5 árboles muestreados"))
                .andExpect(jsonPath("$.actions[0].type").value("DEEPLINK"))
                .andExpect(jsonPath("$.actions[0].label").value("Registrar aclareo"))
                .andExpect(jsonPath("$.actions[0].target").value("thinning/execute?plotId=" + plotId));
    }

    @Test
    @DisplayName("the same clientRequestId gets the same answer; the same data reuses it; forceRefresh writes a new one")
    void idempotencyCacheAndRefresh() throws Exception {
        var plotId = createPlotWithHistory(4.0, 10.0, 5.0);
        var request = UUID.randomUUID();

        var first = answerId(ask("alternation.vs_campaigns", plotId, request, "es", false));
        var replay = answerId(ask("alternation.vs_campaigns", plotId, request, "es", false));
        var sameData = answerId(ask("alternation.vs_campaigns", plotId, UUID.randomUUID(), "es", false));
        var refreshed = answerId(ask("alternation.vs_campaigns", plotId, UUID.randomUUID(), "es", true));
        var otherLanguage = answerId(ask("alternation.vs_campaigns", plotId, UUID.randomUUID(), "en", false));

        assertEquals(first, replay);
        assertEquals(first, sameData);
        assertNotEquals(first, refreshed);
        assertNotEquals(first, otherLanguage);
    }

    @Test
    @DisplayName("an answer goes stale when the data of the plot changes, and asking again writes a new one")
    void freshnessFollowsTheData() throws Exception {
        var plotId = createPlotWithHistory(4.0, 10.0, 5.0);
        var first = answerId(ask("alternation.vs_campaigns", plotId, UUID.randomUUID(), "es", false));
        getAnswer(first).andExpect(jsonPath("$.freshness").value("UP_TO_DATE"));

        givenHistory(plotId, 4.0, 10.0, 5.0, 8.0);

        getAnswer(first).andExpect(jsonPath("$.freshness").value("STALE"));
        mvc.perform(get(ANSWERS).param("limit", "10"))
                .andExpect(jsonPath("$.answers[0].freshness").value("STALE"));
        var second = answerId(ask("alternation.vs_campaigns", plotId, UUID.randomUUID(), "es", false));
        assertNotEquals(first, second);
        getAnswer(second).andExpect(jsonPath("$.freshness").value("UP_TO_DATE"));
    }

    @Test
    @DisplayName("lists the latest answers newest first, honours the limit and reopens the last of a screen")
    void historyAndLatest() throws Exception {
        var plotId = createPlotWithHistory(4.0, 10.0, 5.0);
        givenPrescription();
        ask("alternation.why_index", plotId, UUID.randomUUID(), "es", false);
        ask("alternation.vs_campaigns", plotId, UUID.randomUUID(), "es", false);
        ask("plan.if_none", plotId, UUID.randomUUID(), "es", false);

        mvc.perform(get(ANSWERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers", hasSize(3)))
                .andExpect(jsonPath("$.answers[0].questionId").value("plan.if_none"))
                .andExpect(jsonPath("$.answers[0].question").value("¿Y si no aclaro?"))
                .andExpect(jsonPath("$.answers[2].questionId").value("alternation.why_index"));
        mvc.perform(get(ANSWERS).param("limit", "2"))
                .andExpect(jsonPath("$.answers", hasSize(2)));
        mvc.perform(get(ANSWERS + "/latest").param("screen", "ALTERNATION").param("plotId", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("alternation.vs_campaigns"));
        mvc.perform(get(ANSWERS + "/latest").param("screen", "PLAN").param("plotId", plotId))
                .andExpect(jsonPath("$.questionId").value("plan.if_none"));
    }

    @Test
    @DisplayName("a screen without answers answers 404 on latest, and an answer of another producer is not found")
    void notFound() throws Exception {
        var plotId = createPlotWithHistory(4.0, 10.0, 5.0);
        var id = answerId(ask("alternation.why_index", plotId, UUID.randomUUID(), "es", false));

        mvc.perform(get(ANSWERS + "/latest").param("screen", "PLAN").param("plotId", plotId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANSWER_NOT_FOUND"));

        authenticateAs(UUID.randomUUID().toString());
        getAnswer(id).andExpect(status().isNotFound());
        mvc.perform(get(ANSWERS)).andExpect(jsonPath("$.answers", hasSize(0)));
    }

    @Test
    @DisplayName("an unknown question, a question of another screen's namespace or a malformed id answers 400")
    void rejectsInvalidRequests() throws Exception {
        var plotId = createPlotWithHistory(4.0, 10.0, 5.0);

        ask("alternation.nothing", plotId, UUID.randomUUID(), "es", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La pregunta no es válida para esta pantalla"));
        ask("home.week_plan", plotId, UUID.randomUUID(), "es", false).andExpect(status().isBadRequest());
        ask("Not a question", plotId, UUID.randomUUID(), "es", false).andExpect(status().isBadRequest());
        mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON).header(HttpHeaders.ACCEPT_LANGUAGE, "es")
                        .content("{\"questionId\":\"alternation.why_index\",\"plotId\":\"%s\",\"clientRequestId\":\"nope\"}"
                                .formatted(plotId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El identificador de la solicitud no es válido"));
        mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"alternation.why_index\",\"clientRequestId\":\"%s\"}"
                                .formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
        mvc.perform(get(ANSWERS).param("limit", "0")).andExpect(status().isBadRequest());
        mvc.perform(get(ANSWERS).param("limit", "11")).andExpect(status().isBadRequest());
        mvc.perform(get(ANSWERS + "/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a question without enough data answers 409 and is not saved")
    void rejectsAQuestionWithoutData() throws Exception {
        var plotId = createPlot();

        ask("plan.why_percent", plotId, UUID.randomUUID(), "es", false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Aún no hay datos suficientes para responder esta pregunta"));
        ask("alternation.why_index", plotId, UUID.randomUUID(), "es", false).andExpect(status().isConflict());
        mvc.perform(get(ANSWERS)).andExpect(jsonPath("$.answers", hasSize(0)));
    }

    @Test
    @DisplayName("another producer's plot answers 403 and an unknown plot 404")
    void protectsThePlot() throws Exception {
        var plotId = createPlotWithHistory(4.0, 10.0, 5.0);
        ask("alternation.why_index", UUID.randomUUID().toString(), UUID.randomUUID(), "es", false)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLOT_NOT_FOUND"));

        authenticateAs(UUID.randomUUID().toString());
        ask("alternation.why_index", plotId, UUID.randomUUID(), "es", false)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        mvc.perform(get(ANSWERS + "/latest").param("screen", "ALTERNATION").param("plotId", plotId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("without an active plan every route answers 403")
    void requiresAnActivePlan() throws Exception {
        var plotId = createPlotWithHistory(4.0, 10.0, 5.0);
        var id = answerId(ask("alternation.why_index", plotId, UUID.randomUUID(), "es", false));
        when(subscriptionContextFacade.findEntitledQuotaHectares(producer)).thenReturn(Optional.empty());

        ask("alternation.why_index", plotId, UUID.randomUUID(), "es", false)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ASSISTANT_FORBIDDEN"));
        mvc.perform(get(ANSWERS)).andExpect(status().isForbidden());
        getAnswer(id).andExpect(status().isForbidden());
        mvc.perform(get(ANSWERS + "/latest").param("screen", "ALTERNATION").param("plotId", plotId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deleting the account deletes its answers")
    void accountDeletionRemovesTheAnswers() throws Exception {
        var plotId = createPlotWithHistory(4.0, 10.0, 5.0);
        ask("alternation.why_index", plotId, UUID.randomUUID(), "es", false).andExpect(status().isOk());
        assertEquals(1, answers.findRecent(producer, Instant.EPOCH, 10).size());

        // The account deletion publishes the event inside its own transaction, and the purgers join it
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                events.publishEvent(new UserAccountDeletedEvent(producer, "producer@viora.test", Instant.now())));

        assertTrue(answers.findRecent(producer, Instant.EPOCH, 10).isEmpty());
    }

    // ---- Helpers -------------------------------------------------------------------------------------------------

    private ResultActions ask(String questionId, String plotId, UUID requestId, String language, boolean forceRefresh)
            throws Exception {
        return mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .content("{\"questionId\":\"%s\",\"plotId\":\"%s\",\"clientRequestId\":\"%s\",\"forceRefresh\":%s}"
                        .formatted(questionId, plotId, requestId, forceRefresh)));
    }

    private ResultActions getAnswer(String answerId) throws Exception {
        return mvc.perform(get(ANSWERS + "/" + answerId));
    }

    private String answerId(ResultActions result) throws Exception {
        var body = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.answerId");
    }

    private static void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    /** Campaigns 2023, 2024, 2025 (and 2026 when a fourth yield is given) with these tonnes per hectare. */
    private void givenHistory(String plotId, double... tonnesPerHa) {
        double area = orchardContextFacade.findPlotAreaHectares(plotId).orElseThrow();
        var yields = new TreeMap<Integer, Double>();
        for (int i = 0; i < tonnesPerHa.length; i++) {
            yields.put(2023 + i, tonnesPerHa[i] * area * 1000.0);
        }
        when(phenologyContextFacade.findHistoricalYields(plotId)).thenReturn(yields);
        when(phenologyContextFacade.findBiennialBearingIndex(plotId))
                .thenReturn(Optional.of(new PlotBearingIndex(0.50, "MODERATE_ALTERNATION")));
    }

    private void givenPrescription() {
        when(thinningContextFacade.findCampaignThinningPlan(anyString(), anyInt())).thenReturn(Optional.of(
                new CampaignThinningPlan(0.60, 0.40, 30.0, LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 14))));
        when(thinningContextFacade.findPrescriptionAvailability(anyString(), anyInt())).thenReturn(Optional.of(
                new PrescriptionAvailability(true, 30.0, 5, 0, null)));
    }

    private String createPlotWithHistory(double... tonnesPerHa) throws Exception {
        var plotId = createPlot();
        givenHistory(plotId, tonnesPerHa);
        return plotId;
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
