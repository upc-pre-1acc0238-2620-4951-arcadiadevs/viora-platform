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
import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real controller, services, facades and H2 persistence: the template answers of the assistant (P116-P119). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantanswers;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant answers (POST/GET /assistant/answers)")
class AssistantAnswerControllerIntegrationTest {

    private static final String ROUTE = "/api/v1/assistant/answers";

    @Autowired WebApplicationContext context;
    @Autowired Clock clock;
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
    @DisplayName("A key served from the cache still returns that answer after a newer one exists, and stays bound")
    void cachedKeyStaysBound() throws Exception {
        var plotId = createPlotWithTwoHarvests();
        var first = ask("alternation.why_index", plotId, UUID.randomUUID().toString(), false, "es")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String answerId = JsonPath.read(first, "$.answerId");
        var key = UUID.randomUUID().toString();
        ask("alternation.why_index", plotId, key, false, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(answerId));
        // A newer answer with the same facts now heads the cache
        ask("alternation.why_index", plotId, UUID.randomUUID().toString(), true, "es")
                .andExpect(status().isCreated());

        ask("alternation.why_index", plotId, key, false, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(answerId));
        ask("alternation.vs_campaigns", plotId, key, false, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_REQUEST_CONFLICT"));
    }

    @Test
    @DisplayName("Creates a template answer (201) and replays it for the same clientRequestId (200)")
    void createsThenReplays() throws Exception {
        var plotId = createPlotWithTwoHarvests();
        var key = UUID.randomUUID().toString();

        var created = ask("alternation.why_index", plotId, key, false, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answerId").isNotEmpty())
                .andExpect(jsonPath("$.questionId").value("alternation.why_index"))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.dataVersion", startsWith("sha256:")))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"))
                .andExpect(jsonPath("$.text", startsWith("Tu índice de vecería es 0,50, lo que indica")))
                .andExpect(jsonPath("$.facts.length()").value(2))
                .andExpect(jsonPath("$.facts[0].value").value("0,50"))
                .andExpect(jsonPath("$.facts[0].label").value("índice de vecería"))
                .andExpect(jsonPath("$.facts[0].tone").value("PRIMARY"))
                .andExpect(jsonPath("$.facts[1].value").value("3.000 kg"))
                .andExpect(jsonPath("$.facts[1].tone").value("HIGHLIGHT"))
                .andExpect(jsonPath("$.sources[0].label").value("Basado en 2 campañas registradas"))
                .andExpect(jsonPath("$.actions.length()").value(0))
                .andExpect(jsonPath("$.model.provider").value("template"))
                .andExpect(jsonPath("$.model.name").value("template-v1"))
                .andExpect(jsonPath("$.model.fallback").value(true))
                .andReturn().getResponse().getContentAsString();
        String answerId = JsonPath.read(created, "$.answerId");

        ask("alternation.why_index", plotId, key, false, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(answerId))
                .andExpect(jsonPath("$.text").value((String) JsonPath.read(created, "$.text")));
        // Another request with the same facts and language is served from the cache
        ask("alternation.why_index", plotId, UUID.randomUUID().toString(), false, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(answerId));
        // forceRefresh builds a new one
        ask("alternation.why_index", plotId, UUID.randomUUID().toString(), true, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answerId").value(org.hamcrest.Matchers.not(answerId)));
    }

    @Test
    @DisplayName("Accept-Language writes the text, cards and sources in English with decimal point")
    void answersInEnglish() throws Exception {
        var plotId = createPlotWithTwoHarvests();
        int lastYear = Year.now(clock.withZone(ProducerTimeZone.ZONE)).getValue() - 1;

        ask("alternation.vs_campaigns", plotId, UUID.randomUUID().toString(), false, "en")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Your " + lastYear + " campaign closed at 3,000 kg, 200% above "
                        + "your previous campaign (1,000 kg). On a plot with alternate bearing, high and low years "
                        + "tend to alternate."))
                .andExpect(jsonPath("$.facts[0].value").value("3,000 kg"))
                .andExpect(jsonPath("$.facts[1].value").value("+200%"))
                .andExpect(jsonPath("$.sources[0].label").value("Based on 2 recorded campaigns"));
        ask("alternation.vs_campaigns", plotId, UUID.randomUUID().toString(), false, "es")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Tu campaña " + lastYear + " cerró con 3.000 kg, un 200 % más "
                        + "que tu campaña anterior (1.000 kg). En un lote con vecería, los años altos y bajos suelen "
                        + "alternarse."));
    }

    @Test
    @DisplayName("Unknown or foreign questionId, missing or malformed plotId or clientRequestId answer 400")
    void rejectsInvalidRequests() throws Exception {
        var plotId = createPlot();
        var key = UUID.randomUUID().toString();

        for (var questionId : List.of("weather.today", "plan.unknown", "NOT A QUESTION")) {
            ask(questionId, plotId, key, false, "es")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.detail").value("La pregunta del asistente no es válida"));
        }
        send("{\"questionId\":\"plan.why_percent\",\"clientRequestId\":\"" + key + "\"}", "es")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Indica el lote de esta pantalla"));
        ask("plan.why_percent", "not-a-uuid", key, false, "es")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El identificador del lote no es válido"));
        ask("plan.why_percent", plotId, "not-a-uuid", false, "es")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("El identificador de la solicitud no es válido"));
        send("{\"questionId\":\"plan.why_percent\",\"plotId\":\"" + plotId + "\"}", "es")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El identificador de la solicitud no es válido"));
    }

    @Test
    @DisplayName("Another producer's plot answers 403")
    void rejectsOtherProducersPlot() throws Exception {
        var plotId = createPlotWithTwoHarvests();
        authenticateAs(UUID.randomUUID().toString());

        ask("alternation.why_index", plotId, UUID.randomUUID().toString(), false, "es")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        mvc.perform(get(ROUTE + "/latest").param("screen", "ALTERNATION").param("plotId", plotId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
    }

    @Test
    @DisplayName("Without an active plan every route answers 403")
    void rejectsWithoutActivePlan() throws Exception {
        var plotId = createPlotWithTwoHarvests();
        when(subscriptionContextFacade.findEntitledQuotaHectares(producer)).thenReturn(Optional.empty());

        ask("alternation.why_index", plotId, UUID.randomUUID().toString(), false, "es")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ASSISTANT_FORBIDDEN"))
                .andExpect(jsonPath("$.detail").value("Necesitas un plan activo para usar el asistente"));
        mvc.perform(get(ROUTE).header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ASSISTANT_FORBIDDEN"));
        mvc.perform(get(ROUTE + "/" + UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ASSISTANT_FORBIDDEN"));
        mvc.perform(get(ROUTE + "/latest").param("screen", "PLAN").param("plotId", plotId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ASSISTANT_FORBIDDEN"));
    }

    @Test
    @DisplayName("An unknown plot answers 404")
    void rejectsUnknownPlot() throws Exception {
        ask("alternation.why_index", UUID.randomUUID().toString(), UUID.randomUUID().toString(), false, "es")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLOT_NOT_FOUND"));
    }

    @Test
    @DisplayName("A disabled question answers 409 with the catalog's reason, and stores nothing")
    void rejectsDisabledQuestion() throws Exception {
        var plotId = createPlotWithTwoHarvests();

        ask("plan.why_percent", plotId, UUID.randomUUID().toString(), false, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_QUESTION_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Sin plan de aclareo activo"))
                .andExpect(jsonPath("$.reason").value("assistant.question.unavailable"));
        ask("alternation.next_year", plotId, UUID.randomUUID().toString(), false, "en")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Caliber data is missing · record a sampling"));
        mvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("History lists the answers newest first, reopens one, and /latest finds the screen's last one")
    void listsAndReopens() throws Exception {
        var plotId = createPlotWithTwoHarvests();
        mvc.perform(get(ROUTE + "/latest").param("screen", "ALTERNATION").param("plotId", plotId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        String first = JsonPath.read(ask("alternation.why_index", plotId, UUID.randomUUID().toString(), false, "es")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.answerId");
        String second = JsonPath.read(ask("alternation.vs_campaigns", plotId, UUID.randomUUID().toString(), false,
                "es").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.answerId");

        mvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].answerId").value(second))
                .andExpect(jsonPath("$[0].freshness").value("UP_TO_DATE"))
                .andExpect(jsonPath("$[1].answerId").value(first));
        mvc.perform(get(ROUTE).param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].answerId").value(second));
        for (var limit : List.of("0", "11", "ten")) {
            mvc.perform(get(ROUTE).param("limit", limit).header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("El límite debe estar entre 1 y 10"));
        }
        mvc.perform(get(ROUTE + "/" + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(first))
                .andExpect(jsonPath("$.questionId").value("alternation.why_index"))
                .andExpect(jsonPath("$.facts.length()").value(2));
        mvc.perform(get(ROUTE + "/latest").param("screen", "ALTERNATION").param("plotId", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(second));
        mvc.perform(get(ROUTE + "/latest").param("screen", "PLAN").param("plotId", plotId))
                .andExpect(status().isNoContent());

        // A new harvest changes the facts: the stored answers turn stale
        recordHarvest(plotId, Year.now(clock.withZone(ProducerTimeZone.ZONE)).getValue() - 3, 2000.0);
        mvc.perform(get(ROUTE + "/" + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freshness").value("STALE"));

        // Answers of another producer are not found, never forbidden
        authenticateAs(UUID.randomUUID().toString());
        mvc.perform(get(ROUTE + "/" + first))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ASSISTANT_ANSWER_NOT_FOUND"));
        mvc.perform(get(ROUTE + "/not-a-uuid"))
                .andExpect(status().isNotFound());
        mvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private ResultActions ask(String questionId, String plotId, String clientRequestId, boolean forceRefresh,
            String language) throws Exception {
        return send("""
                {"questionId":"%s","plotId":"%s","clientRequestId":"%s","forceRefresh":%s}
                """.formatted(questionId, plotId, clientRequestId, forceRefresh), language);
    }

    private ResultActions send(String body, String language) throws Exception {
        return mvc.perform(post(ROUTE)
                .contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private static void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    /** Two campaigns of 1000 and 3000 kg give a Hoblyn index of |3000 - 1000| / 4000 = 0.50. */
    private String createPlotWithTwoHarvests() throws Exception {
        var plotId = createPlot();
        int year = Year.now(clock.withZone(ProducerTimeZone.ZONE)).getValue();
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
