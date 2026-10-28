package com.arcadiadevs.viora.platform.subscription.application;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import com.arcadiadevs.viora.platform.shared.application.result.*;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.arcadiadevs.viora.platform.subscription.application.internal.commandservices.SubscriptionCommandServiceImpl;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionCatalog;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.CreateSubscriptionCommand;
import com.arcadiadevs.viora.platform.subscription.application.internal.commandservices.CheckoutServiceImpl;
import com.arcadiadevs.viora.platform.subscription.application.ports.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtension;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.*;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import static org.junit.jupiter.api.Assertions.*;
class CheckoutServiceTest {
    static class Store implements SubscriptionRepository, PaymentAttemptRepository, SubscriptionUnitOfWork, QuotaExtensionRepository {
        final Map<String, QuotaExtension> extensions = new HashMap<>();
        public QuotaExtension save(QuotaExtension e) { extensions.put(e.snapshot().id().quotaExtensionId(), e); return e; }
        public Optional<QuotaExtension> findById(QuotaExtensionId id) { return Optional.ofNullable(extensions.get(id.quotaExtensionId())); }
        public boolean existsByExtensionCodeIdAndProducerId(ExtensionCodeId code, ProducerId producer) { return false; }
        public Optional<PaymentAttempt> latestForExtension(UUID id) { return attempts.values().stream().filter(a -> id.equals(a.extensionId())).max(Comparator.comparing(PaymentAttempt::createdAt)); }
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
        public Optional<Subscription> findPendingByProducerId(ProducerId p) { return contracts.values().stream().filter(s -> s.snapshot().producerId().equals(p) && s.snapshot().status() == SubscriptionStatus.PENDING_PAYMENT).findFirst(); }
        public PaymentAttempt save(PaymentAttempt a) { attempts.put(a.id(), a); return a; }
        public Optional<PaymentAttempt> find(UUID id) { return Optional.ofNullable(attempts.get(id)); }
        public Optional<PaymentAttempt> latest(UUID id) { return attempts.values().stream().filter(a -> a.subscriptionId().equals(id) && a.extensionId() == null).max(Comparator.comparing(PaymentAttempt::createdAt)); }
        public <T> T exclusive(String p, Supplier<T> action) { return action.get(); }
        public Optional<Operation> findOperation(String p, String key) { return Optional.ofNullable(operations.get(p + key)); }
        public void recordOperation(String p, String key, String kind, String hash, UUID id) { operations.put(p + key, new Operation(kind, hash, id)); }
    }
    static class Gateway implements PaymentGateway {
        int creates;
        boolean loseResponse;
        boolean omitSearchResults;
        boolean failSearch;
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
        public List<VerifiedPayment> findPayments(UUID id) {
            if (failSearch) { throw new Unavailable(); }
            return payment == null || omitSearchResults ? List.of() : List.of(payment);
        }
        final List<String> closed = new ArrayList<>();
        public void close(String preferenceId, Instant at) { closed.add(preferenceId); }
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
            Clock.fixed(now.plusSeconds(20), ZoneOffset.UTC), Duration.ofHours(24), store);
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
    @Test void aRefundOrChargeBackEndsTheAccessOnce() {
        var checkout = service.create("550e8400-e29b-41d4-a716-446655440000", contractId, "k").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "approved", price.total(), "PEN",
                now.plusSeconds(21), now.plusSeconds(22));
        assertTrue(service.confirm("123").isSuccess());
        assertTrue(sub.hasActiveEntitlement(now.plusSeconds(30)));

        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "refunded", price.total(), "PEN",
                now.plusSeconds(21), now.plusSeconds(22));
        assertTrue(service.confirm("123").isSuccess());
        assertTrue(service.confirm("123").isSuccess());

        assertEquals(SubscriptionStatus.CANCELLED, sub.snapshot().status());
        assertFalse(sub.hasActiveEntitlement(now.plusSeconds(30)));
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

    private final String producer = "550e8400-e29b-41d4-a716-446655440000";
    private final SubscriptionPricingPolicy pricing = new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18"));
    private final SubscriptionCommandServiceImpl purchases = new SubscriptionCommandServiceImpl(store, store,
            new SubscriptionCatalog() {
                public List<PlanOffer> plans() { return List.of(); }
                public SubscriptionPrice quote(String plan, IndividualHectaresQuota q) { return pricing.quote(q); }
                public SubscriptionPrice quoteExtension(String plan, IndividualHectaresQuota q, int months) { throw new UnsupportedOperationException(); }
            }, owner -> true, Clock.fixed(now.plusSeconds(30), ZoneOffset.UTC), service);
    private Result<SubscriptionCommandService.CreatedSubscription, ApplicationError> buy(String hectares, String key) {
        return purchases.handle(new CreateSubscriptionCommand(producer, "PRODUCER_ANNUAL", new BigDecimal(hectares), key));
    }
    @Test void anUnpaidPurchaseTakesTheNewSelectionAndItsOpenLinkIsClosed() {
        var checkout = service.create(producer, contractId, "k").success().orElseThrow();
        var requoted = buy("2", "other-size").success().orElseThrow();
        assertEquals(contractId, requoted.id());
        assertEquals(0, new BigDecimal("2").compareTo(sub.snapshot().quota().hectares()));
        assertEquals(pricing.quote(new IndividualHectaresQuota(new BigDecimal("2"))), sub.snapshot().price());
        assertEquals(List.of("preference-1"), gateway.closed);
        assertEquals("EXPIRED", store.find(checkout.intentId()).orElseThrow().status());
        assertNotEquals(checkout.intentId(), service.create(producer, contractId, "k2").success().orElseThrow().intentId());
    }
    @Test void aPurchaseWithoutCheckoutTakesTheNewSelectionWithoutTheProvider() {
        assertTrue(buy("0.5", "smaller").isSuccess());
        assertEquals(0, new BigDecimal("0.5").compareTo(sub.snapshot().quota().hectares()));
        assertTrue(gateway.closed.isEmpty());
    }
    @Test void theSameSelectionKeepsThePurchaseAndItsLink() {
        service.create(producer, contractId, "k");
        assertEquals(contractId, buy("5", "same").success().orElseThrow().id());
        assertEquals(price, sub.snapshot().price());
        assertTrue(gateway.closed.isEmpty());
    }
    @Test void anUnresolvedPaymentKeepsThePurchaseAsItIs() {
        var checkout = service.create(producer, contractId, "k").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "in_process", price.total(), "PEN", now.plusSeconds(21), null);
        assertTrue(buy("2", "other-size").isFailure());
        assertEquals(price, sub.snapshot().price());
        assertTrue(gateway.closed.isEmpty());
        assertEquals("PENDING", store.find(checkout.intentId()).orElseThrow().status());
    }
    @Test void aRejectedPaymentStillLetsThePurchaseChangeAndClosesItsLink() {
        var checkout = service.create(producer, contractId, "k").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "rejected", price.total(), "PEN", now.plusSeconds(21), null);
        service.confirm("123");
        assertTrue(buy("2", "other-size").isSuccess());
        assertEquals(List.of("preference-1"), gateway.closed);
    }
    @Test void aProviderFailureKeepsThePurchaseAsItIs() {
        service.create(producer, contractId, "k");
        gateway.failSearch = true;
        assertTrue(buy("2", "other-size").isFailure());
        assertEquals(price, sub.snapshot().price());
    }

    private CheckoutService afterExpiry() {
        return new CheckoutServiceImpl(store, store, store, gateway, events::add,
                Clock.fixed(now.plus(Duration.ofHours(25)), ZoneOffset.UTC), Duration.ofHours(24), store);
    }
    @Test void expiredPendingObservationSurvivesAnEmptyProviderSearch() {
        var checkout = service.create(sub.snapshot().producerId().producerId(), contractId, "initial").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "pending", price.total(), "PEN", now.plusSeconds(21), null);
        service.confirm("123");
        gateway.omitSearchResults = true;
        assertTrue(afterExpiry().create(sub.snapshot().producerId().producerId(), contractId, "retry").isFailure());
        assertEquals("PENDING", store.find(checkout.intentId()).orElseThrow().status());
        assertEquals(1, gateway.creates);
    }
    @Test void expiredOpenCheckoutCanBeReplacedAfterAuthoritativeEmptySearch() {
        var first = service.create(sub.snapshot().producerId().producerId(), contractId, "initial").success().orElseThrow();
        var next = afterExpiry().create(sub.snapshot().producerId().producerId(), contractId, "retry").success().orElseThrow();
        assertNotEquals(first.intentId(), next.intentId());
        assertEquals("EXPIRED", store.find(first.intentId()).orElseThrow().status());
        assertEquals(2, gateway.creates);
    }
    @Test void expiredOperationKeyCannotBeReusedForANewAttempt() {
        service.create(sub.snapshot().producerId().producerId(), contractId, "initial");
        assertTrue(afterExpiry().create(sub.snapshot().producerId().producerId(), contractId, "initial").isFailure());
        assertEquals(1, gateway.creates);
    }
    @Test void providerFailureDuringExpiryReconciliationDoesNotCreateAgain() {
        service.create(sub.snapshot().producerId().producerId(), contractId, "initial");
        gateway.failSearch = true;
        assertTrue(afterExpiry().create(sub.snapshot().producerId().producerId(), contractId, "retry").isFailure());
        assertEquals(1, gateway.creates);
    }
    @Test void confirmedRejectionOfExpiredPendingPaymentAllowsANewAttempt() {
        var first = service.create(sub.snapshot().producerId().producerId(), contractId, "initial").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", first.intentId(), "pending", price.total(), "PEN", now.plusSeconds(21), null);
        service.confirm("123");
        gateway.payment = new PaymentGateway.VerifiedPayment("123", first.intentId(), "rejected", price.total(), "PEN", now.plusSeconds(21), null);
        var next = afterExpiry().create(sub.snapshot().producerId().producerId(), contractId, "retry").success().orElseThrow();
        assertNotEquals(first.intentId(), next.intentId());
        assertEquals(2, gateway.creates);
    }

    @Test void legacyApprovalAndReplayPreserveStoredAmountAndPeriod() {
        var legacyOwner = UUID.randomUUID().toString();
        var legacyPrice = new SubscriptionPrice(new Money(new BigDecimal("330.00")),
                new Money(new BigDecimal("59.40")), new Money(new BigDecimal("389.40")));
        var legacy = Subscription.createIndividual(new ProducerId(legacyOwner), "PRODUCER_ANNUAL",
                new IndividualHectaresQuota(new BigDecimal("0.5")), legacyPrice, now.minusSeconds(10));
        store.save(legacy);
        var legacyId = UUID.fromString(legacy.snapshot().id().subscriptionId());
        var checkout = service.create(legacyOwner, legacyId, "legacy-price").success().orElseThrow();
        gateway.payment = new PaymentGateway.VerifiedPayment("123", checkout.intentId(), "approved", legacyPrice.total(),
                "PEN", now.plusSeconds(21), now.plusSeconds(22));
        assertTrue(service.confirm("123").isSuccess());
        var approved = legacy.snapshot();
        assertEquals(legacyPrice, approved.price());
        assertEquals(SubscriptionStatus.ACTIVE, approved.status());
        assertTrue(service.confirm("123").isSuccess());
        assertEquals(approved, legacy.snapshot());
    }
}
