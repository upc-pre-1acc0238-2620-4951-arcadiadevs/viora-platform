package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real controllers, services, Orchard facade and H2: the THINNING_DONE screen (P63, P145-P147). The outcome of the
 * confirmed thinning is stubbed at the Thinning facade; the clock is fixed in the 2026 campaign.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantthinningdone;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Assistant THINNING_DONE screen")
class AssistantThinningDoneScreenIntegrationTest {

    private static final String QUESTIONS = "/api/v1/assistant/questions";
    private static final String ANSWERS = "/api/v1/assistant/answers";
    private static final LocalDate EXECUTED_ON = LocalDate.of(2026, 10, 28);

    @Autowired WebApplicationContext context;
    @TestBean Clock clock;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    @MockitoBean ThinningContextFacade thinningContextFacade;
    private MockMvc mvc;
    private final String producer = UUID.randomUUID().toString();

    /** October 30, 2026 at 10:00 in Lima: campaign 2026, two days after the thinning. */
    static Clock clock() {
        return Clock.fixed(Instant.parse("2026-10-30T15:00:00Z"), ZoneOffset.UTC);
    }

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
    @DisplayName("thinning recorded: three questions enabled and answers from the outcome Thinning computed")
    void thinningRecorded() throws Exception {
        var plotId = createPlot();
        when(thinningContextFacade.findThinningOutcome(eq(plotId), eq(2026))).thenReturn(Optional.of(
                new ThinningOutcome(EXECUTED_ON, 30.0, "OPTIMAL", 0.5714, 0.4, 0.4, "BALANCED", 11700.0, 8200.0,
                        "FRUIT_COUNT_ONLY", 3, "SYNTHETIC_DEMO", 14, 49)));

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("THINNING_DONE"))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.questions.length()").value(3))
                .andExpect(jsonPath("$.questions[0].questionId").value("thinning.harvest_change"))
                .andExpect(jsonPath("$.questions[0].label").value("¿Cómo cambia mi cosecha?"))
                .andExpect(jsonPath("$.questions[0].icon").value("harvest"))
                .andExpect(jsonPath("$.questions[1].questionId").value("thinning.why_target"))
                .andExpect(jsonPath("$.questions[1].label").value("¿Por qué el objetivo es 0,40?"))
                .andExpect(jsonPath("$.questions[1].icon").value("prio"))
                .andExpect(jsonPath("$.questions[2].questionId").value("thinning.next_sampling"))
                .andExpect(jsonPath("$.questions[2].label").value("¿Cuándo vuelvo a muestrear?"))
                .andExpect(jsonPath("$.questions[2].icon").value("calendar"))
                .andExpect(jsonPath("$.questions[*].enabled").value(everyItem(is(true))))
                .andExpect(jsonPath("$.questions[*].disabledReason").value(everyItem(nullValue())));
        catalog(plotId, "en")
                .andExpect(jsonPath("$.questions[0].label").value("How does my harvest change?"))
                .andExpect(jsonPath("$.questions[1].label").value("Why is the target 0.40?"))
                .andExpect(jsonPath("$.questions[2].label").value("When do I sample again?"));

        answer("thinning.harvest_change", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("thinning.harvest_change"))
                .andExpect(jsonPath("$.question").value("¿Cómo cambia mi cosecha?"))
                .andExpect(jsonPath("$.text").value("Con la carga que dejó tu aclareo, Viora estima 8.200 kg por "
                        + "hectárea para esta campaña. Sin aclarear la cifra llegaría a 11.700 kg, pero es un techo: "
                        + "con más fruta el calibre baja y Viora aún no lo mide para tu variedad. La carga quedó en "
                        + "0,40 frutos por brote, dentro del objetivo de 0,40."))
                .andExpect(jsonPath("$.facts", hasSize(2)))
                .andExpect(jsonPath("$.facts[0].value").value("8.200"))
                .andExpect(jsonPath("$.facts[0].label").value("kg por hectárea estimados"))
                .andExpect(jsonPath("$.facts[0].pill").value("por número de frutos"))
                .andExpect(jsonPath("$.facts[0].tone").value("PRIMARY"))
                .andExpect(jsonPath("$.facts[1].value").value("0,40"))
                .andExpect(jsonPath("$.facts[1].label").value("frutos por brote tras el aclareo"))
                .andExpect(jsonPath("$.facts[1].pill").value("objetivo 0,40"))
                .andExpect(jsonPath("$.facts[1].tone").value("HIGHLIGHT"))
                .andExpect(jsonPath("$.sources[0].label").value("Basado en el aclareo registrado el 28 de octubre"))
                .andExpect(jsonPath("$.actions", hasSize(0)))
                .andExpect(jsonPath("$.model.provider").value("TEMPLATE"));
        answer("thinning.why_target", plotId, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("Why is the target 0.40?"))
                .andExpect(jsonPath("$.text").value("The target of 0.40 fruits per shoot is the load the tree can "
                        + "sustain according to the thinning profile of your variety. That profile is a demonstration "
                        + "one and no agronomist validated it. Your sampling measured 0.57, you removed 30 % of the "
                        + "fruits and the load ended at 0.40."))
                .andExpect(jsonPath("$.facts[0].value").value("0.40"))
                .andExpect(jsonPath("$.facts[0].label").value("target fruits per shoot"))
                .andExpect(jsonPath("$.facts[0].pill").value("demonstration"))
                .andExpect(jsonPath("$.facts[1].pill").value("you removed 30 %"))
                .andExpect(jsonPath("$.sources[0].label").value("Based on the thinning recorded on October 28"));
        answer("thinning.next_sampling", plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Ya registraste el aclareo el 28 de octubre, así que esta campaña "
                        + "no pide más muestreos. En la campaña 2027, registra la plena floración de tu lote y muestrea "
                        + "antes de que abra la ventana de aclareo, que en el perfil de tu variedad va de 14 a 49 días "
                        + "después de la floración."))
                .andExpect(jsonPath("$.facts[0].value").value("2027"))
                .andExpect(jsonPath("$.facts[0].label").value("próximo muestreo"))
                .andExpect(jsonPath("$.facts[0].pill").value("tras la plena floración"))
                .andExpect(jsonPath("$.facts[1].value").value("14–49"))
                .andExpect(jsonPath("$.facts[1].label").value("días después de la floración"))
                .andExpect(jsonPath("$.facts[1].pill").value("ventana de aclareo"));

        // The latest answer of the screen is found by the thinning. prefix of its questions
        mvc.perform(get(ANSWERS + "/latest").param("screen", "THINNING_DONE").param("plotId", plotId)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId", startsWith("thinning.")))
                .andExpect(jsonPath("$.freshness").value("UP_TO_DATE"));
    }

    @Test
    @DisplayName("without the yield or the target: those two disabled with their reasons, next_sampling still answered")
    void partialOutcome() throws Exception {
        var plotId = createPlot();
        when(thinningContextFacade.findThinningOutcome(eq(plotId), eq(2026))).thenReturn(Optional.of(
                new ThinningOutcome(EXECUTED_ON, null, null, null, null, null, null, null, null, null, 0, null, null,
                        null)));

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].enabled").value(false))
                .andExpect(jsonPath("$.questions[0].disabledReason")
                        .value("Faltan cosechas registradas o la superficie del lote para estimar la cosecha"))
                .andExpect(jsonPath("$.questions[1].enabled").value(false))
                .andExpect(jsonPath("$.questions[1].label").value("¿Por qué ese objetivo de carga?"))
                .andExpect(jsonPath("$.questions[1].disabledReason").value("Faltan datos para responder esta pregunta"))
                .andExpect(jsonPath("$.questions[2].enabled").value(true));
        catalog(plotId, "en")
                .andExpect(jsonPath("$.questions[0].disabledReason")
                        .value("Recorded harvests or the plot area are missing to estimate the harvest"))
                .andExpect(jsonPath("$.questions[1].disabledReason").value("Data is missing to answer this question"));
        answer("thinning.next_sampling", plotId, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("You already recorded the thinning on October 28, so this "
                        + "campaign needs no more sampling. In the 2027 campaign, record the full bloom of your plot "
                        + "and sample again before thinning."))
                .andExpect(jsonPath("$.facts[1].value").value("Recorded"))
                .andExpect(jsonPath("$.facts[1].pill").value("on Oct 28"));
        answer("thinning.harvest_change", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"));
    }

    @Test
    @DisplayName("before the thinning is recorded: every question disabled with its reason, and answering one is a 409")
    void notRecorded() throws Exception {
        var plotId = createPlot();

        catalog(plotId, "es")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(3))
                .andExpect(jsonPath("$.questions[*].enabled").value(everyItem(is(false))))
                .andExpect(jsonPath("$.questions[*].disabledReason")
                        .value(everyItem(is("Sin aclareo registrado en esta campaña"))))
                .andExpect(jsonPath("$.questions[1].label").value("¿Por qué ese objetivo de carga?"));
        catalog(plotId, "en")
                .andExpect(jsonPath("$.questions[2].disabledReason").value("No thinning recorded this campaign"));
        answer("thinning.next_sampling", plotId, "es")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSISTANT_CONFLICT"));
        mvc.perform(get(ANSWERS + "/latest").param("screen", "THINNING_DONE").param("plotId", plotId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANSWER_NOT_FOUND"));
    }

    @Test
    @DisplayName("THINNING_DONE is a plot screen: 400 without plotId, 404 for an unknown plot")
    void requiresPlot() throws Exception {
        mvc.perform(get(QUESTIONS).param("screen", "THINNING_DONE").header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Indica el lote de esta pantalla"));
        mvc.perform(post(ANSWERS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"thinning.harvest_change\",\"clientRequestId\":\"%s\"}"
                                .formatted(UUID.randomUUID()))
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isBadRequest());
        catalog(UUID.randomUUID().toString(), "es").andExpect(status().isNotFound());
    }

    private ResultActions catalog(String plotId, String language) throws Exception {
        return mvc.perform(get(QUESTIONS).param("screen", "THINNING_DONE").param("plotId", plotId)
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
