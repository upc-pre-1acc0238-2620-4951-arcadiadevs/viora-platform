package com.arcadiadevs.viora.platform.subscription.interfaces;

import java.time.Instant;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import com.arcadiadevs.viora.platform.subscription.application.ports.PaymentGateway;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "viora.payments.webhook-secret=notification-fixture")
class IndividualPaymentNotificationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private CheckoutService payments;
    @MockitoBean private PaymentGateway gateway;

    private String signature(String id) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("notification-fixture".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        var ts = Long.toString(java.time.Instant.now().getEpochSecond());
        return "ts=" + ts + ",v1=" + HexFormat.of().formatHex(mac.doFinal(
                ("id:" + id + ";request-id:fixture-request;ts:" + ts + ";").getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void signedApprovalActivatesOnceAndInvalidSignatureCannotQueryProvider() throws Exception {
        var paymentId = Long.toString(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        var owner = new ProducerId(UUID.randomUUID().toString());
        var quota = new IndividualHectaresQuota(new BigDecimal("0.5"));
        var price = new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18")).quote(quota);
        var contract = subscriptions.save(Subscription.createIndividual(owner, "PRODUCER_ANNUAL", quota, price, Instant.now()));
        when(gateway.configured()).thenReturn(true);
        when(gateway.create(any(), any())).thenReturn(new PaymentGateway.Checkout("fixture-" + UUID.randomUUID(), "https://www.mercadopago.com.pe/checkout/test"));
        var checkout = payments.create(owner.producerId(), UUID.fromString(contract.snapshot().id().subscriptionId()), "notify").success().orElseThrow();
        var at = Instant.now();
        when(gateway.verify(paymentId)).thenReturn(new PaymentGateway.VerifiedPayment(paymentId, checkout.intentId(), "approved", price.total(), "PEN", at, at));
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        var body = "{\"type\":\"payment\",\"data\":{\"id\":\"" + paymentId + "\"}}";
        mvc.perform(post("/api/v1/payment-notifications/mercado-pago").param("data.id", paymentId)
                .header("x-request-id", "fixture-request").header("x-signature", "invalid")
                .contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        verify(gateway, never()).verify(anyString());
        mvc.perform(post("/api/v1/payment-notifications/mercado-pago").param("data.id", paymentId)
                .header("x-request-id", "fixture-request").header("x-signature", signature(paymentId))
                .contentType("application/json").content(body)).andExpect(status().isOk());
        var active = subscriptions.findById(contract.snapshot().id()).orElseThrow();
        assertEquals(SubscriptionStatus.ACTIVE, active.snapshot().status());
        assertTrue(active.hasActiveEntitlement(Instant.now()));
        var end = active.snapshot().period().endsAt();
        mvc.perform(post("/api/v1/payment-notifications/mercado-pago").param("data.id", paymentId)
                .header("x-request-id", "fixture-request").header("x-signature", signature(paymentId))
                .contentType("application/json").content(body)).andExpect(status().isOk());
        assertEquals(end, subscriptions.findById(contract.snapshot().id()).orElseThrow().snapshot().period().endsAt());
    }
}
