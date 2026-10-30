package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The limits per producer over HTTP: 429 with Retry-After and a localized detail. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantratelimit;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "viora.assistant.rate-limit.answers-per-hour=1",
        "viora.assistant.rate-limit.refreshes-per-hour=1",
        "viora.assistant.rate-limit.catalog-per-hour=1"
})
@DisplayName("Assistant rate limits (429)")
class AssistantRateLimitIntegrationTest {

    /** The first request of the hour leaves in about an hour; the real clock may have moved a second or two. */
    private static final org.hamcrest.Matcher<String> ABOUT_AN_HOUR = matchesPattern("3[56]\\d\\d");

    @Autowired WebApplicationContext context;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        // A new producer per test: the limits are per producer
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null, List.of()));
        when(subscriptionContextFacade.findEntitledQuotaHectares(anyString())).thenReturn(Optional.of(BigDecimal.TEN));
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("A second question within the hour answers 429 with Retry-After and a Spanish detail")
    void answersLimitInSpanish() throws Exception {
        var plotId = createPlot();
        // Without harvests the question is disabled (409), and it still counts
        ask(plotId, "es").andExpect(status().isConflict());

        ask(plotId, "es")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, ABOUT_AN_HOUR))
                .andExpect(jsonPath("$.code").value("ASSISTANT_TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber())
                .andExpect(jsonPath("$.detail").value("Le hiciste demasiadas preguntas al asistente en poco tiempo. "
                        + "Espera unos minutos y vuelve a intentarlo."));
    }

    @Test
    @DisplayName("A second catalog read within the hour answers 429 with Retry-After and an English detail")
    void catalogLimitInEnglish() throws Exception {
        var plotId = createPlot();
        catalog(plotId, "en").andExpect(status().isOk());

        catalog(plotId, "en")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, ABOUT_AN_HOUR))
                .andExpect(jsonPath("$.code").value("ASSISTANT_TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.detail").value("You asked the assistant too many times in a short while. "
                        + "Wait a few minutes and try again."));
    }

    @Test
    @DisplayName("Asking a question does not spend the catalog limit, though it evaluates the catalog")
    void questionsDoNotSpendTheCatalog() throws Exception {
        var plotId = createPlot();
        ask(plotId, "es").andExpect(status().isConflict());

        catalog(plotId, "es").andExpect(status().isOk());
    }

    private ResultActions ask(String plotId, String language) throws Exception {
        return mvc.perform(post("/api/v1/assistant/answers").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"questionId":"alternation.why_index","plotId":"%s","clientRequestId":"%s"}
                        """.formatted(plotId, UUID.randomUUID()))
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private ResultActions catalog(String plotId, String language) throws Exception {
        return mvc.perform(get("/api/v1/assistant/questions").param("screen", "ALTERNATION").param("plotId", plotId)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language));
    }

    private String createPlot() throws Exception {
        var payload = """
                {"name":"Cuartel %s","variety":"SEVILLANA",
                 "polygonGeoJson":"{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                 "rowSpacingM":7.0,"treeSpacingM":5.0}
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        return JsonPath.read(mvc.perform(post("/api/v1/plots").contentType(MediaType.APPLICATION_JSON)
                .content(payload)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
                "$.id");
    }
}
