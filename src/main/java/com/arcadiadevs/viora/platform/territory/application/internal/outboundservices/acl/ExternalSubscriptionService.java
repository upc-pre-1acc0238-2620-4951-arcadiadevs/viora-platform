package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.DataSharingState;
import org.springframework.stereotype.Service;

import java.util.List;
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

    /**
     * Lists the members a license sponsors now, with whether they share their data.
     *
     * @param licenseId the license
     * @return the members
     */
    public List<SponsoredMember> findMembers(LicenseId licenseId) {
        return subscriptionContextFacade.findLicenseMembers(licenseId.licenseId()).stream()
                .map(member -> new SponsoredMember(new ProducerId(member.producerId()), member.memberSince(),
                        member.quotaHectares(), member.maskedCode(), DataSharingState.valueOf(member.sharing()),
                        member.withdrawnAt()))
                .toList();
    }

    /**
     * Finds the name of the organization that holds a license.
     *
     * @param licenseId the license
     * @return the name, or empty
     */
    public Optional<String> findOrganizationName(LicenseId licenseId) {
        return subscriptionContextFacade.findLicenseOrganizationName(licenseId.licenseId());
    }

    /**
     * Lists the licenses in force that have a manager in charge, whose sectors deserve alerts.
     *
     * @return the licenses
     */
    public List<LicenseId> findManagedLicensesInForce() {
        return subscriptionContextFacade.findManagedLicenseIdsInForce().stream().map(LicenseId::new).toList();
    }
}
