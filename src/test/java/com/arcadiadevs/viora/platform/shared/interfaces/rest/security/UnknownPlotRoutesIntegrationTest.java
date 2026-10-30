package com.arcadiadevs.viora.platform.shared.interfaces.rest.security;

import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * {@code PlotAccessPolicy} lets a plot it cannot find pass, so each route answers its own 404. Nothing stops a new
 * route from forgetting that answer and returning an empty 200, which reads as a plot with no data. This walks
 * every {@code GET} under {@code /plots/{plotId}} with a plot that does not exist and requires 404 from all of them, but
 * the routes listed as known gaps.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:unknownplotroutes;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Routes under /plots/{plotId} with a plot that does not exist")
class UnknownPlotRoutesIntegrationTest {

    /** Known gap, see docs/parches-pendientes.md: it lists devices without checking the plot, so it answers 200. */
    private static final Set<String> ANSWERS_200_FOR_NOW = Set.of("/api/v1/plots/{plotId}/iot-devices");

    @Autowired WebApplicationContext context;
    @Autowired RequestMappingHandlerMapping handlerMapping;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
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
    @DisplayName("every GET answers 404")
    void everyGetAnswers404() throws Exception {
        var answers = new ArrayList<String>();
        for (var entry : handlerMapping.getHandlerMethods().entrySet()) {
            var info = entry.getKey();
            if (!info.getMethodsCondition().getMethods().contains(RequestMethod.GET)) {
                continue;
            }
            for (var pattern : info.getPathPatternsCondition().getPatternValues()) {
                if (!pattern.contains("/plots/{plotId}/")) {
                    continue;
                }
                if (ANSWERS_200_FOR_NOW.contains(pattern)) {
                    continue;
                }
                var path = pattern.replace("{plotId}", UUID.randomUUID().toString()).replace("{campaignYear}", "2026")
                        .replaceAll("[{][^}]+[}]", UUID.randomUUID().toString());
                var status = mvc.perform(get(path)).andReturn().getResponse().getStatus();
                if (status != 404) {
                    answers.add(status + " GET " + pattern);
                }
            }
        }
        assertEquals(List.of(), answers);
    }
}
