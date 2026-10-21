package com.arcadiadevs.viora.platform.subscription.interfaces;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class CurrentSubscriptionTest {
    @Autowired private WebApplicationContext context;
    @Autowired private SubscriptionRepository subscriptions;
    private MockMvc mvc;
    private ProducerId owner;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        owner = new ProducerId(UUID.randomUUID().toString());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(owner.producerId(), null, List.of()));
    }

    @AfterEach
    void clearActor() { SecurityContextHolder.clearContext(); }

    @Test
    void emptyOwnerGetsNoContentAndCannotReadAnotherProducer() throws Exception {
        mvc.perform(get("/api/v1/subscriptions")).andExpect(status().isNoContent());
        var quota = new IndividualHectaresQuota(new BigDecimal("0.5"));
        subscriptions.save(Subscription.createIndividual(new ProducerId(UUID.randomUUID().toString()), "PRODUCER_ANNUAL", quota,
                new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18")).quote(quota), Instant.now()));
        mvc.perform(get("/api/v1/subscriptions")).andExpect(status().isNoContent());
    }

    @Test
    void activeSponsoredAccessIsNotHiddenByNewerPendingPurchase() throws Exception {
        var now = Instant.now();
        var sponsored = Subscription.reconstitute(new SubscriptionSnapshot(new SubscriptionId(), owner,
                PlanMode.PLAN_COOPERATIVO_PATROCINADO, SubscriptionStatus.ACTIVE,
                new HectaresQuota(new BigDecimal("0.25")), new SubscriptionPeriod(now.minusSeconds(60), now.plusSeconds(600)),
                new CooperativeId(UUID.randomUUID().toString()), new CooperativeLicenseId(UUID.randomUUID().toString()),
                new InvitationCodeId(UUID.randomUUID().toString()), 0L));
        subscriptions.save(sponsored);
        var quota = new IndividualHectaresQuota(new BigDecimal("0.5"));
        subscriptions.save(Subscription.createIndividual(owner, "PRODUCER_ANNUAL", quota,
                new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18")).quote(quota), now));
        mvc.perform(get("/api/v1/subscriptions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sponsored.snapshot().id().subscriptionId()))
                .andExpect(jsonPath("$.mode").value("PLAN_COOPERATIVO_PATROCINADO"))
                .andExpect(jsonPath("$.entitlementActive").value(true))
                .andExpect(jsonPath("$.totalAmount").doesNotExist());
    }
}
