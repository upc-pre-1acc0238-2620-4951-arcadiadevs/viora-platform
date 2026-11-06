package com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.iam.interfaces.acl.IamContextFacade;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import org.springframework.stereotype.Service;

/**
 * Outbound ACL service used by the Subscription context to read the role of a user in IAM.
 */
@Service("subscriptionExternalIamService")
public class ExternalIamService {

    private static final String PRODUCER_ROLE = "ROLE_PRODUCER";
    private static final String TECHNICAL_MANAGER_ROLE = "ROLE_TECHNICAL_MANAGER";

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
        return isProducer(producerId);
    }

    /**
     * Checks whether IAM knows a user as a producer, such as before an individual purchase.
     *
     * @param producerId the user
     * @return true if IAM knows the user as a producer; false for an unknown user or another role
     */
    public boolean isProducer(ProducerId producerId) {
        if (producerId == null) {
            return false;
        }
        return iamContextFacade.findUserRole(producerId.producerId())
                .map(PRODUCER_ROLE::equals)
                .orElse(false);
    }

    /**
     * Checks whether a user may take charge of a cooperative license: only technical managers IAM knows do.
     *
     * @param managerId the user asking to redeem a manager code
     * @return true if IAM knows the user as a technical manager; false otherwise
     */
    public boolean isTechnicalManager(ManagerId managerId) {
        if (managerId == null) {
            return false;
        }
        return iamContextFacade.findUserRole(managerId.managerId())
                .map(TECHNICAL_MANAGER_ROLE::equals)
                .orElse(false);
    }
}
