package com.arcadiadevs.viora.platform.shared.interfaces.rest.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The lists that filter by an optional {@code plotId} query parameter answer like the routes under
 * {@code /plots/{plotId}}: 403 for a plot of another producer, whatever the route, and no filter or an own plot as before.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:foreignplotparameter;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Plot query parameter of the incident and thinning event lists")
class ForeignPlotQueryParameterIntegrationTest {

    private static final String INCIDENTS = "/api/v1/agroclimatic-incidents";
    private static final String THINNING_EVENTS = "/api/v1/thinning-events";

    @Autowired WebApplicationContext context;
    @MockitoBean SubscriptionContextFacade subscriptionContextFacade;
    private MockMvc mvc;
    private final String owner = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        authenticateAs(owner);
        when(subscriptionContextFacade.findEntitledQuotaHectares(anyString())).thenReturn(Optional.of(BigDecimal.TEN));
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {INCIDENTS, THINNING_EVENTS})
    @DisplayName("A plot of another producer answers 403 on both lists")
    void rejectsAnotherProducersPlot(String route) throws Exception {
        var plotId = createPlot();
        authenticateAs(UUID.randomUUID().toString());

        mvc.perform(get(route).param("plotId", plotId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
    }

    @ParameterizedTest
    @ValueSource(strings = {INCIDENTS, THINNING_EVENTS})
    @DisplayName("The owner's own plot and the list without a filter keep answering 200")
    void ownPlotAndNoFilterStillWork(String route) throws Exception {
        var plotId = createPlot();

        mvc.perform(get(route).param("plotId", plotId)).andExpect(status().isOk());
        mvc.perform(get(route)).andExpect(status().isOk());
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
