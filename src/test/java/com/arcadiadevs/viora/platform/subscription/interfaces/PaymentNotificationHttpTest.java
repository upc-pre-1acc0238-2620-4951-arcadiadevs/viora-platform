package com.arcadiadevs.viora.platform.subscription.interfaces;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import com.arcadiadevs.viora.platform.subscription.application.ports.PaymentGateway;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.*;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the signed provider boundary through the actual HTTP filter chain and database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "viora.payments.webhook-secret=http-notification-fixture")
class PaymentNotificationHttpTest {
    @LocalServerPort private int port;
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private PaymentAttemptRepository attempts;
    @Autowired private CheckoutService payments;
    @MockitoBean private PaymentGateway gateway;
    private String paymentId;
    private Subscription contract;
    private CheckoutService.CheckoutResult checkout;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @BeforeEach
    void preparePendingPurchase() {
        paymentId = Long.toString(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        var quota = new IndividualHectaresQuota(new BigDecimal("0.5"));
        var price = new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18")).quote(quota);
        var owner = new ProducerId(UUID.randomUUID().toString());
        contract = subscriptions.save(Subscription.createIndividual(owner, "PRODUCER_ANNUAL", quota, price, Instant.now()));
        when(gateway.configured()).thenReturn(true);
        when(gateway.create(any(), any())).thenReturn(new PaymentGateway.Checkout(
                "fixture-" + UUID.randomUUID(), "https://sandbox.mercadopago.com.pe/checkout/test"));
        checkout = payments.create(owner.producerId(), UUID.fromString(contract.snapshot().id().subscriptionId()),
                "notification-http").success().orElseThrow();
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "in_process", "rejected", "cancelled"})
    void nonApprovedEvidenceIsPersistedWithoutGrantingAccess(String status) throws Exception {
        evidence(status, contract.snapshot().price().total());
        var response = notify(paymentId, "payment", signature(paymentId));
        assertEquals(200, response.statusCode(), response.body());
        var reloaded = subscriptions.findById(contract.snapshot().id()).orElseThrow();
        assertEquals(SubscriptionStatus.PENDING_PAYMENT, reloaded.snapshot().status());
        assertNull(reloaded.snapshot().period());
        assertFalse(reloaded.hasActiveEntitlement(Instant.now()));
        assertEquals(status, attempts.find(checkout.intentId()).orElseThrow().paymentStates().get(paymentId));
    }

    @Test
    void approvedReplayDoesNotExtendAccessOrRegressOnLaterRejection() throws Exception {
        evidence("approved", contract.snapshot().price().total());
        assertEquals(200, notify(paymentId, "payment", signature(paymentId)).statusCode());
        var activated = subscriptions.findById(contract.snapshot().id()).orElseThrow().snapshot();
        assertEquals(SubscriptionStatus.ACTIVE, activated.status());
        assertEquals(200, notify(paymentId, "payment", signature(paymentId)).statusCode());
        evidence("rejected", contract.snapshot().price().total());
        assertEquals(200, notify(paymentId, "payment", signature(paymentId)).statusCode());
        var reloaded = subscriptions.findById(contract.snapshot().id()).orElseThrow().snapshot();
        assertEquals(activated.period(), reloaded.period());
        assertEquals(SubscriptionStatus.ACTIVE, reloaded.status());
        assertEquals("APPROVED", attempts.find(checkout.intentId()).orElseThrow().status());
    }

    @Test
    void forgedOrInconsistentEnvelopesNeverReachProvider() throws Exception {
        assertEquals(401, notify(paymentId, "payment", "invalid").statusCode());
        assertEquals(401, notify("456", "payment", signature(paymentId)).statusCode());
        assertEquals(401, notify(paymentId, "merchant_order", signature(paymentId)).statusCode());
        verify(gateway, never()).verify(anyString());
    }

    @Test
    void providerFailureIsRetryableAndDoesNotActivateAccess() throws Exception {
        when(gateway.verify(paymentId)).thenThrow(new PaymentGateway.Unavailable());
        assertEquals(503, notify(paymentId, "payment", signature(paymentId)).statusCode());
        assertEquals(SubscriptionStatus.PENDING_PAYMENT,
                subscriptions.findById(contract.snapshot().id()).orElseThrow().snapshot().status());
    }

    @Test
    void approvedPaymentWithDifferentAmountCannotActivateAccess() throws Exception {
        evidence("approved", new Money(new BigDecimal("1.00")));
        var response = notify(paymentId, "payment", signature(paymentId));
        assertEquals(422, response.statusCode(), response.body());
        assertEquals(SubscriptionStatus.PENDING_PAYMENT,
                subscriptions.findById(contract.snapshot().id()).orElseThrow().snapshot().status());
    }
    private void evidence(String status, Money amount) {
        var now = Instant.now();
        when(gateway.verify(paymentId)).thenReturn(new PaymentGateway.VerifiedPayment(paymentId, checkout.intentId(),
                status, amount, "PEN", now, "approved".equals(status) ? now : null));
    }

    private HttpResponse<String> notify(String bodyId, String type, String signature) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/api/v1/payment-notifications/mercado-pago?data.id=" + paymentId))
                .header("Content-Type", "application/json").header("x-request-id", "http-fixture")
                .header("x-signature", signature).POST(HttpRequest.BodyPublishers.ofString(
                        "{\"type\":\"" + type + "\",\"data\":{\"id\":\"" + bodyId + "\"}}" )).build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String signature(String id) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("http-notification-fixture".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        var ts = Long.toString(java.time.Instant.now().getEpochSecond());
        return "ts=" + ts + ",v1=" + HexFormat.of().formatHex(mac.doFinal(
                ("id:" + id + ";request-id:http-fixture;ts:" + ts + ";").getBytes(StandardCharsets.UTF_8)));
    }
}
