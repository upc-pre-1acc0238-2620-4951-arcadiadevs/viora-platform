package com.arcadiadevs.viora.platform.subscription.application;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import com.arcadiadevs.viora.platform.subscription.application.internal.commandservices.SubscriptionCommandServiceImpl;
import com.arcadiadevs.viora.platform.subscription.application.ports.*;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionCatalog;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.CreateSubscriptionCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import static org.junit.jupiter.api.Assertions.*;
class SubscriptionCommandServiceTest {
    static class Store implements SubscriptionRepository, SubscriptionUnitOfWork {
        final Map<UUID, Subscription> rows = new HashMap<>();
        final Map<String, Operation> operations = new HashMap<>();
        public Subscription save(Subscription s) { rows.put(s.snapshot().id(), s); return s; }
        public Optional<Subscription> findById(UUID id) { return Optional.ofNullable(rows.get(id)); }
        public Optional<Subscription> findCurrent(String p) { return rows.values().stream().filter(s -> s.snapshot().producerId().equals(p)).findFirst(); }
        public <T> T exclusive(String p, Supplier<T> work) { return work.get(); }
        public Optional<Operation> findOperation(String p, String key) { return Optional.ofNullable(operations.get(p + key)); }
        public void recordOperation(String p, String k, String kind, String hash, UUID id) { operations.put(p + k, new Operation(kind, hash, id)); }
    }
    private final Store store = new Store();
    private final SubscriptionCatalog catalog = new SubscriptionCatalog() {
        public List<PlanOffer> plans() { return List.of(); }
        public SubscriptionPrice quote(String code, HectaresQuota quota) {
            if (!code.equals("PRODUCER_ANNUAL")) { throw new IllegalArgumentException("subscription.plan.invalid"); }
            return new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18")).quote(quota);
        }
    };
    private SubscriptionCommandServiceImpl service(boolean ready) {
        return new SubscriptionCommandServiceImpl(store, store, catalog, p -> ready, Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC));
    }
    private CreateSubscriptionCommand command(String owner, String key, String ha) {
        return new CreateSubscriptionCommand(owner, "PRODUCER_ANNUAL", new BigDecimal(ha), key);
    }
    @Test void lostResponseReplaysSameContract() {
        var service = service(true);
        var first = service.handle(command("a", "key", "5")).success().orElseThrow();
        var second = service.handle(command("a", "key", "5.00")).success().orElseThrow();
        assertEquals(first.id(), second.id());
        assertTrue(second.replayed());
        assertEquals(1, store.rows.size());
    }
    @Test void changingPayloadWithSameKeyConflicts() {
        var service = service(true);
        service.handle(command("a", "key", "5"));
        assertEquals("SUBSCRIPTION_CONFLICT", service.handle(command("a", "key", "6")).failure().orElseThrow().code());
    }
    @Test void anotherKeyCannotCreateSecondPendingContract() {
        var service = service(true);
        service.handle(command("a", "key", "5"));
        assertTrue(service.handle(command("a", "another", "5")).isFailure());
    }
    @Test void idempotencyIsScopedToOwner() {
        var service = service(true);
        service.handle(command("a", "key", "5"));
        assertTrue(service.handle(command("b", "key", "5")).isSuccess());
    }
    @Test void requiresReadyProfileAndValidOffer() {
        assertTrue(service(false).handle(command("a", "key", "5")).isFailure());
        assertTrue(service(true).handle(new CreateSubscriptionCommand("a", "UNKNOWN", BigDecimal.ONE, "k")).isFailure());
    }
}
