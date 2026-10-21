package com.arcadiadevs.viora.platform.subscription.application;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import com.arcadiadevs.viora.platform.subscription.application.ports.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.*;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import static org.junit.jupiter.api.Assertions.*;
class CheckoutServiceTest {
    static class Store implements SubscriptionRepository, PaymentAttemptRepository, SubscriptionUnitOfWork {
        final Map<UUID, Subscription> contracts = new HashMap<>();
        final Map<UUID, PaymentAttempt> attempts = new HashMap<>();
        final Map<String, Operation> operations = new HashMap<>();
        public Subscription save(Subscription s) { contracts.put(s.snapshot().id(), s); return s; }
        public Optional<Subscription> findById(UUID id) { return Optional.ofNullable(contracts.get(id)); }
        public Optional<Subscription> findCurrent(String p) { return contracts.values().stream().filter(s -> s.snapshot().producerId().equals(p)).findFirst(); }
        public PaymentAttempt save(PaymentAttempt a) { attempts.put(a.id(), a); return a; }
        public Optional<PaymentAttempt> find(UUID id) { return Optional.ofNullable(attempts.get(id)); }
        public Optional<PaymentAttempt> latest(UUID id) { return attempts.values().stream().filter(a -> a.subscriptionId().equals(id)).max(Comparator.comparing(PaymentAttempt::createdAt)); }
        public <T> T exclusive(String p, Supplier<T> action) { return action.get(); }
        public Optional<Operation> findOperation(String p, String key) { return Optional.ofNullable(operations.get(p + key)); }
        public void recordOperation(String p, String key, String kind, String hash, UUID id) { operations.put(p + key, new Operation(kind, hash, id)); }
    }
    static class Gateway implements PaymentGateway {
        int creates;
        boolean loseResponse;
        boolean configured = true;
        VerifiedPayment payment;
        Checkout existing;
        public boolean configured() { return configured; }
        public Checkout create(PaymentAttempt a, SubscriptionPrice price) {
            creates++; existing = new Checkout("preference-1", "https://www.mercadopago.com.pe/checkout/test");
            if (loseResponse) { throw new Unavailable(); }
            return existing;
        }
        public Optional<Checkout> recover(UUID id) { return Optional.ofNullable(existing); }
        public VerifiedPayment verify(String id) { return payment; }
        public List<VerifiedPayment> findPayments(UUID id) { return payment == null ? List.of() : List.of(payment); }
    }
    final Instant now = Instant.parse("2026-10-21T12:00:00Z");
    final Store store = new Store();
    final Gateway gateway = new Gateway();
    final List<Object> events = new ArrayList<>();
    final HectaresQuota quota = new HectaresQuota(new BigDecimal("5"));
    final SubscriptionPrice price = new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18")).quote(quota);
    final Subscription sub = Subscription.create("owner", "PRODUCER_ANNUAL", quota, price, now.minusSeconds(10));
    final CheckoutService service = new CheckoutService(store, store, store, gateway, events::add,
            Clock.fixed(now.plusSeconds(20), ZoneOffset.UTC), Duration.ofHours(24));
    @BeforeEach void setup() { store.save(sub); }
    @Test void repeatedClicksReusePreferenceAcrossDifferentKeys() {
        var first = service.create("owner", sub.snapshot().id(), "k1").success().orElseThrow();
        var second = service.create("owner", sub.snapshot().id(), "k2").success().orElseThrow();
        assertEquals(first.intentId(), second.intentId()); assertEquals(1, gateway.creates);
        assertFalse(sub.hasActiveEntitlement(now.plusSeconds(20)));
        assertEquals(now.plusSeconds(20).plus(Duration.ofHours(24)), first.expiresAt());
    }
    @Test void lostProviderResponseIsRecoveredWithoutSecondCreate() {
        gateway.loseResponse = true;
        assertTrue(service.create("owner", sub.snapshot().id(), "k").isFailure());
        gateway.loseResponse = false;
        assertTrue(service.create("owner", sub.snapshot().id(), "k").isSuccess());
        assertEquals(1, gateway.creates);
    }
    @Test void unresolvedAttemptDoesNotGenerateAnotherPreference() {
        gateway.loseResponse = true;
        service.create("owner", sub.snapshot().id(), "k"); gateway.existing = null;
        assertTrue(service.create("owner", sub.snapshot().id(), "new-key").isFailure());
        assertEquals(1, gateway.creates);
    }
    @Test void approvalIsDurableAndRepeatedNotificationDoesNotExtendEntitlement() {
        var checkout = service.create("owner", sub.snapshot().id(), "k").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "approved", price.total(), "PEN",
                now.plusSeconds(21), now.plusSeconds(22));
        assertTrue(service.confirm("123").isSuccess());
        var expiry = sub.snapshot().period().endsAt();
        assertTrue(service.confirm("123").isSuccess());
        assertEquals(expiry, sub.snapshot().period().endsAt()); assertEquals(1, events.size());
    }
    @Test void paymentMismatchNeverActivates() {
        var checkout = service.create("owner", sub.snapshot().id(), "k").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "approved", new Money(BigDecimal.ONE), "PEN",
                now.plusSeconds(21), now.plusSeconds(22));
        assertTrue(service.confirm("123").isFailure()); assertEquals("PENDING_PAYMENT", sub.snapshot().status());
    }
    @Test void rejectionReusesStillValidCheckoutRatherThanOpeningParallelPayment() {
        var checkout = service.create("owner", sub.snapshot().id(), "k").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "rejected", price.total(), "PEN", now.plusSeconds(21), null);
        service.confirm("123");
        assertTrue(service.create("owner", sub.snapshot().id(), "retry").isSuccess()); assertEquals(1, gateway.creates);
    }
    @Test void wrongOwnerAndDisabledGatewayDoNotCreateAnything() {
        assertTrue(service.create("intruder", sub.snapshot().id(), "k").isFailure());
        gateway.configured = false;
        assertTrue(service.create("owner", sub.snapshot().id(), "k").isFailure()); assertEquals(0, gateway.creates);
    }
    @Test void attachingCheckoutCannotUndoAnEarlyApproval() {
        var attempt = new PaymentAttempt(UUID.randomUUID(), sub.snapshot().id(), "owner", "APPROVED", null, null, now, now.plusSeconds(3600));
        assertEquals("APPROVED", attempt.withCheckout("p", "https://www.mercadopago.com.pe/").status());
    }
}
