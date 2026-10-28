package com.arcadiadevs.viora.platform.subscription.interfaces.acl;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Open Host Service (OHS) facade exposed by Subscription to other Bounded Contexts.
 *
 * <p>Exclusively returns Java standard and primitive types.</p>
 */
public interface SubscriptionContextFacade {

    /**
     * Resolves the hectares a producer may manage now: those of the plan in force, only while it gives access.
     *
     * @param producerId producer identifier string
     * @return the hectares of the active plan, or empty when the producer has no plan giving access now
     */
    Optional<BigDecimal> findEntitledQuotaHectares(String producerId);
}
