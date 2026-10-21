package com.arcadiadevs.viora.platform.subscription.interfaces;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.arcadiadevs.viora.platform.subscription.application.ports.ProducerProfilePort;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.CreateSubscriptionCommand;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties = "viora.payments.webhook-secret=integration-fixture")
class SubscriptionContractIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired SubscriptionCommandService commands;
    @MockitoBean ProducerProfilePort profiles;
    @MockitoBean com.arcadiadevs.viora.platform.subscription.application.ports.PaymentGateway gateway;
    @Autowired com.arcadiadevs.viora.platform.subscription.domain.repositories.PaymentAttemptRepository attempts;
    MockMvc mvc;
    String owner;
    @BeforeEach void setup() {
        owner = UUID.randomUUID().toString();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(owner, null, List.of()));
        when(profiles.isReady(anyString())).thenReturn(true);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void catalogIsPublicAndUsesAgreedTariff() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/v1/subscription-plans")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].monthlyRatePerHa").value(55))
            .andExpect(jsonPath("$[0].minimumQuotaHa").value(0.5));
    }
    @Test void creationReplayCurrentAndOwnership() throws Exception {
        mvc.perform(get("/api/v1/subscriptions")).andExpect(status().isNoContent());
        var request = "{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":5}";
        var response = mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "op-1")
            .contentType("application/json").content(request)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.totalAmount").value(3894.0))
            .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.entitlementActive").value(false)).andReturn();
        var location = response.getResponse().getHeader("Location");
        mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "op-1")
            .contentType("application/json").content(request)).andExpect(status().isOk())
            .andExpect(header().string("Location", location));
        mvc.perform(get("/api/v1/subscriptions")).andExpect(status().isOk());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("another", null, List.of()));
        mvc.perform(get(location)).andExpect(status().isNotFound());
    }
    @Test void concurrentInitialRequestsCannotCreateTwoContracts() throws Exception {
        var command = new CreateSubscriptionCommand(owner, "PRODUCER_ANNUAL", new java.math.BigDecimal("5"), "one");
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Callable<Boolean> call = () -> { start.await(); return commands.handle(command).isSuccess(); };
            var a = pool.submit(call); var b = pool.submit(call); start.countDown();
            assertTrue(a.get(15, TimeUnit.SECONDS)); assertTrue(b.get(15, TimeUnit.SECONDS));
        }
    }
    @Test void invalidCapacityHasStructuredFailure() throws Exception {
        mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "bad")
            .contentType("application/json").content("{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":0.7}"))
            .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }
    @Test void signedWebhookActivatesStoredContractAndDuplicatesDoNotExtendIt() throws Exception {
        var created = commands.handle(new CreateSubscriptionCommand(owner, "PRODUCER_ANNUAL", new java.math.BigDecimal("0.5"), "webhook-contract")).success().orElseThrow();
        var id = created.id();
        when(gateway.configured()).thenReturn(true);
        when(gateway.create(any(), any())).thenReturn(new com.arcadiadevs.viora.platform.subscription.application.ports.PaymentGateway.Checkout(UUID.randomUUID().toString(), "https://sandbox.mercadopago.com.pe/checkout/test"));
        mvc.perform(post("/api/v1/subscriptions/" + id + "/checkouts").header("Idempotency-Key", "checkout"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.checkoutUrl").isString());
        var attempt = attempts.latest(id).orElseThrow();
        var now = java.time.Instant.now();
        when(gateway.verify("123")).thenReturn(new com.arcadiadevs.viora.platform.subscription.application.ports.PaymentGateway.VerifiedPayment("123", attempt.id(), "approved",
            new com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.Money(new java.math.BigDecimal("389.40")), "PEN", now, now));
        var notification = post("/api/v1/payment-notifications/mercado-pago").queryParam("data.id", "123")
            .header("x-request-id", "fixture-request").header("x-signature", signature("123"))
            .contentType("application/json").content("{\"type\":\"payment\",\"data\":{\"id\":\"123\"}}");
        SecurityContextHolder.clearContext();
        mvc.perform(notification).andExpect(status().isOk());
        mvc.perform(notification).andExpect(status().isOk());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(owner, null, List.of()));
        mvc.perform(get("/api/v1/subscriptions/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.entitlementActive").value(true));
        mvc.perform(get("/api/v1/subscriptions/" + id + "/payment-status")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("intruder", null, List.of()));
        mvc.perform(get("/api/v1/subscriptions/" + id + "/payment-status")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/subscriptions/" + id + "/checkouts").header("Idempotency-Key", "intruder" )).andExpect(status().isNotFound());
    }
    @Test void forgedNotificationCannotCallProvider() throws Exception {
        mvc.perform(post("/api/v1/payment-notifications/mercado-pago").queryParam("data.id", "123")
            .header("x-request-id", "fixture-request").header("x-signature", "ts=1,v1=" + "0".repeat(64))
            .contentType("application/json").content("{\"type\":\"payment\",\"data\":{\"id\":\"123\"}}"))
            .andExpect(status().isUnauthorized());
        verify(gateway, never()).verify(anyString());
    }
    private String signature(String id) throws Exception {
        var mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec("integration-fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        var manifest = "id:" + id + ";request-id:fixture-request;ts:1;";
        return "ts=1,v1=" + HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
