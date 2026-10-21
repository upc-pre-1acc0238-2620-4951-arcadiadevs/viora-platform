package com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.iam.interfaces.acl.IamContextFacade;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import org.springframework.stereotype.Service;

/**
 * Outbound ACL service used by the Subscription context to read the role of a user in IAM.
 */
@Service("subscriptionExternalIamService")
public class ExternalIamService {

    private static final String PRODUCER_ROLE = "ROLE_PRODUCER";

    private final IamContextFacade iamContextFacade;

    /**
     * Creates the service.
     *
     * @param iamContextFacade the IAM context facade
     * @throws IllegalArgumentException if the facade is null
     */
    public ExternalIamService(IamContextFacade iamContextFacade) {
        if (iamContextFacade == null) {
            throw new IllegalArgumentException("subscription.iam_facade.null");
        }
        this.iamContextFacade = iamContextFacade;
    }

    /**
     * Checks whether a user may redeem cooperative codes: only producers IAM knows do.
     *
     * @param producerId the user asking to redeem a code
     * @return true if IAM knows the user as a producer; false otherwise
     */
    public boolean mayRedeemCooperativeCodes(ProducerId producerId) {
        if (producerId == null) {
            return false;
        }
        return iamContextFacade.findUserRole(producerId.producerId())
                .map(PRODUCER_ROLE::equals)
                .orElse(false);
    }
}
