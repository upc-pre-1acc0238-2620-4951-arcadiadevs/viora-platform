package com.arcadiadevs.viora.platform.subscription.interfaces;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.arcadiadevs.viora.platform.subscription.application.ports.PaymentGateway;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IndividualCheckoutTest {
    @Autowired private WebApplicationContext context;
    @Autowired private SubscriptionRepository subscriptions;
    @MockitoBean private PaymentGateway gateway;

    @AfterEach
    void clearActor() { SecurityContextHolder.clearContext(); }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void recoversOneHostedPreferenceAndNeverGrantsAccessFromTheLink(boolean legacy) throws Exception {
        var owner = new ProducerId(UUID.randomUUID().toString());
        var quota = new IndividualHectaresQuota(new BigDecimal("0.5"));
        var originalPrice = legacy ? new SubscriptionPrice(new Money(new BigDecimal("330.00")),
                new Money(new BigDecimal("59.40")), new Money(new BigDecimal("389.40")))
                : new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18")).quote(quota);
        var contract = subscriptions.save(Subscription.createIndividual(owner, "PRODUCER_ANNUAL", quota,
                originalPrice, Instant.now()));
        when(gateway.configured()).thenReturn(true);
        when(gateway.create(any(), any())).thenReturn(new PaymentGateway.Checkout("fixture-" + UUID.randomUUID(), "https://www.mercadopago.com.pe/checkout/test"));
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        var path = "/api/v1/subscriptions/" + contract.snapshot().id().subscriptionId() + "/checkouts";
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(owner.producerId(), null, List.of()));
        mvc.perform(post(path).header("Idempotency-Key", "one")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkoutUrl").value("https://www.mercadopago.com.pe/checkout/test"));
        mvc.perform(post(path).header("Idempotency-Key", "two")).andExpect(status().isCreated());
        var sentPrice = org.mockito.ArgumentCaptor.forClass(SubscriptionPrice.class);
        verify(gateway, times(1)).create(any(), sentPrice.capture());
        assertEquals(originalPrice, sentPrice.getValue());
        assertEquals(originalPrice, subscriptions.findById(contract.snapshot().id()).orElseThrow().snapshot().price());
        assertFalse(subscriptions.findById(contract.snapshot().id()).orElseThrow().hasActiveEntitlement(Instant.now()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null, List.of()));
        mvc.perform(post(path).header("Idempotency-Key", "foreign")).andExpect(status().isNotFound());
        SecurityContextHolder.clearContext();
        mvc.perform(post(path).header("Idempotency-Key", "anonymous")).andExpect(status().isUnauthorized());
    }
}
