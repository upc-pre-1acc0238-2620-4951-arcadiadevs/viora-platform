package com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.profiles.interfaces.acl.ProfileContextFacade;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Outbound ACL service used by the Subscription context to name the producers who redeemed a code and the managers
 * in charge of a license.
 */
@Service("subscriptionExternalProfileService")
public class ExternalProfileService {

    private final ProfileContextFacade profileContextFacade;

    /**
     * Creates the service.
     *
     * @param profileContextFacade the Profiles context facade
     * @throws IllegalArgumentException if the facade is null
     */
    public ExternalProfileService(ProfileContextFacade profileContextFacade) {
        if (profileContextFacade == null) {
            throw new IllegalArgumentException("subscription.profile_facade.null");
        }
        this.profileContextFacade = profileContextFacade;
    }

    /**
     * Finds the full name of a producer.
     *
     * @param producerId the producer
     * @return the name, or empty if the producer has no profile
     */
    public Optional<String> findFullName(ProducerId producerId) {
        if (producerId == null) {
            return Optional.empty();
        }
        return profileContextFacade.findFullNameByUserId(producerId.producerId());
    }

    /**
     * Finds the full name of a technical manager.
     *
     * @param managerId the manager
     * @return the name, or empty if the manager is null or has no profile
     */
    public Optional<String> findFullName(ManagerId managerId) {
        if (managerId == null) {
            return Optional.empty();
        }
        return profileContextFacade.findFullNameByUserId(managerId.managerId());
    }
}
