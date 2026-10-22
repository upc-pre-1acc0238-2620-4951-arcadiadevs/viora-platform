package com.arcadiadevs.viora.platform.subscription.application.internal.commandservices;

import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import com.arcadiadevs.viora.platform.shared.application.result.*;
import com.arcadiadevs.viora.platform.subscription.application.ports.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
/** Coordinates durable attempts without holding a database transaction during provider requests. */
@Service
public class CheckoutServiceImpl implements CheckoutService {
    private final SubscriptionRepository subscriptions;
    private final PaymentAttemptRepository attempts;
    private final SubscriptionUnitOfWork work;
    private final PaymentGateway gateway;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final Duration ttl;
    /**
     * Creates the durable individual checkout use case.
     * @param subscriptions shared contract repository port
     * @param attempts durable attempt repository port
     * @param work transactional producer serialization
     * @param gateway hosted payment provider port
     * @param events application domain-event publisher
     * @param clock application time source
     * @param ttl configured hosted-link lifetime
     * @throws IllegalArgumentException if the lifetime is not positive
     */
    public CheckoutServiceImpl(SubscriptionRepository subscriptions, PaymentAttemptRepository attempts,
            SubscriptionUnitOfWork work, PaymentGateway gateway, ApplicationEventPublisher events, Clock clock, @org.springframework.beans.factory.annotation.Value("${viora.payments.checkout-ttl}") Duration ttl) {
        this.subscriptions = subscriptions; this.attempts = attempts; this.work = work;
        this.gateway = gateway; this.events = events; this.clock = clock; this.ttl = ttl;
        if (ttl.isNegative() || ttl.isZero()) { throw new IllegalArgumentException("subscription.checkout.ttl.invalid"); }
    }
    private record Reservation(PaymentAttempt attempt, SubscriptionPrice price, boolean created) { }
    /**
     * Reserves or recovers a hosted checkout after checking ownership and unresolved payments.
     * @param owner authenticated producer identifier
     * @param subscriptionId requested contract identifier
     * @param key durable client operation identifier
     * @return hosted navigation data or a consistent application failure
     */
    @Override
    public Result<CheckoutResult, ApplicationError> create(String owner, UUID subscriptionId, String key) {
        if (key == null || key.isBlank() || key.length() > 128) { return invalid("subscription.operation.invalid"); }
        final ProducerId producer;
        try { producer = new ProducerId(owner); }
        catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.notFound("subscription", subscriptionId.toString()));
        }
        var contract = subscriptions.findByIdAndProducerId(new SubscriptionId(subscriptionId.toString()), producer);
        if (contract.isEmpty()) {
            return Result.failure(ApplicationError.notFound("subscription", subscriptionId.toString()));
        }
        if (contract.get().snapshot().status() != SubscriptionStatus.PENDING_PAYMENT
                || contract.get().snapshot().mode() != PlanMode.PLAN_PRODUCTOR_INDIVIDUAL) {
            return conflict("subscription.checkout.state");
        }
        if (!gateway.configured()) { return unavailable(); }
        // Expired links are reconciled before another attempt can be reserved.
        var latest = attempts.latest(subscriptionId);
        if (latest.isPresent() && !clock.instant().isBefore(latest.get().expiresAt())
                && !Set.of("EXPIRED").contains(latest.get().status())) {
            try {
                var payments = gateway.findPayments(latest.get().id());
                for (var payment : payments) { var applied = apply(payment); if (applied.isFailure()) { return Result.failure(applied.failure().orElseThrow()); } }
                if (payments.stream().anyMatch(p -> !Set.of("rejected", "cancelled").contains(p.status()))) {
                    return conflict("subscription.payment.unresolved");
                }
                var old = latest.get();
                Result<Boolean, ApplicationError> expiration = work.exclusive(owner, () -> {
                    var stored = attempts.find(old.id()).orElseThrow();
                    if ("PENDING".equals(stored.status())) { return conflict("subscription.payment.unresolved"); }
                    if ("APPROVED".equals(stored.status())) { return conflict("subscription.checkout.state"); }
                    attempts.save(stored.withStatus("EXPIRED"));
                    return Result.success(true);
                });
                if (expiration.isFailure()) { return Result.failure(expiration.failure().orElseThrow()); }
            } catch (PaymentGateway.Unavailable ex) { return unavailable(); }
        }
        Result<Reservation, ApplicationError> reserved = work.exclusive(owner, () -> {
            var sub = subscriptions.findByIdAndProducerId(new SubscriptionId(subscriptionId.toString()), producer).orElseThrow();
            if (sub.snapshot().status() != SubscriptionStatus.PENDING_PAYMENT || sub.snapshot().mode() != PlanMode.PLAN_PRODUCTOR_INDIVIDUAL) { return conflict("subscription.checkout.state"); }
            var operation = work.findOperation(owner, key);
            if (operation.isPresent()) {
                var op = operation.get();
                if (!op.kind().equals("CREATE_CHECKOUT") || !op.fingerprint().equals(subscriptionId.toString())) {
                    return conflict("subscription.operation.conflict");
                }
                var previous = attempts.find(op.resourceId()).orElseThrow();
                if (!clock.instant().isBefore(previous.expiresAt()) || Set.of("EXPIRED").contains(previous.status())) {
                    return conflict("subscription.checkout.expired");
                }
                if ("PENDING".equals(previous.status())) { return conflict("subscription.payment.unresolved"); }
                return Result.success(new Reservation(previous, sub.snapshot().price(), false));
            }
            var active = attempts.latest(subscriptionId).filter(a -> !Set.of("EXPIRED").contains(a.status()));
            if (active.isPresent()) {
                if ("PENDING".equals(active.get().status())) { return conflict("subscription.payment.unresolved"); }
                work.recordOperation(owner, key, "CREATE_CHECKOUT", subscriptionId.toString(), active.get().id());
                return Result.success(new Reservation(active.get(), sub.snapshot().price(), false));
            }
            var now = clock.instant();
            var attempt = attempts.save(new PaymentAttempt(UUID.randomUUID(), subscriptionId, owner,
                    "CREATING", null, null, now, now.plus(ttl)));
            work.recordOperation(owner, key, "CREATE_CHECKOUT", subscriptionId.toString(), attempt.id());
            return Result.success(new Reservation(attempt, sub.snapshot().price(), true));
        });
        if (reserved.isFailure()) { return Result.failure(reserved.failure().orElseThrow()); }
        var reservation = reserved.success().orElseThrow();
        var attempt = reservation.attempt();
        if (attempt.checkoutUrl() != null) {
            return Result.success(new CheckoutResult(attempt.id(), attempt.checkoutUrl(), attempt.expiresAt()));
        }
        try {
            PaymentGateway.Checkout checkout;
            if (reservation.created()) {
                checkout = gateway.create(attempt, reservation.price());
            } else {
                // Never repeat a POST when the prior create may have succeeded remotely.
                checkout = gateway.recover(attempt.id()).orElse(null);
                if (checkout == null) { return unavailable(); }
            }
            return work.exclusive(owner, () -> {
                var current = attempts.find(attempt.id()).orElseThrow();
                if (current.checkoutUrl() == null) { attempts.save(current.withCheckout(checkout.preferenceId(), checkout.checkoutUrl())); }
                return Result.success(new CheckoutResult(current.id(), checkout.checkoutUrl(), current.expiresAt()));
            });
        } catch (PaymentGateway.Unavailable ex) { return unavailable(); }
    }
    /**
     * Applies authoritative provider evidence rather than browser-return claims.
     * @param paymentId provider transaction identifier received through a trusted integration
     * @return acknowledgement after durable processing, or a consistent failure
     */
    @Override
    public Result<Boolean, ApplicationError> confirm(String paymentId) {
        try { return apply(gateway.verify(paymentId)); }
        catch (PaymentGateway.Unavailable ex) { return unavailable(); }
    }
    private Result<Boolean, ApplicationError> apply(PaymentGateway.VerifiedPayment payment) {
        var found = attempts.find(payment.intentId());
        if (found.isEmpty()) { return Result.failure(ApplicationError.notFound("payment_attempt", payment.intentId().toString())); }
        var attempt = found.get();
        return work.exclusive(attempt.producerId(), () -> {
            var current = attempts.find(attempt.id()).orElseThrow();
            var sub = subscriptions.findById(new SubscriptionId(current.subscriptionId().toString())).orElseThrow();
            if (sub.snapshot().mode() != PlanMode.PLAN_PRODUCTOR_INDIVIDUAL || sub.snapshot().price() == null
                    || !"PEN".equals(payment.currency()) || !sub.snapshot().price().total().equals(payment.amount())
                    || payment.initiatedAt() == null || payment.initiatedAt().isBefore(current.createdAt().minusSeconds(5))
                    || !payment.initiatedAt().isBefore(current.expiresAt())) {
                return invalid("subscription.payment.mismatch");
            }
            if ("approved".equals(payment.status())) {
                if (payment.approvedAt() == null || payment.approvedAt().isBefore(payment.initiatedAt()) || payment.approvedAt().isAfter(clock.instant().plusSeconds(300))) {
                    return invalid("subscription.payment.mismatch");
                }
                try { sub.activateIndividual(payment.id(), payment.amount(), payment.currency(), payment.approvedAt()); }
                catch (IllegalArgumentException | IllegalStateException ex) { return conflict(ex.getMessage()); }
                subscriptions.save(sub);
                attempts.save(current.withPaymentState(payment.id(), "approved"));
                sub.domainEvents().forEach(events::publishEvent);
                sub.clearDomainEvents();
            } else if (Set.of("pending", "in_process", "authorized", "in_mediation", "rejected", "cancelled").contains(payment.status())) {
                attempts.save(current.withPaymentState(payment.id(), payment.status()));
            }
            return Result.success(true);
        });
    }
    /**
     * Reads locally persisted attempt state without contacting the provider.
     * @param subscriptionId contract identifier already authorized by the caller
     * @return latest durable attempt when present
     */
    @Override
    public Optional<PaymentAttempt> latest(UUID subscriptionId) { return attempts.latest(subscriptionId); }
    private static <T> Result<T, ApplicationError> conflict(String key) {
        return Result.failure(ApplicationError.conflict("subscription", key));
    }
    private static <T> Result<T, ApplicationError> invalid(String key) {
        return Result.failure(ApplicationError.businessRuleViolation("subscription", key));
    }
    private static <T> Result<T, ApplicationError> unavailable() {
        return Result.failure(new ApplicationError("PAYMENT_UNAVAILABLE", "subscription.gateway.unavailable", "subscription.gateway.unavailable"));
    }
}
