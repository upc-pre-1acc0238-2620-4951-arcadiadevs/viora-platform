package com.arcadiadevs.viora.platform.assistant.interfaces.rest;

import com.arcadiadevs.viora.platform.assistant.infrastructure.ai.FakeChatModel;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real application with provider=groq and a fake chat model (no network): model text or template fallback. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantmodel;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "viora.assistant.provider=groq",
        "viora.assistant.model=test-model"
})
@DisplayName("Assistant answers written by a model (provider=groq)")
class AssistantAnswerModelIntegrationTest {

    private static final String ROUTE = "/api/v1/assistant/answers";

    @TestConfiguration
    static class FakeModel {
        @Bean
        FakeChatModel fakeChatModel() {
            return new FakeChatModel();
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired Clock clock;
    @Autowired FakeChatModel chatModel;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null, List.of()));
        when(subscriptionContextFacade.findEntitledQuotaHectares(anyString())).thenReturn(Optional.of(BigDecimal.TEN));
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        chatModel.reset();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("A backed model text is the answer (201, groq, no fallback); a replay never calls the model again")
    void modelTextIsTheAnswer() throws Exception {
        var plotId = createPlotWithTwoHarvests();
        chatModel.replyText("Tu índice de vecería es 0,50: tus cosechas alternan entre años altos y bajos.");
        var key = UUID.randomUUID().toString();

        var created = ask(plotId, key)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text")
                        .value("Tu índice de vecería es 0,50: tus cosechas alternan entre años altos y bajos."))
                .andExpect(jsonPath("$.model.provider").value("groq"))
                .andExpect(jsonPath("$.model.name").value("test-model"))
                .andExpect(jsonPath("$.model.fallback").value(false))
                .andExpect(jsonPath("$.facts.length()").value(2))
                .andExpect(jsonPath("$.facts[0].value").value("0,50"))
                .andReturn().getResponse().getContentAsString();

        ask(plotId, key).andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value((String) JsonPath.read(created, "$.answerId")));
        ask(plotId, UUID.randomUUID().toString()).andExpect(status().isOk());
        assertThat(chatModel.prompts()).hasSize(1);
    }

    @Test
    @DisplayName("A model text with an invented number falls back to the template text (fallback=true)")
    void inventedNumberFallsBack() throws Exception {
        var plotId = createPlotWithTwoHarvests();
        chatModel.replyText("Tu índice de vecería es 0,62, así que el próximo año será bajo.");

        ask(plotId, UUID.randomUUID().toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text", startsWith("Tu índice de vecería es 0,50, lo que indica")))
                .andExpect(jsonPath("$.model.provider").value("template"))
                .andExpect(jsonPath("$.model.name").value("template-v1"))
                .andExpect(jsonPath("$.model.fallback").value(true));
        assertThat(chatModel.prompts()).hasSize(1);
    }

    private ResultActions ask(String plotId, String clientRequestId) throws Exception {
        return mvc.perform(post(ROUTE).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"questionId":"alternation.why_index","plotId":"%s","clientRequestId":"%s"}
                        """.formatted(plotId, clientRequestId))
                .header(HttpHeaders.ACCEPT_LANGUAGE, "es"));
    }

    /** Two campaigns of 1000 and 3000 kg give a Hoblyn index of 0.50. */
    private String createPlotWithTwoHarvests() throws Exception {
        var payload = """
                {"name":"Cuartel %s","variety":"SEVILLANA",
                 "polygonGeoJson":"{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                 "rowSpacingM":7.0,"treeSpacingM":5.0}
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        String plotId = JsonPath.read(mvc.perform(post("/api/v1/plots").contentType(MediaType.APPLICATION_JSON)
                .content(payload)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
                "$.id");
        int year = Year.now(clock.withZone(ProducerTimeZone.ZONE)).getValue();
        for (var harvest : List.of(List.of(year - 2, 1000.0), List.of(year - 1, 3000.0))) {
            mvc.perform(post("/api/v1/plots/" + plotId + "/harvest-records").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"campaignYear\":%s,\"totalYieldKg\":%s}".formatted(harvest.get(0),
                                    harvest.get(1))))
                    .andExpect(status().isCreated());
        }
        return plotId;
    }
}
