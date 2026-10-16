package com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.profiles.interfaces.acl.ProfileContextFacade;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Outbound ACL used by Settlement to name the holder of a sealed dossier from the producer profile. */
@Service("settlementExternalProfileService")
public class ExternalProfileService {

    private final ProfileContextFacade profileContextFacade;

    public ExternalProfileService(ProfileContextFacade profileContextFacade) {
        this.profileContextFacade = profileContextFacade;
    }

    /**
     * Full civil name of a producer.
     *
     * @param producerId the producer, who is the user account owning the plot
     * @return the full name, or empty when the producer has no profile yet
     */
    public Optional<String> findFullName(UserId producerId) {
        if (producerId == null) {
            return Optional.empty();
        }
        return profileContextFacade.findFullNameByUserId(producerId.userId())
                .map(String::trim)
                .filter(name -> !name.isEmpty());
    }
}
