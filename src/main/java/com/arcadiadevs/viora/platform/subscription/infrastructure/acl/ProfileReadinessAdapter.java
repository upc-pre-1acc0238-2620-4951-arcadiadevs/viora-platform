package com.arcadiadevs.viora.platform.subscription.infrastructure.acl;

import org.springframework.stereotype.Component;
import com.arcadiadevs.viora.platform.profiles.interfaces.acl.ProfileContextFacade;
import com.arcadiadevs.viora.platform.subscription.application.ports.ProducerProfilePort;
/** Translates the published profile capability without importing profile entities. */
@Component
public class ProfileReadinessAdapter implements ProducerProfilePort {
    private final ProfileContextFacade profiles;
    public ProfileReadinessAdapter(ProfileContextFacade profiles) { this.profiles = profiles; }
    @Override public boolean isReady(String producerId) { return profiles.existsProfileByUserId(producerId); }
}
