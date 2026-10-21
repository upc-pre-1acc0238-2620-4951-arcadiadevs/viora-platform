package com.arcadiadevs.viora.platform.subscription.application.queryservices;

import java.util.Optional;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;

/** Application read port for the authenticated producer's current subscription. */
public interface SubscriptionQueryService {
    /**
     * Resolves effective access first, otherwise the latest registered owned contract.
     * @param owner authenticated producer identifier
     * @return current owned contract, or empty when no contract exists
     * @throws IllegalArgumentException if the producer identifier is invalid
     */
    Optional<Subscription> current(String owner);
}
