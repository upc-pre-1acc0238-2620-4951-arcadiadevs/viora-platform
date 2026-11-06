package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.iam.interfaces.acl.IamContextFacade;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Anti-corruption layer reading from IAM the role of the signed-in user.
 */
@Service("territoryExternalIamService")
public class ExternalIamService {

    private final IamContextFacade iamContextFacade;

    /**
     * Creates the service.
     *
     * @param iamContextFacade the IAM facade
     */
    public ExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    /**
     * Finds the role of a user.
     *
     * @param userId the user
     * @return the role name, such as {@code ROLE_PRODUCER}, or empty for an unknown user
     */
    public Optional<String> findRole(UserId userId) {
        return iamContextFacade.findUserRole(userId.userId());
    }
}
