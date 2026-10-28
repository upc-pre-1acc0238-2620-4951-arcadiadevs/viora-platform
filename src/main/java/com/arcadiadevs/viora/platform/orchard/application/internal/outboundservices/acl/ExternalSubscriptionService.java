package com.arcadiadevs.viora.platform.orchard.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import org.springframework.stereotype.Service;

import java.util.OptionalDouble;

/**
 * Outbound ACL service used by Orchard to learn how many hectares the producer's plan covers.
 */
@Service("orchardExternalSubscriptionService")
public class ExternalSubscriptionService {

    private final SubscriptionContextFacade subscriptionContextFacade;

    public ExternalSubscriptionService(SubscriptionContextFacade subscriptionContextFacade) {
        this.subscriptionContextFacade = subscriptionContextFacade;
    }

    /**
     * Resolves the hectares the producer's active plan covers.
     *
     * @param producerId the producer identifier string
     * @return the hectares, or empty when the producer has no plan giving access now
     */
    public OptionalDouble findEntitledQuotaHectares(String producerId) {
        return subscriptionContextFacade.findEntitledQuotaHectares(producerId)
                .map(hectares -> OptionalDouble.of(hectares.doubleValue()))
                .orElseGet(OptionalDouble::empty);
    }
}
