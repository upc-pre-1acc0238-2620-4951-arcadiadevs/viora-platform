package com.arcadiadevs.viora.platform.subscription.infrastructure.acl;

import org.springframework.stereotype.Component;
import com.arcadiadevs.viora.platform.profiles.interfaces.acl.ProfileContextFacade;
import com.arcadiadevs.viora.platform.iam.interfaces.acl.IamContextFacade;
import com.arcadiadevs.viora.platform.subscription.application.ports.ProducerProfilePort;

/** Translates public IAM/profile capabilities without importing their entities. */
@Component
public class ProfileReadinessAdapter implements ProducerProfilePort {
    private static final String PRODUCER_ROLE = "ROLE_PRODUCER";
    private final ProfileContextFacade profiles;
    private final IamContextFacade identity;

    /**
     * Creates the individual purchase readiness adapter.
     * @param profiles published profile capability
     * @param identity published IAM capability
     */
    public ProfileReadinessAdapter(ProfileContextFacade profiles, IamContextFacade identity) {
        this.profiles = profiles;
        this.identity = identity;
    }

    /**
     * Requires a known producer and a completed profile.
     * @param producerId authenticated identity
     * @return whether an individual purchase is allowed
     */
    @Override
    public boolean isReady(String producerId) {
        return identity.findUserRole(producerId).filter(PRODUCER_ROLE::equals).isPresent()
                && profiles.existsProfileByUserId(producerId);
    }
}
