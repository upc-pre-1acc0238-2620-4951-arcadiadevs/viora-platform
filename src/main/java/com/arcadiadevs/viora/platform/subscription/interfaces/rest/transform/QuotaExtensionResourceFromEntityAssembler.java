package com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionQueryService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtension;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.QuotaExtensionResource;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps a quota extension to its public response, with the quota its plan holds after it.
 *
 * <p>The plan is read through the subscription query service, so the controllers only deal with application
 * services.</p>
 */
@Component
public class QuotaExtensionResourceFromEntityAssembler {

    private final SubscriptionQueryService subscriptionQueryService;

    /**
     * Constructs the assembler.
     *
     * @param subscriptionQueryService the plan reads
     * @throws IllegalArgumentException if the query service is null
     */
    public QuotaExtensionResourceFromEntityAssembler(SubscriptionQueryService subscriptionQueryService) {
        if (subscriptionQueryService == null) {
            throw new IllegalArgumentException("subscription.query_service.null");
        }
        this.subscriptionQueryService = subscriptionQueryService;
    }

    /**
     * Maps an extension to its public response.
     *
     * @param extension     the extension; its plan belongs to the same producer
     * @param paymentStatus the latest payment state, or null when there is none to show
     * @return the public resource of the extension
     */
    public QuotaExtensionResource toResource(QuotaExtension extension, String paymentStatus) {
        var snapshot = extension.snapshot();
        var plan = subscriptionQueryService.find(snapshot.producerId().producerId(),
                UUID.fromString(snapshot.subscriptionId().subscriptionId())).orElseThrow();
        return QuotaExtensionResourceAssembler.toResource(extension, plan, paymentStatus);
    }
}
