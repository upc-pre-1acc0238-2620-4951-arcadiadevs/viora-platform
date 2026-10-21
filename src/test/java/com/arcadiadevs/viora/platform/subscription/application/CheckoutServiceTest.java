package com.arcadiadevs.viora.platform.subscription.application;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import com.arcadiadevs.viora.platform.subscription.application.internal.commandservices.CheckoutServiceImpl;
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
        public Subscription save(Subscription s) { contracts.put(UUID.fromString(s.snapshot().id().subscriptionId()), s); return s; }
        public Optional<Subscription> findById(SubscriptionId id) { return Optional.ofNullable(contracts.get(UUID.fromString(id.subscriptionId()))); }
        public Optional<Subscription> findByIdAndProducerId(SubscriptionId id, ProducerId owner) {
            return findById(id).filter(subscription -> subscription.snapshot().producerId().equals(owner));
        }
        public Optional<Subscription> findCurrent(ProducerId p, Instant at) { return contracts.values().stream().filter(s -> s.snapshot().producerId().equals(p)).findFirst(); }
        public boolean existsActiveByProducerId(ProducerId p, Instant at) { return contracts.values().stream().anyMatch(s -> s.snapshot().producerId().equals(p) && s.hasActiveEntitlement(at)); }
        public boolean existsPendingByProducerId(ProducerId p) { return contracts.values().stream().anyMatch(s -> s.snapshot().producerId().equals(p) && s.snapshot().status() == SubscriptionStatus.PENDING_PAYMENT); }
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
    final IndividualHectaresQuota quota = new IndividualHectaresQuota(new BigDecimal("5"));
    final SubscriptionPrice price = new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18")).quote(quota);
    final Subscription sub = Subscription.createIndividual(new ProducerId("550e8400-e29b-41d4-a716-446655440000"), "PRODUCER_ANNUAL", quota, price, now.minusSeconds(10));
    final UUID contractId = UUID.fromString(sub.snapshot().id().subscriptionId());
    final CheckoutService service = new CheckoutServiceImpl(store, store, store, gateway, events::add,
            Clock.fixed(now.plusSeconds(20), ZoneOffset.UTC), Duration.ofHours(24));
    @BeforeEach void setup() { store.save(sub); }
    @Test void repeatedClicksReusePreferenceAcrossDifferentKeys() {
        var first = service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "k1").success().orElseThrow();
        var second = service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "k2").success().orElseThrow();
        assertEquals(first.intentId(), second.intentId()); assertEquals(1, gateway.creates);
        assertFalse(sub.hasActiveEntitlement(now.plusSeconds(20)));
        assertEquals(now.plusSeconds(20).plus(Duration.ofHours(24)), first.expiresAt());
    }
    @Test void lostProviderResponseIsRecoveredWithoutSecondCreate() {
        gateway.loseResponse = true;
        assertTrue(service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "k").isFailure());
        gateway.loseResponse = false;
        assertTrue(service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "k").isSuccess());
        assertEquals(1, gateway.creates);
    }
    @Test void unresolvedAttemptDoesNotGenerateAnotherPreference() {
        gateway.loseResponse = true;
        service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "k"); gateway.existing = null;
        assertTrue(service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "new-key").isFailure());
        assertEquals(1, gateway.creates);
    }
    @Test void approvalIsDurableAndRepeatedNotificationDoesNotExtendEntitlement() {
        var checkout = service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "k").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "approved", price.total(), "PEN",
                now.plusSeconds(21), now.plusSeconds(22));
        assertTrue(service.confirm("123").isSuccess());
        var expiry = sub.snapshot().period().endsAt();
        assertTrue(service.confirm("123").isSuccess());
        assertEquals(expiry, sub.snapshot().period().endsAt()); assertEquals(1, events.size());
    }
    @Test void paymentMismatchNeverActivates() {
        var checkout = service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "k").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "approved", new Money(BigDecimal.ONE), "PEN",
                now.plusSeconds(21), now.plusSeconds(22));
        assertTrue(service.confirm("123").isFailure()); assertEquals(SubscriptionStatus.PENDING_PAYMENT, sub.snapshot().status());
    }
    @Test void rejectionReusesStillValidCheckoutRatherThanOpeningParallelPayment() {
        var checkout = service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "k").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "rejected", price.total(), "PEN", now.plusSeconds(21), null);
        service.confirm("123");
        assertTrue(service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "retry").isSuccess()); assertEquals(1, gateway.creates);
    }
    @Test void wrongOwnerAndDisabledGatewayDoNotCreateAnything() {
        assertTrue(service.create("intruder", contractId, "k").isFailure());
        gateway.configured = false;
        assertTrue(service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "k").isFailure()); assertEquals(0, gateway.creates);
    }
    @Test void attachingCheckoutCannotUndoAnEarlyApproval() {
        var attempt = new PaymentAttempt(UUID.randomUUID(), contractId, "550e8400-e29b-41d4-a716-446655440000", "APPROVED", null, null, now, now.plusSeconds(3600));
        assertEquals("APPROVED", attempt.withCheckout("p", "https://www.mercadopago.com.pe/").status());
    }
    @Test void earlierRejectionCannotHidePendingTransaction() {
        var checkout = service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "k").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "pending", price.total(), "PEN", now.plusSeconds(21), null);
        service.confirm("123");
        gateway.payment = new PaymentGateway.VerifiedPayment("122", checkout.intentId(), "rejected", price.total(), "PEN", now.plusSeconds(21), null);
        service.confirm("122");
        assertEquals("PENDING", service.latest(contractId).orElseThrow().status());
        assertFalse(sub.hasActiveEntitlement(now.plusSeconds(22)));
    }
    @Test void pendingPaymentCannotOpenAnotherCheckoutAndItsOwnRejectionUnlocksRetry() {
        var checkout = service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "k").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "pending", price.total(), "PEN", now.plusSeconds(21), null);
        service.confirm("123");
        assertTrue(service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "k").isFailure());
        assertTrue(service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "other").isFailure());
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "rejected", price.total(), "PEN", now.plusSeconds(21), null);
        service.confirm("123");
        assertEquals("REJECTED", service.latest(contractId).orElseThrow().status());
        assertTrue(service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "retry").isSuccess());
        assertEquals(1, gateway.creates);
    }
}
