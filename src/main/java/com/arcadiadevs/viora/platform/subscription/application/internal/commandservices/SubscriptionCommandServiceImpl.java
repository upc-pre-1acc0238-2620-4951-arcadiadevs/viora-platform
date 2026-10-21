package com.arcadiadevs.viora.platform.subscription.application.internal.commandservices;

import java.time.Clock;
import org.springframework.stereotype.Service;
import com.arcadiadevs.viora.platform.shared.application.result.*;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.arcadiadevs.viora.platform.subscription.application.ports.*;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionCatalog;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.CreateSubscriptionCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
/** Serializes initial purchases per owner and replays successful operations without duplication. */
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {
    private final SubscriptionRepository subscriptions;
    private final SubscriptionUnitOfWork unitOfWork;
    private final SubscriptionCatalog catalog;
    private final ProducerProfilePort profiles;
    private final Clock clock;
    /** @param subscriptions persistence port @param unitOfWork transaction port @param catalog tariff @param profiles readiness @param clock time */
    public SubscriptionCommandServiceImpl(SubscriptionRepository subscriptions, SubscriptionUnitOfWork unitOfWork,
            SubscriptionCatalog catalog, ProducerProfilePort profiles, Clock clock) {
        this.subscriptions = subscriptions;
        this.unitOfWork = unitOfWork;
        this.catalog = catalog;
        this.profiles = profiles;
        this.clock = clock;
    }
    /** @param command initial request @return created identifier or an application error */
    @Override public Result<CreatedSubscription, ApplicationError> handle(CreateSubscriptionCommand command) {
        if (command == null || command.operationId().isBlank() || command.operationId().length() > 128) {
            return Result.failure(ApplicationError.validationError("operationId", "subscription.operation.invalid"));
        }
        final HectaresQuota quota;
        try {
            quota = new HectaresQuota(command.requestedQuotaHa());
            catalog.quote(command.planCode(), quota);
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.businessRuleViolation("subscription", ex.getMessage()));
        }
        var fingerprint = command.planCode() + ":" + quota.value().toPlainString();
        return unitOfWork.exclusive(command.producerId(), () -> {
            var previous = unitOfWork.findOperation(command.producerId(), command.operationId());
            if (previous.isPresent()) {
                var operation = previous.get();
                if (!operation.kind().equals("CREATE_SUBSCRIPTION") || !operation.fingerprint().equals(fingerprint)) {
                    return Result.failure(ApplicationError.conflict("subscription", "subscription.operation.conflict"));
                }
                return Result.success(new CreatedSubscription(operation.resourceId(), true));
            }
            if (!profiles.isReady(command.producerId())) {
                return Result.failure(new ApplicationError("SUBSCRIPTION_FORBIDDEN", "subscription.profile.required"));
            }
            var current = subscriptions.findCurrent(command.producerId());
            if (current.isPresent() && (current.get().snapshot().status().equals("PENDING_PAYMENT")
                    || current.get().hasActiveEntitlement(clock.instant()))) {
                return Result.failure(ApplicationError.conflict("subscription", "subscription.current.exists"));
            }
            var created = subscriptions.save(Subscription.create(command.producerId(), command.planCode(), quota,
                    catalog.quote(command.planCode(), quota), clock.instant()));
            var id = created.snapshot().id();
            unitOfWork.recordOperation(command.producerId(), command.operationId(), "CREATE_SUBSCRIPTION", fingerprint, id);
            return Result.success(new CreatedSubscription(id, false));
        });
    }
}
