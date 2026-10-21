package com.arcadiadevs.viora.platform.subscription.interfaces;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.web.FilterChainProxy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "viora.subscription.monthly-rate=10", "viora.subscription.tax-rate=0.10",
        "viora.subscription.tariff-version=integration-fixture"})
class SubscriptionPlanIntegrationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy securityFilters;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilters).build();
    }

    @Test
    void exposesConfiguredOfferWithoutAuthenticationOrClientAmounts() throws Exception {
        mvc.perform(get("/api/v1/subscription-plans").param("monthlyRatePerHa", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PRODUCER_ANNUAL"))
                .andExpect(jsonPath("$[0].monthlyRatePerHa").value(10))
                .andExpect(jsonPath("$[0].taxRate").value(0.1))
                .andExpect(jsonPath("$[0].minimumQuotaHa").value(0.5))
                .andExpect(jsonPath("$[0].quotaIncrementHa").value(0.5))
                .andExpect(jsonPath("$[0].billingMonths").value(12))
                .andExpect(jsonPath("$[0].tariffVersion").value("integration-fixture"));
    }
}
