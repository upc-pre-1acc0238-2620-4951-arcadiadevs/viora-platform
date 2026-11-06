package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Anti-corruption layer reading from Subscription which license a manager is in charge of and which members it
 * sponsors.
 */
@Service("territoryExternalSubscriptionService")
public class ExternalSubscriptionService {

    private final SubscriptionContextFacade subscriptionContextFacade;

    /**
     * Creates the service.
     *
     * @param subscriptionContextFacade the Subscription facade
     */
    public ExternalSubscriptionService(SubscriptionContextFacade subscriptionContextFacade) {
        this.subscriptionContextFacade = subscriptionContextFacade;
    }

    /**
     * Finds the license a manager is in charge of while it is in force.
     *
     * @param managerId the manager
     * @return the license, or empty
     */
    public Optional<LicenseId> findLicenseInChargeOf(ManagerId managerId) {
        return subscriptionContextFacade.findLicenseIdInChargeOf(managerId.managerId()).map(LicenseId::new);
    }

    /**
     * Finds the license that sponsors a member's plan while it gives access.
     *
     * @param producerId the member
     * @return the license, or empty if the producer is not sponsored now
     */
    public Optional<LicenseId> findSponsoringLicense(ProducerId producerId) {
        return subscriptionContextFacade.findSponsoringLicenseId(producerId.producerId()).map(LicenseId::new);
    }

    /**
     * Lists the members a license sponsors now.
     *
     * @param licenseId the license
     * @return the members
     */
    public Set<ProducerId> findSponsoredProducers(LicenseId licenseId) {
        return subscriptionContextFacade.findSponsoredProducerIds(licenseId.licenseId()).stream()
                .map(ProducerId::new)
                .collect(Collectors.toSet());
    }
}
