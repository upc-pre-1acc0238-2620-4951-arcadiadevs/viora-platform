package com.arcadiadevs.viora.platform.subscription.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.subscription.domain.model.events.CooperativeLicenseRenewedEvent;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.DataSharingConsentRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extends the sponsored plans that ended with a license when it is renewed, and the consents still active with them,
 * without new codes. A plan that ends sooner, such as a released member's, keeps its end, and a withdrawn consent
 * does not come back.
 */
@Component
public class ExtendSponsoredPlansOnLicenseRenewedHandler {

    private final SubscriptionRepository subscriptionRepository;
    private final DataSharingConsentRepository dataSharingConsentRepository;

    /**
     * Creates the handler.
     *
     * @param subscriptionRepository       the sponsored plans
     * @param dataSharingConsentRepository the consents to share data
     * @throws IllegalArgumentException if any dependency is null
     */
    public ExtendSponsoredPlansOnLicenseRenewedHandler(SubscriptionRepository subscriptionRepository,
                                                       DataSharingConsentRepository dataSharingConsentRepository) {
        if (subscriptionRepository == null || dataSharingConsentRepository == null) {
            throw new IllegalArgumentException("subscription.renewal_handler.dependency.null");
        }
        this.subscriptionRepository = subscriptionRepository;
        this.dataSharingConsentRepository = dataSharingConsentRepository;
    }

    /**
     * Extends the plans and consents of the renewed license.
     *
     * @param event the renewal
     */
    @EventListener
    @Transactional
    public void on(CooperativeLicenseRenewedEvent event) {
        if (event == null) {
            return;
        }
        var licenseId = new CooperativeLicenseId(event.licenseId());
        var at = event.occurredOn();
        for (var plan : subscriptionRepository.findSponsoredInForceByLicenseId(licenseId, at)) {
            if (plan.snapshot().period().endsAt().equals(event.previousEndsAt())) {
                plan.extendTo(event.newEndsAt());
                subscriptionRepository.save(plan);
            }
        }
        for (var consent : dataSharingConsentRepository.findAllByLicenseId(licenseId)) {
            if (consent.snapshot().endsAt().equals(event.previousEndsAt())
                    && consent.extendTo(event.newEndsAt(), at)) {
                dataSharingConsentRepository.save(consent);
            }
        }
    }
}
