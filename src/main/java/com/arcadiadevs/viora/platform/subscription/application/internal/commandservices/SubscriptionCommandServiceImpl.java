package com.arcadiadevs.viora.platform.subscription.application.internal.commandservices;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.arcadiadevs.viora.platform.shared.application.result.*;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.arcadiadevs.viora.platform.subscription.application.ports.*;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionCatalog;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.CreateSubscriptionCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.services.PlanExtensionPolicy;

/**
 * Coordinates idempotent individual purchases without replacing sponsored access. A producer whose plan is in its
 * renewal window can buy the next year, which starts when the current plan ends. A purchase still waiting for its
 * payment takes the new selection instead of blocking it, unless a payment for it is approved or unresolved.
 */
@Service
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {
    private static final String OPERATION = "CREATE_SUBSCRIPTION";
    private final SubscriptionRepository subscriptions;
    private final SubscriptionUnitOfWork work;
    private final SubscriptionCatalog catalog;
    private final ProducerProfilePort profiles;
    private final Clock clock;
    private final CheckoutService checkouts;

    /** What the first serialized step decided: a finished result, or a pending purchase to requote. */
    private record Step(Result<CreatedSubscription, ApplicationError> done, Subscription pending) { }

    /**
     * Creates the individual purchase use case.
     * @param subscriptions shared persistence port
     * @param work transactional owner serialization
     * @param catalog server-owned tariff
     * @param profiles producer authorization and profile readiness
     * @param clock application clock
     * @param checkouts closes the unpaid checkout of a purchase before its terms change
     */
    public SubscriptionCommandServiceImpl(SubscriptionRepository subscriptions, SubscriptionUnitOfWork work,
            SubscriptionCatalog catalog, ProducerProfilePort profiles, Clock clock, CheckoutService checkouts) {
        this.subscriptions = subscriptions;
        this.work = work;
        this.catalog = catalog;
        this.profiles = profiles;
        this.clock = clock;
        this.checkouts = checkouts;
    }

    /**
     * Registers a pending purchase or replays its durable operation.
     * @param command authenticated purchase request
     * @return created identifier or a consistent application error
     */
    @Override
    public Result<CreatedSubscription, ApplicationError> handle(CreateSubscriptionCommand command) {
        if (command == null || command.operationId().isBlank() || command.operationId().length() > 128) {
            return Result.failure(ApplicationError.validationError("operationId", "subscription.operation.invalid"));
        }
        final ProducerId owner;
        final IndividualHectaresQuota quota;
        final SubscriptionPrice price;
        try {
            owner = new ProducerId(command.producerId());
            quota = new IndividualHectaresQuota(command.requestedQuotaHa());
            price = catalog.quote(command.planCode(), quota);
            new HectaresQuota(quota.value());
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.businessRuleViolation("subscription", exception.getMessage()));
        }
        if (!profiles.isReady(owner.producerId())) {
            return Result.failure(ApplicationError.forbidden("subscription", "subscription.profile.required"));
        }
        var fingerprint = command.planCode() + ":" + quota.value().toPlainString();
        Step step = work.exclusive(owner.producerId(), () -> {
            var replayed = replay(owner, command, fingerprint);
            if (replayed != null) {
                return new Step(replayed, null);
            }
            if (holdsPlanBeyondRenewal(owner)) {
                return new Step(Result.failure(ApplicationError.conflict("subscription", "subscription.current.exists")), null);
            }
            var pending = subscriptions.findPendingByProducerId(owner);
            if (pending.isEmpty()) {
                var created = subscriptions.save(Subscription.createIndividual(owner, command.planCode(), quota,
                        price, clock.instant()));
                var id = UUID.fromString(created.snapshot().id().subscriptionId());
                work.recordOperation(owner.producerId(), command.operationId(), OPERATION, fingerprint, id);
                return new Step(Result.success(new CreatedSubscription(id, false)), null);
            }
            var terms = pending.get().snapshot();
            if (command.planCode().equals(terms.planCode()) && quota.value().compareTo(terms.quota().hectares()) == 0) {
                // The same selection again is the same purchase, with the terms it was quoted with.
                var id = UUID.fromString(terms.id().subscriptionId());
                work.recordOperation(owner.producerId(), command.operationId(), OPERATION, fingerprint, id);
                return new Step(Result.success(new CreatedSubscription(id, true)), null);
            }
            return new Step(null, pending.get());
        });
        if (step.done() != null) {
            return step.done();
        }
        var pendingId = UUID.fromString(step.pending().snapshot().id().subscriptionId());
        // The provider is contacted outside the serialized work: no link may still pay the old terms.
        var released = checkouts.release(owner.producerId(), pendingId);
        if (released.isFailure()) {
            return Result.failure(released.failure().orElseThrow());
        }
        return work.exclusive(owner.producerId(), () -> {
            var replayed = replay(owner, command, fingerprint);
            if (replayed != null) {
                return replayed;
            }
            var pending = subscriptions.findPendingByProducerId(owner)
                    .filter(found -> found.snapshot().id().equals(step.pending().snapshot().id()));
            var reopened = checkouts.latest(pendingId).filter(attempt -> !"EXPIRED".equals(attempt.status()));
            if (pending.isEmpty() || reopened.isPresent()) {
                return Result.failure(ApplicationError.conflict("subscription", "subscription.current.exists"));
            }
            var requoted = pending.get();
            requoted.requote(command.planCode(), quota, price);
            subscriptions.save(requoted);
            work.recordOperation(owner.producerId(), command.operationId(), OPERATION, fingerprint, pendingId);
            return Result.success(new CreatedSubscription(pendingId, false));
        });
    }

    /** @return the result of an operation already handled with this key, or null for a new one */
    private Result<CreatedSubscription, ApplicationError> replay(ProducerId owner, CreateSubscriptionCommand command,
            String fingerprint) {
        var previous = work.findOperation(owner.producerId(), command.operationId());
        if (previous.isEmpty()) {
            return null;
        }
        var operation = previous.get();
        if (!OPERATION.equals(operation.kind()) || !fingerprint.equals(operation.fingerprint())) {
            return Result.failure(ApplicationError.conflict("subscription", "subscription.operation.conflict"));
        }
        return Result.success(new CreatedSubscription(operation.resourceId(), true));
    }

    /**
     * Tells whether the producer's access blocks a new purchase: a plan in force outside its renewal window, or a
     * renewal already bought for after it.
     */
    private boolean holdsPlanBeyondRenewal(ProducerId owner) {
        var now = clock.instant();
        return subscriptions.findCurrent(owner, now)
                .filter(plan -> plan.hasActiveEntitlement(now))
                .map(plan -> !PlanExtensionPolicy.isInRenewalWindow(plan.snapshot().period(), now)
                        || subscriptions.existsActiveByProducerId(owner, plan.snapshot().period().endsAt()))
                .orElse(false);
    }
}
