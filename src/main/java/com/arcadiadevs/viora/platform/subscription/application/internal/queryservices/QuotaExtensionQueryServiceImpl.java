package com.arcadiadevs.viora.platform.subscription.application.internal.queryservices;

import com.arcadiadevs.viora.platform.subscription.application.queryservices.QuotaExtensionQueryService;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionCatalog;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtension;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PlanMode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.QuotaExtensionId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.PaymentAttemptRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.QuotaExtensionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.services.PlanExtensionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves extension terms and owned extensions without provider calls.
 */
@Service
@Transactional(readOnly = true)
public class QuotaExtensionQueryServiceImpl implements QuotaExtensionQueryService {

    private final SubscriptionRepository subscriptions;
    private final QuotaExtensionRepository extensions;
    private final PaymentAttemptRepository attempts;
    private final SubscriptionCatalog catalog;
    private final Clock clock;

    /**
     * Creates the query service.
     *
     * @param subscriptions the plans
     * @param extensions    the quota extensions
     * @param attempts      the stored payment attempts
     * @param catalog       the server-owned tariff
     * @param clock         the application clock
     */
    public QuotaExtensionQueryServiceImpl(SubscriptionRepository subscriptions, QuotaExtensionRepository extensions,
                                          PaymentAttemptRepository attempts, SubscriptionCatalog catalog,
                                          Clock clock) {
        this.subscriptions = subscriptions;
        this.extensions = extensions;
        this.attempts = attempts;
        this.catalog = catalog;
        this.clock = clock;
    }

    @Override
    public Optional<ExtensionTerms> terms(String owner, UUID subscriptionId) {
        return subscriptions.findByIdAndProducerId(new SubscriptionId(subscriptionId.toString()), new ProducerId(owner))
                .map(this::terms);
    }

    private ExtensionTerms terms(Subscription plan) {
        var now = clock.instant();
        var s = plan.snapshot();
        var id = UUID.fromString(s.id().subscriptionId());
        var endsAt = s.period() == null ? null : s.period().endsAt();
        if (!plan.hasActiveEntitlement(now)) {
            return new ExtensionTerms(id, s.mode().name(), false, "subscription.extension.not_active", "NONE",
                    s.quota().hectares(), endsAt, null, null, null, null, null, null, null);
        }
        if (s.mode() == PlanMode.PLAN_COOPERATIVO_PATROCINADO) {
            return new ExtensionTerms(id, s.mode().name(), true, null, "CODE", s.quota().hectares(), endsAt,
                    null, null, null, null, null, null, null);
        }
        if (PlanExtensionPolicy.isInRenewalWindow(s.period(), now)) {
            return new ExtensionTerms(id, s.mode().name(), false, "subscription.extension.renewal_window", "RENEW",
                    s.quota().hectares(), endsAt, null, null, null, null, null, null, null);
        }
        var offer = catalog.plans().stream().filter(candidate -> candidate.code().equals(s.planCode())).findFirst();
        var days = PlanExtensionPolicy.billableDays(s.period(), now);
        return new ExtensionTerms(id, s.mode().name(), offer.isPresent(), offer.isPresent() ? null : "subscription.plan.invalid",
                offer.isPresent() ? "PAY" : "NONE", s.quota().hectares(), endsAt,
                PlanExtensionPolicy.billableMonths(s.period(), now),
                offer.map(SubscriptionCatalog.PlanOffer::monthlyRatePerHa).orElse(null),
                offer.map(SubscriptionCatalog.PlanOffer::currency).orElse(null),
                offer.map(SubscriptionCatalog.PlanOffer::minimumQuotaHa).orElse(null),
                offer.map(SubscriptionCatalog.PlanOffer::quotaIncrementHa).orElse(null),
                offer.isPresent() ? days : null,
                offer.isPresent() ? catalog.extensionRatePerHa(s.planCode(), days, PlanExtensionPolicy.periodDays(s.period()))
                        : null);
    }

    @Override
    public Optional<QuotaExtension> find(String owner, UUID subscriptionId, UUID extensionId) {
        var producer = new ProducerId(owner);
        var plan = new SubscriptionId(subscriptionId.toString());
        return extensions.findById(new QuotaExtensionId(extensionId.toString()))
                .filter(found -> found.snapshot().producerId().equals(producer)
                        && found.snapshot().subscriptionId().equals(plan));
    }

    @Override
    public String paymentStatus(UUID extensionId) {
        return attempts.latestForExtension(extensionId).map(attempt -> attempt.status()).orElse(null);
    }
}
