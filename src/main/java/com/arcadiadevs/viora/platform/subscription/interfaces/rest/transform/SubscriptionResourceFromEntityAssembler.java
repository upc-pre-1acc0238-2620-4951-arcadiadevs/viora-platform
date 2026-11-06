package com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.subscription.application.queryservices.CooperativeLicenseQueryService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeLicenseByIdQuery;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.SubscriptionResource;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Maps a subscription to its public response, naming the cooperative that sponsors it, if any.
 *
 * <p>The cooperative is read through the license query service, so the controllers only deal with application
 * services.</p>
 */
@Component
public class SubscriptionResourceFromEntityAssembler {

    private final CooperativeLicenseQueryService cooperativeLicenseQueryService;
    private final Clock clock;

    /**
     * Constructs the assembler.
     *
     * @param cooperativeLicenseQueryService the license reads
     * @param clock                          the application clock
     * @throws IllegalArgumentException if a dependency is null
     */
    public SubscriptionResourceFromEntityAssembler(CooperativeLicenseQueryService cooperativeLicenseQueryService,
                                                   Clock clock) {
        if (cooperativeLicenseQueryService == null || clock == null) {
            throw new IllegalArgumentException("subscription.resource_assembler.dependency.null");
        }
        this.cooperativeLicenseQueryService = cooperativeLicenseQueryService;
        this.clock = clock;
    }

    /**
     * Maps a subscription to its public response.
     *
     * @param subscription the subscription
     * @return the public resource, with the sponsoring cooperative for a sponsored plan
     */
    public SubscriptionResource toResource(Subscription subscription) {
        var licenseId = subscription.snapshot().licenseId();
        var cooperativeName = licenseId == null ? null
                : cooperativeLicenseQueryService.handle(new GetCooperativeLicenseByIdQuery(licenseId))
                        .map(license -> license.snapshot().cooperativeName())
                        .orElse(null);
        return SubscriptionResourceAssembler.toResource(subscription, cooperativeName, clock.instant());
    }
}
