package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real controllers, services, Orchard facade and H2: the SAMPLING_SUMMARY screen (P54, P142-P144). The sampling and
 * the thinning plan of the plot are stubbed at the Thinning facade, with the window around the application day.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantsampling;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant SAMPLING_SUMMARY screen")
class AssistantSamplingSummaryScreenIntegrationTest {

    private static final String QUESTIONS = "/api/v1/assistant/questions";
    private static final String ANSWERS = "/api/v1/assistant/answers";
    private static final String NOT_ENOUGH_DATA = "Aún no hay datos suficientes para responder esta pregunta";

    @Autowired WebApplicationContext context;
    @Autowired Clock clock;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    @MockitoBean ThinningContextFacade thinningContextFacade;
    private MockMvc mvc;
    private LocalDate today;
    private final String producer = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        authenticateAs(producer);
        today = LocalDate.now(clock.withZone(ProducerTimeZone.ZONE));
        when(subscriptionContextFacade.findEntitledQuotaHectares(anyString())).thenReturn(Optional.of(BigDecimal.TEN));
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("contract example: 5/5 trees, 0,59 fruits per shoot against 0,40, window open; correct_tree disabled")
    void representativeSampling() throws Exception {
        var plotId = createPlot();
        stubPlan(plotId, new ThinningPlanDetail(true, 32.0, 0.59, 0.4, today.minusDays(10), today.plusDays(11), 5,
                today.minusDays(1), null, null, null, null, null), 0);

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("SAMPLING_SUMMARY"))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(jsonPath("$.questions[0].questionId").value("sampling.enough_trees"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Son suficientes árboles?"))
                .andExpect(jsonPath("$.questions[0].icon").value("lote"))
                .andExpect(jsonPath("$.questions[1].questionId").value("sampling.meaning"))
                .andExpect(jsonPath("$.questions[1].label").value("¿Qué significa este resultado?"))
                .andExpect(jsonPath("$.questions[1].icon").value("fruit"))
                .andExpect(jsonPath("$.questions[2].questionId").value("sampling.next"))
                .andExpect(jsonPath("$.questions[2].label").value("¿Qué hago ahora?"))
                .andExpect(jsonPath("$.questions[2].icon").value("calendar"))
                .andExpect(jsonPath("$.questions[3].questionId").value("sampling.correct_tree"))
                .andExpect(jsonPath("$.questions[3].label").value("¿Cómo corrijo un árbol?"))
                .andExpect(jsonPath("$.questions[3].icon").value("pruning"))
                .andExpect(jsonPath("$.questions[0].enabled").value(true))
                .andExpect(jsonPath("$.questions[1].enabled").value(true))
                .andExpect(jsonPath("$.questions[2].enabled").value(true))
                .andExpect(jsonPath("$.questions[3].enabled").value(false))
                .andExpect(jsonPath("$.questions[3].disabledReason")
                        .value("Corregir un árbol todavía no está disponible"));
        catalog(plotId, "en")
                .andExpect(jsonPath("$.questions[0].label").value("Are these enough trees?"))
                .andExpect(jsonPath("$.questions[3].disabledReason").value("Correcting a tree is not available yet"));

        answer("sampling.enough_trees", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("sampling.enough_trees"))
                .andExpect(jsonPath("$.question").value("¿Son suficientes árboles?"))
                .andExpect(jsonPath("$.text").value("Sí: muestreaste 5 árboles y el mínimo es 5. La muestra da 0,59 "
                        + "frutos por brote frente al objetivo de 0,40."))
                .andExpect(jsonPath("$.facts", hasSize(2)))
                .andExpect(jsonPath("$.facts[0].value").value("5/5"))
                .andExpect(jsonPath("$.facts[0].label").value("árboles muestreados"))
                .andExpect(jsonPath("$.facts[0].pill").value("muestra representativa"))
                .andExpect(jsonPath("$.facts[0].tone").value("PRIMARY"))
                .andExpect(jsonPath("$.facts[1].value").value("0,59"))
                .andExpect(jsonPath("$.facts[1].pill").value("objetivo 0,40"))
                .andExpect(jsonPath("$.facts[1].tone").value("HIGHLIGHT"))
                .andExpect(jsonPath("$.sources[0].label", startsWith("Basado en 5 árboles, ronda del ")))
                .andExpect(jsonPath("$.actions", hasSize(0)))
                .andExpect(jsonPath("$.model.provider").value("TEMPLATE"));
        answer("sampling.meaning", plotId, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("What does this result mean?"))
                .andExpect(jsonPath("$.text").value("Your trees carry 0.59 fruits per shoot and the plan aims at "
                        + "0.40, the load the tree can sustain: today they carry 1.48 times that load. That is why "
                        + "the plan says to remove 32 % of the fruits."))
                .andExpect(jsonPath("$.facts[1].value").value("32%"))
                .andExpect(jsonPath("$.facts[1].label").value("of the fruits to remove"));
        answer("sampling.next", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", startsWith("Aclarea: la ventana está abierta hasta el ")))
                .andExpect(jsonPath("$.text", containsString("(quedan 12 días)")))
                .andExpect(jsonPath("$.facts[1].value").value("12"))
                .andExpect(jsonPath("$.facts[1].pill").value("ventana abierta"))
                .andExpect(jsonPath("$.actions", hasSize(1)))
                .andExpect(jsonPath("$.actions[0].type").value("DEEPLINK"))
                .andExpect(jsonPath("$.actions[0].label").value("Registrar aclareo"))
                .andExpect(jsonPath("$.actions[0].target").value("thinning/execute?plotId=" + plotId));
        answer("sampling.correct_tree", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"))
                .andExpect(jsonPath("$.detail").value(NOT_ENOUGH_DATA));

        mvc.perform(get(ANSWERS + "/latest").param("screen", "SAMPLING_SUMMARY").param("plotId", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("sampling.next"))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"));
    }

    @Test
    @DisplayName("sampling halfway: the trees missing, meaning disabled until the plan; never sampled: all disabled")
    void halfwayAndNeverSampled() throws Exception {
        var plotId = createPlot();
        stubPlan(plotId, new ThinningPlanDetail(false, null, 0.57, null, null, null, 3, today, null, null, null, null,
                null), 2);

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].enabled").value(true))
                .andExpect(jsonPath("$.questions[1].enabled").value(false))
                .andExpect(jsonPath("$.questions[1].disabledReason").value("Sin plan de aclareo activo"))
                .andExpect(jsonPath("$.questions[2].enabled").value(true))
                .andExpect(jsonPath("$.questions[3].enabled").value(false));
        answer("sampling.enough_trees", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Aún no: muestreaste 3 árboles y el mínimo para una muestra "
                        + "representativa es 5. Faltan 2 árboles para calcular el plan de aclareo."))
                .andExpect(jsonPath("$.facts[0].value").value("3/5"))
                .andExpect(jsonPath("$.facts[0].pill").value("faltan 2"))
                .andExpect(jsonPath("$.facts[1].value").value("Sin plan"));
        answer("sampling.next", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Muestrea 2 árboles más para llegar al mínimo de 5 y calcular el "
                        + "plan de aclareo."))
                .andExpect(jsonPath("$.actions", hasSize(0)));
        answer("sampling.meaning", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"));

        var never = createPlot();
        catalog(never, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[*].enabled").value(everyItem(is(false))))
                .andExpect(jsonPath("$.questions[0].disabledReason")
                        .value("Sin muestreo en esta campaña · registra un muestreo"))
                .andExpect(jsonPath("$.questions[3].disabledReason")
                        .value("Corregir un árbol todavía no está disponible"));
        catalog(never, "en")
                .andExpect(jsonPath("$.questions[0].disabledReason")
                        .value("No sampling this campaign · record a sampling"));
        answer("sampling.next", never, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"))
                .andExpect(jsonPath("$.detail").value(NOT_ENOUGH_DATA));
        mvc.perform(get(ANSWERS + "/latest").param("screen", "SAMPLING_SUMMARY").param("plotId", never))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANSWER_NOT_FOUND"));
    }

    @Test
    @DisplayName("SAMPLING_SUMMARY requires its plot: missing 400, unknown 404, another producer's 403")
    void requiresOwnPlot() throws Exception {
        mvc.perform(get(QUESTIONS).param("screen", "SAMPLING_SUMMARY").header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Indica el lote de esta pantalla"));
        catalog(UUID.randomUUID().toString(), "es").andExpect(status().isNotFound());
        authenticateAs(UUID.randomUUID().toString());
        var foreign = createPlot();
        authenticateAs(producer);
        catalog(foreign, "es")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        answer("sampling.enough_trees", foreign, "es").andExpect(status().isForbidden());
    }

    private void stubPlan(String plotId, ThinningPlanDetail plan, int treesNeeded) {
        when(thinningContextFacade.findThinningPlanDetail(eq(plotId), anyInt())).thenReturn(Optional.of(plan));
        when(thinningContextFacade.findPrescriptionAvailability(eq(plotId), anyInt()))
                .thenReturn(Optional.of(new PrescriptionAvailability(plan.issued(), plan.percentageToRemove(),
                        plan.sampledTrees(), treesNeeded, plan.caliberProjectionStatus())));
    }

    private ResultActions catalog(String plotId, String language) throws Exception {
        return mvc.perform(get(QUESTIONS).param("screen", "SAMPLING_SUMMARY").param("plotId", plotId)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private ResultActions answer(String questionId, String plotId, String language) throws Exception {
        var body = "{\"questionId\":\"%s\",\"plotId\":\"%s\",\"clientRequestId\":\"%s\",\"forceRefresh\":false}"
                .formatted(questionId, plotId, UUID.randomUUID());
        return mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
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
