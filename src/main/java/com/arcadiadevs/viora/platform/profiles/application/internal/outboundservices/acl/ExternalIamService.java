package com.arcadiadevs.viora.platform.profiles.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.iam.interfaces.acl.IamContextFacade;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Service;

/**
 * Outbound ACL service used by User Profiles context to verify account existence in IAM.
 */
@Service("profilesExternalIamService")
public class ExternalIamService {

    private final IamContextFacade iamContextFacade;

    public ExternalIamService(IamContextFacade iamContextFacade) {
        if (iamContextFacade == null) {
            throw new IllegalArgumentException("profile.iam_facade.null");
        }
        this.iamContextFacade = iamContextFacade;
    }

    /**
     * Checks if the referenced user account exists in active status within IAM.
     *
     * @param userId local Profiles UserId value object
     * @return true if user exists in IAM; false otherwise
     */
    public boolean existsActiveUser(UserId userId) {
        if (userId == null) {
            return false;
        }
        return iamContextFacade.existsActiveUser(userId.userId());
    }
}
