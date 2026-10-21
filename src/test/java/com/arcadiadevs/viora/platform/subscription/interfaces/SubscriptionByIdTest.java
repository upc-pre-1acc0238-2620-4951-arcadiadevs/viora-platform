package com.arcadiadevs.viora.platform.subscription.interfaces;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SubscriptionByIdTest {
    @Autowired private WebApplicationContext context;
    @Autowired private SubscriptionRepository subscriptions;

    @AfterEach
    void clearActor() { SecurityContextHolder.clearContext(); }

    @Test
    void exposesOnlyOwnedContractAndValidatesIdentifier() throws Exception {
        var owner = new ProducerId(UUID.randomUUID().toString());
        var quota = new IndividualHectaresQuota(new BigDecimal("0.5"));
        var contract = subscriptions.save(Subscription.createIndividual(owner, "PRODUCER_ANNUAL", quota,
                new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18")).quote(quota), Instant.now()));
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        var path = "/api/v1/subscriptions/" + contract.snapshot().id().subscriptionId();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(owner.producerId(), null, List.of()));
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(389.4));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null, List.of()));
        mvc.perform(get(path)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/subscriptions/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/subscriptions/not-a-uuid")).andExpect(status().isBadRequest());
        SecurityContextHolder.clearContext();
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
}
