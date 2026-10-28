package com.arcadiadevs.viora.platform.subscription.application.internal.commandservices;

import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import com.arcadiadevs.viora.platform.shared.application.result.*;
import com.arcadiadevs.viora.platform.subscription.application.ports.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
/**
 * Coordinates durable attempts without holding a database transaction during provider requests. An attempt pays a
 * pending individual plan or a pending paid extension of a plan in force; both share the same provider flow.
 */
@Service
public class CheckoutServiceImpl implements CheckoutService {
    private final SubscriptionRepository subscriptions;
    private final PaymentAttemptRepository attempts;
    private final SubscriptionUnitOfWork work;
    private final PaymentGateway gateway;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final Duration ttl;
    private final QuotaExtensionRepository extensions;
    /**
     * Creates the durable individual checkout use case.
     * @param subscriptions shared contract repository port
     * @param attempts durable attempt repository port
     * @param work transactional producer serialization
     * @param gateway hosted payment provider port
     * @param events application domain-event publisher
     * @param clock application time source
     * @param ttl configured hosted-link lifetime
     * @param extensions paid plan extensions
     * @throws IllegalArgumentException if the lifetime is not positive
     */
    public CheckoutServiceImpl(SubscriptionRepository subscriptions, PaymentAttemptRepository attempts,
            SubscriptionUnitOfWork work, PaymentGateway gateway, ApplicationEventPublisher events, Clock clock,
            @org.springframework.beans.factory.annotation.Value("${viora.payments.checkout-ttl}") Duration ttl,
            QuotaExtensionRepository extensions) {
        this.subscriptions = subscriptions; this.attempts = attempts; this.work = work;
        this.gateway = gateway; this.events = events; this.clock = clock; this.ttl = ttl; this.extensions = extensions;
        if (ttl.isNegative() || ttl.isZero()) { throw new IllegalArgumentException("subscription.checkout.ttl.invalid"); }
    }
    private record Reservation(PaymentAttempt attempt, SubscriptionPrice price, boolean created) { }
    /**
     * What one checkout pays: the plan itself, or one of its extensions when {@code extensionId} is set.
     * @param subscriptionId owned plan
     * @param extensionId pending extension, or null
     * @param price server-owned amount to collect
     */
    private record Target(UUID subscriptionId, UUID extensionId, SubscriptionPrice price) {
        /** @return the identity the attempt and its operation fingerprint are keyed by */
        UUID key() { return extensionId == null ? subscriptionId : extensionId; }
    }
    /**
     * Reserves or recovers a hosted checkout after checking ownership and unresolved payments.
     * @param owner authenticated producer identifier
     * @param subscriptionId requested contract identifier
     * @param key durable client operation identifier
     * @return hosted navigation data or a consistent application failure
     */
    @Override
    public Result<CheckoutResult, ApplicationError> create(String owner, UUID subscriptionId, String key) {
        return create(owner, subscriptionId, null, key);
    }
    /**
     * Reserves or recovers a hosted checkout for a pending paid extension of an owned plan.
     * @param owner authenticated producer identifier
     * @param subscriptionId owned plan
     * @param extensionId pending extension of that plan
     * @param key durable client operation identifier
     * @return hosted navigation data or a consistent application failure
     */
    @Override
    public Result<CheckoutResult, ApplicationError> createForExtension(String owner, UUID subscriptionId,
            UUID extensionId, String key) {
        if (extensionId == null) {
            return Result.failure(ApplicationError.notFound("quota_extension", "null"));
        }
        return create(owner, subscriptionId, extensionId, key);
    }
    private Result<CheckoutResult, ApplicationError> create(String owner, UUID subscriptionId, UUID extensionId, String key) {
        if (key == null || key.isBlank() || key.length() > 128) { return invalid("subscription.operation.invalid"); }
        final ProducerId producer;
        try { producer = new ProducerId(owner); }
        catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.notFound("subscription", subscriptionId.toString()));
        }
        var payable = target(producer, subscriptionId, extensionId);
        if (payable.isFailure()) { return Result.failure(payable.failure().orElseThrow()); }
        if (!gateway.configured()) { return unavailable(); }
        var target = payable.success().orElseThrow();
        // Expired links are reconciled before another attempt can be reserved.
        var latest = latest(target);
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
            var current = target(producer, subscriptionId, extensionId);
            if (current.isFailure()) { return Result.failure(current.failure().orElseThrow()); }
            var operation = work.findOperation(owner, key);
            if (operation.isPresent()) {
                var op = operation.get();
                if (!op.kind().equals("CREATE_CHECKOUT") || !op.fingerprint().equals(target.key().toString())) {
                    return conflict("subscription.operation.conflict");
                }
                var previous = attempts.find(op.resourceId()).orElseThrow();
                if (!clock.instant().isBefore(previous.expiresAt()) || Set.of("EXPIRED").contains(previous.status())) {
                    return conflict("subscription.checkout.expired");
                }
                if ("PENDING".equals(previous.status())) { return conflict("subscription.payment.unresolved"); }
                return Result.success(new Reservation(previous, target.price(), false));
            }
            var active = latest(target).filter(a -> !Set.of("EXPIRED").contains(a.status()));
            if (active.isPresent()) {
                if ("PENDING".equals(active.get().status())) { return conflict("subscription.payment.unresolved"); }
                work.recordOperation(owner, key, "CREATE_CHECKOUT", target.key().toString(), active.get().id());
                return Result.success(new Reservation(active.get(), target.price(), false));
            }
            var now = clock.instant();
            var attempt = attempts.save(new PaymentAttempt(UUID.randomUUID(), subscriptionId, owner,
                    "CREATING", null, null, now, now.plus(ttl), Map.of(), target.extensionId()));
            work.recordOperation(owner, key, "CREATE_CHECKOUT", target.key().toString(), attempt.id());
            return Result.success(new Reservation(attempt, target.price(), true));
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
     * Closes the unpaid checkout of a pending plan so its terms can change. Provider evidence is reconciled first:
     * an approved or still unresolved payment keeps the plan as it is.
     * @param owner authenticated producer
     * @param subscriptionId owned pending plan
     * @return acknowledgement once no link can pay the current terms, or a consistent failure
     */
    @Override
    public Result<Boolean, ApplicationError> release(String owner, UUID subscriptionId) {
        var latest = attempts.latest(subscriptionId);
        if (latest.isEmpty() || "EXPIRED".equals(latest.get().status())) { return Result.success(true); }
        var open = latest.get();
        if (!owner.equals(open.producerId())) {
            return Result.failure(ApplicationError.notFound("subscription", subscriptionId.toString()));
        }
        if (Set.of("PENDING", "APPROVED").contains(open.status())) { return conflict("subscription.payment.unresolved"); }
        if (!gateway.configured()) { return unavailable(); }
        try {
            var payments = gateway.findPayments(open.id());
            for (var payment : payments) { var applied = apply(payment); if (applied.isFailure()) { return Result.failure(applied.failure().orElseThrow()); } }
            if (payments.stream().anyMatch(p -> !Set.of("rejected", "cancelled").contains(p.status()))) {
                return conflict("subscription.payment.unresolved");
            }
            // A rejected payment leaves the link open for another try, so it is closed as well.
            var preference = open.preferenceId() != null ? Optional.of(open.preferenceId())
                    : gateway.recover(open.id()).map(PaymentGateway.Checkout::preferenceId);
            if (preference.isPresent()) { gateway.close(preference.get(), clock.instant()); }
        } catch (PaymentGateway.Unavailable ex) { return unavailable(); }
        return work.exclusive(owner, () -> {
            var stored = attempts.find(open.id()).orElseThrow();
            if (Set.of("PENDING", "APPROVED").contains(stored.status())) { return conflict("subscription.payment.unresolved"); }
            if (!"EXPIRED".equals(stored.status())) { attempts.save(stored.withStatus("EXPIRED")); }
            return Result.success(true);
        });
    }
    /** Resolves what is payable now: a pending individual plan, or a pending paid extension of a plan in force. */
    private Result<Target, ApplicationError> target(ProducerId producer, UUID subscriptionId, UUID extensionId) {
        var contract = subscriptions.findByIdAndProducerId(new SubscriptionId(subscriptionId.toString()), producer);
        if (contract.isEmpty()) {
            return Result.failure(ApplicationError.notFound("subscription", subscriptionId.toString()));
        }
        var plan = contract.get().snapshot();
        if (extensionId == null) {
            if (plan.status() != SubscriptionStatus.PENDING_PAYMENT || plan.mode() != PlanMode.PLAN_PRODUCTOR_INDIVIDUAL) {
                return conflict("subscription.checkout.state");
            }
            return Result.success(new Target(subscriptionId, null, plan.price()));
        }
        var extension = extensions.findById(new QuotaExtensionId(extensionId.toString()))
                .filter(found -> found.snapshot().subscriptionId().equals(plan.id()));
        if (extension.isEmpty()) {
            return Result.failure(ApplicationError.notFound("quota_extension", extensionId.toString()));
        }
        var pending = extension.get().snapshot();
        if (pending.status() != QuotaExtensionStatus.PENDING_PAYMENT || pending.source() != QuotaExtensionSource.PAYMENT
                || !contract.get().hasActiveEntitlement(clock.instant())) {
            return conflict("subscription.checkout.state");
        }
        return Result.success(new Target(subscriptionId, extensionId, pending.price()));
    }
    private Optional<PaymentAttempt> latest(Target target) {
        return target.extensionId() == null ? attempts.latest(target.subscriptionId())
                : attempts.latestForExtension(target.extensionId());
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
            if (current.extensionId() != null) {
                return applyExtension(current, sub, payment);
            }
            if (sub.snapshot().mode() != PlanMode.PLAN_PRODUCTOR_INDIVIDUAL || !matches(current, payment, sub.snapshot().price())) {
                return invalid("subscription.payment.mismatch");
            }
            if ("approved".equals(payment.status())) {
                if (!approvedInTime(payment)) { return invalid("subscription.payment.mismatch"); }
                try {
                    sub.activateIndividual(payment.id(), payment.amount(), payment.currency(), payment.approvedAt(),
                            renewalStart(sub, payment.approvedAt()));
                }
                catch (IllegalArgumentException | IllegalStateException ex) { return conflict(ex.getMessage()); }
                subscriptions.save(sub);
                attempts.save(current.withPaymentState(payment.id(), "approved"));
                sub.domainEvents().forEach(events::publishEvent);
                sub.clearDomainEvents();
            } else if (PaymentAttempt.REVERSED_STATES.contains(payment.status())) {
                // A refunded or charged-back plan stops giving access
                if (sub.revokeReversedPayment(payment.id())) {
                    subscriptions.save(sub);
                }
                attempts.save(current.withPaymentState(payment.id(), payment.status()));
            } else if (Set.of("pending", "in_process", "authorized", "in_mediation", "rejected", "cancelled").contains(payment.status())) {
                attempts.save(current.withPaymentState(payment.id(), payment.status()));
            }
            return Result.success(true);
        });
    }
    /** Adds the paid hectares to the plan once; a repeated notification only refreshes the attempt. */
    private Result<Boolean, ApplicationError> applyExtension(PaymentAttempt current, Subscription sub,
            PaymentGateway.VerifiedPayment payment) {
        var extension = extensions.findById(new QuotaExtensionId(current.extensionId().toString())).orElseThrow();
        if (!matches(current, payment, extension.snapshot().price())) {
            return invalid("subscription.payment.mismatch");
        }
        if ("approved".equals(payment.status())) {
            if (!approvedInTime(payment)) { return invalid("subscription.payment.mismatch"); }
            try {
                if (extension.approvePayment(payment.id(), payment.amount(), payment.currency(), payment.approvedAt())) {
                    sub.extendQuota(extension.snapshot().addedArea());
                    subscriptions.save(sub);
                    extensions.save(extension);
                }
            } catch (IllegalArgumentException | IllegalStateException ex) { return conflict(ex.getMessage()); }
            attempts.save(current.withPaymentState(payment.id(), "approved"));
        } else if (PaymentAttempt.REVERSED_STATES.contains(payment.status())) {
            // The hectares go back only once: the attempt remembers the payment is no longer approved
            if ("approved".equals(current.paymentStates().get(payment.id()))) {
                sub.reduceQuota(extension.snapshot().addedArea());
                subscriptions.save(sub);
            }
            attempts.save(current.withPaymentState(payment.id(), payment.status()));
        } else if (Set.of("pending", "in_process", "authorized", "in_mediation", "rejected", "cancelled").contains(payment.status())) {
            attempts.save(current.withPaymentState(payment.id(), payment.status()));
        }
        return Result.success(true);
    }
    private static boolean matches(PaymentAttempt current, PaymentGateway.VerifiedPayment payment, SubscriptionPrice price) {
        return price != null && "PEN".equals(payment.currency()) && price.total().equals(payment.amount())
                && payment.initiatedAt() != null && !payment.initiatedAt().isBefore(current.createdAt().minusSeconds(5))
                && payment.initiatedAt().isBefore(current.expiresAt());
    }
    private boolean approvedInTime(PaymentGateway.VerifiedPayment payment) {
        return payment.approvedAt() != null && !payment.approvedAt().isBefore(payment.initiatedAt())
                && !payment.approvedAt().isAfter(clock.instant().plusSeconds(300));
    }
    /**
     * A renewal paid while the current plan is still in force starts when that plan ends, so no paid day is lost.
     * @param renewal the plan being activated
     * @param approvedAt verified approval time
     * @return the start of the paid year
     */
    private Instant renewalStart(Subscription renewal, Instant approvedAt) {
        return subscriptions.findCurrent(renewal.snapshot().producerId(), approvedAt)
                .filter(other -> !other.snapshot().id().equals(renewal.snapshot().id()))
                .filter(other -> other.hasActiveEntitlement(approvedAt))
                .map(other -> other.snapshot().period().endsAt())
                .orElse(approvedAt);
    }
    /**
     * Reads locally persisted attempt state without contacting the provider.
     * @param subscriptionId contract identifier already authorized by the caller
     * @return latest durable attempt when present
     */
    @Override
    public Optional<PaymentAttempt> latest(UUID subscriptionId) { return attempts.latest(subscriptionId); }
    private static <T> Result<T, ApplicationError> conflict(String key) {
        var error = ApplicationError.conflict("subscription", key);
        return Result.failure(new ApplicationError(error.code(), error.message(), error.details(), Map.of("reason", key)));
    }
    private static <T> Result<T, ApplicationError> invalid(String key) {
        return Result.failure(ApplicationError.businessRuleViolation("subscription", key));
    }
    private static <T> Result<T, ApplicationError> unavailable() {
        return Result.failure(new ApplicationError("PAYMENT_UNAVAILABLE", "subscription.gateway.unavailable", "subscription.gateway.unavailable"));
    }
}
