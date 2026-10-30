package com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import org.springframework.stereotype.Service;

/**
 * Outbound ACL service used by the assistant to learn whether the producer's plan gives access now.
 */
@Service("assistantExternalSubscriptionService")
public class ExternalSubscriptionService {

    private final SubscriptionContextFacade subscriptionContextFacade;

    public ExternalSubscriptionService(SubscriptionContextFacade subscriptionContextFacade) {
        this.subscriptionContextFacade = subscriptionContextFacade;
    }

    /**
     * Checks whether the producer has a plan giving access now.
     *
     * @param producerId the producer identifier string
     * @return true when the plan is active
     */
    public boolean hasActivePlan(String producerId) {
        return subscriptionContextFacade.findEntitledQuotaHectares(producerId).isPresent();
    }
}
