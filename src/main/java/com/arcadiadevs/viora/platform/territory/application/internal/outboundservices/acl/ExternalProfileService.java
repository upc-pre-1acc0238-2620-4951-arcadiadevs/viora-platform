package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.profiles.interfaces.acl.ProfileContextFacade;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Anti-corruption layer reading from Profiles the names of the members.
 */
@Service("territoryExternalProfileService")
public class ExternalProfileService {

    private final ProfileContextFacade profileContextFacade;

    /**
     * Creates the service.
     *
     * @param profileContextFacade the Profiles facade
     */
    public ExternalProfileService(ProfileContextFacade profileContextFacade) {
        this.profileContextFacade = profileContextFacade;
    }

    /**
     * Finds the full name of a member.
     *
     * @param producerId the member
     * @return the name, or empty if they have no profile
     */
    public Optional<String> findFullName(ProducerId producerId) {
        return profileContextFacade.findFullNameByUserId(producerId.producerId());
    }

    /**
     * Finds the phone of a member, to call them or write them on WhatsApp.
     *
     * @param producerId the member
     * @return the phone in E.164, or empty
     */
    public Optional<String> findPhone(ProducerId producerId) {
        return profileContextFacade.findPhoneNumberByUserId(producerId.producerId());
    }
}
