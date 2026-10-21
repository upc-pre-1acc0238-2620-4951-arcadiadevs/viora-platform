package com.arcadiadevs.viora.platform.subscription.application.commandservices;

import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import com.arcadiadevs.viora.platform.shared.application.result.*;
import com.arcadiadevs.viora.platform.subscription.application.ports.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
/** Coordinates durable attempts without holding a database transaction during provider requests. */
public class CheckoutService {
    private final SubscriptionRepository subscriptions;
    private final PaymentAttemptRepository attempts;
    private final SubscriptionUnitOfWork work;
    private final PaymentGateway gateway;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final Duration ttl;
    /** @param subscriptions contracts @param attempts payment attempts @param work owner serialization @param gateway provider @param events domain events @param clock time @param ttl link lifetime */
    public CheckoutService(SubscriptionRepository subscriptions, PaymentAttemptRepository attempts,
            SubscriptionUnitOfWork work, PaymentGateway gateway, ApplicationEventPublisher events, Clock clock, Duration ttl) {
        this.subscriptions = subscriptions; this.attempts = attempts; this.work = work;
        this.gateway = gateway; this.events = events; this.clock = clock; this.ttl = ttl;
        if (ttl.isNegative() || ttl.isZero()) { throw new IllegalArgumentException("subscription.checkout.ttl.invalid"); }
    }
    public record CheckoutResult(UUID intentId, String checkoutUrl, Instant expiresAt) { }
    private record Reservation(PaymentAttempt attempt, SubscriptionPrice price, boolean created) { }
    /** @param owner verified actor @param subscriptionId owned contract @param key operation id @return checkout, never an active entitlement */
    public Result<CheckoutResult, ApplicationError> create(String owner, UUID subscriptionId, String key) {
        if (key == null || key.isBlank() || key.length() > 128) { return invalid("subscription.operation.invalid"); }
        var contract = subscriptions.findById(subscriptionId);
        if (contract.isEmpty() || !contract.get().snapshot().producerId().equals(owner)) {
            return Result.failure(ApplicationError.notFound("subscription", subscriptionId.toString()));
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
                work.exclusive(owner, () -> { attempts.save(attempts.find(old.id()).orElseThrow().withStatus("EXPIRED")); return true; });
            } catch (PaymentGateway.Unavailable ex) { return unavailable(); }
        }
        Result<Reservation, ApplicationError> reserved = work.exclusive(owner, () -> {
            var sub = subscriptions.findById(subscriptionId).orElseThrow();
            if (!"PENDING_PAYMENT".equals(sub.snapshot().status())) { return conflict("subscription.checkout.state"); }
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
                return Result.success(new Reservation(previous, sub.snapshot().price(), false));
            }
            var active = attempts.latest(subscriptionId).filter(a -> !Set.of("EXPIRED").contains(a.status()));
            if (active.isPresent()) {
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
    /** @param paymentId provider identifier from an authenticated webhook @return acknowledgement only after durable processing */
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
            var sub = subscriptions.findById(current.subscriptionId()).orElseThrow();
            if (!"PEN".equals(payment.currency()) || !sub.snapshot().price().total().equals(payment.amount())
                    || payment.initiatedAt() == null || payment.initiatedAt().isBefore(current.createdAt().minusSeconds(5))
                    || !payment.initiatedAt().isBefore(current.expiresAt())) {
                return invalid("subscription.payment.mismatch");
            }
            if ("approved".equals(payment.status())) {
                if (payment.approvedAt() == null || payment.approvedAt().isAfter(clock.instant().plusSeconds(300))) {
                    return invalid("subscription.payment.mismatch");
                }
                try { sub.activate(payment.id(), payment.amount(), payment.currency(), payment.approvedAt()); }
                catch (IllegalArgumentException | IllegalStateException ex) { return conflict(ex.getMessage()); }
                subscriptions.save(sub);
                attempts.save(current.withStatus("APPROVED"));
                sub.domainEvents().forEach(events::publishEvent);
                sub.clearDomainEvents();
            } else if (!"APPROVED".equals(current.status())) {
                // An earlier failed transaction must not hide a newer transaction still being processed.
                if (Set.of("pending", "in_process", "authorized", "in_mediation").contains(payment.status())) {
                    attempts.save(current.withStatus("PENDING"));
                } else if (Set.of("rejected", "cancelled").contains(payment.status())) {
                    attempts.save(current.withStatus("REJECTED"));
                }
            }
            return Result.success(true);
        });
    }
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
