package com.arcadiadevs.viora.platform.subscription.infrastructure.mercadopago;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import com.arcadiadevs.viora.platform.subscription.application.ports.PaymentGateway;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MercadoPagoGatewayTest {
    MockRestServiceServer server;
    MercadoPagoGateway gateway;
    final UUID intent = UUID.randomUUID();
    @BeforeEach void setup() {
        var builder = RestClient.builder().baseUrl("https://api.mercadopago.com");
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new MercadoPagoGateway(true, true, "fixture-token", "https://example.test/notify", "https://example.test/payments/return", builder.build());
    }
    @AfterEach void verify() { server.verify(); }
    @Test void preferenceContainsExactTotalAndConfiguredLifetime() {
        var now = Instant.parse("2026-10-21T12:00:00Z");
        var attempt = new PaymentAttempt(intent, UUID.randomUUID(), "owner", "CREATING", null, null, now, now.plusSeconds(86400));
        server.expect(requestTo("https://api.mercadopago.com/checkout/preferences")).andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.external_reference").value(intent.toString()))
            .andExpect(jsonPath("$.items[0].unit_price").value(389.4))
            .andExpect(jsonPath("$.expiration_date_to").value("2026-10-22T12:00:00Z"))
            .andRespond(withSuccess("{\"id\":\"pref-1\",\"collector_id\":42,\"sandbox_init_point\":\"https://sandbox.mercadopago.com.pe/checkout/test\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/users/me")).andRespond(withSuccess("{\"id\":42}", MediaType.APPLICATION_JSON));
        var price = new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18")).quote(new HectaresQuota(new BigDecimal("0.5")));
        assertEquals("pref-1", gateway.create(attempt, price).preferenceId());
    }
    @Test void realPaymentCannotEnterSandboxEvenWithApprovedStatus() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/123"))
            .andRespond(withSuccess("{\"id\":123,\"collector_id\":42,\"live_mode\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/users/me")).andRespond(withSuccess("{\"id\":42}", MediaType.APPLICATION_JSON));
        assertThrows(PaymentGateway.Unavailable.class, () -> gateway.verify("123"));
    }
    @Test void receiverMismatchCannotActivateContract() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/123"))
            .andRespond(withSuccess("{\"id\":123,\"collector_id\":99,\"live_mode\":false}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/users/me")).andRespond(withSuccess("{\"id\":42}", MediaType.APPLICATION_JSON));
        assertThrows(PaymentGateway.Unavailable.class, () -> gateway.verify("123"));
    }
    @Test void authoritativePaymentKeepsAmountAndReference() {
        var body = "{\"id\":123,\"collector_id\":42,\"live_mode\":false,\"external_reference\":\"" + intent + "\",\"status\":\"approved\",\"currency_id\":\"PEN\",\"transaction_amount\":389.40,\"date_created\":\"2026-10-21T12:00:00Z\",\"date_approved\":\"2026-10-21T12:01:00Z\"}";
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/123")).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/users/me")).andRespond(withSuccess("{\"id\":42}", MediaType.APPLICATION_JSON));
        var payment = gateway.verify("123");
        assertEquals(intent, payment.intentId()); assertEquals(new Money(new BigDecimal("389.40")), payment.amount());
    }
    @Test void verifiedTestSellerCanReportLiveModeInSandbox() {
        var body = "{\"id\":123,\"collector_id\":42,\"live_mode\":true,\"external_reference\":\"" + intent + "\",\"status\":\"approved\",\"currency_id\":\"PEN\",\"transaction_amount\":389.40,\"date_created\":\"2026-10-21T12:00:00Z\",\"date_approved\":\"2026-10-21T12:01:00Z\"}";
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/123")).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/users/me")).andRespond(withSuccess("{\"id\":42,\"tags\":[\"test_user\"]}", MediaType.APPLICATION_JSON));
        assertEquals("approved", gateway.verify("123").status());
    }
    @Test void testSellerCannotEnterProductionEvenWhenLiveModeIsTrue() {
        var builder = RestClient.builder().baseUrl("https://api.mercadopago.com");
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new MercadoPagoGateway(true, false, "fixture-token", "https://example.test/notify", "https://example.test/return", builder.build());
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/123"))
            .andRespond(withSuccess("{\"id\":123,\"collector_id\":42,\"live_mode\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/users/me")).andRespond(withSuccess("{\"id\":42,\"tags\":[\"test_user\"]}", MediaType.APPLICATION_JSON));
        assertThrows(PaymentGateway.Unavailable.class, () -> gateway.verify("123"));
    }

}
