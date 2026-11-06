package com.arcadiadevs.viora.platform.subscription.application.acl;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PlanMode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.DataSharingConsentRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.DataSharingConsent;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.DataSharingConsentStatus;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Implementation of the Subscription ACL facade, answering from the subscription in force. */
@Service
@Transactional(readOnly = true)
public class SubscriptionContextFacadeImpl implements SubscriptionContextFacade {

    private final SubscriptionRepository subscriptionRepository;
    private final CooperativeLicenseRepository cooperativeLicenseRepository;
    private final DataSharingConsentRepository dataSharingConsentRepository;
    private final InvitationCodeRepository invitationCodeRepository;
    private final Clock clock;

    /**
     * Creates the facade.
     *
     * @param subscriptionRepository       the plans
     * @param cooperativeLicenseRepository the cooperative licenses
     * @param dataSharingConsentRepository the consents to share data
     * @param invitationCodeRepository     the redeemed codes
     * @param clock                        the application clock
     */
    public SubscriptionContextFacadeImpl(SubscriptionRepository subscriptionRepository,
                                         CooperativeLicenseRepository cooperativeLicenseRepository,
                                         DataSharingConsentRepository dataSharingConsentRepository,
                                         InvitationCodeRepository invitationCodeRepository, Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.cooperativeLicenseRepository = cooperativeLicenseRepository;
        this.dataSharingConsentRepository = dataSharingConsentRepository;
        this.invitationCodeRepository = invitationCodeRepository;
        this.clock = clock;
    }

    @Override
    public Optional<BigDecimal> findEntitledQuotaHectares(String producerId) {
        final ProducerId id;
        try {
            id = new ProducerId(producerId);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        var now = clock.instant();
        return subscriptionRepository.findCurrent(id, now)
                .filter(subscription -> subscription.hasActiveEntitlement(now))
                .map(subscription -> subscription.snapshot().quota().hectares());
    }

    @Override
    public Optional<String> findLicenseIdInChargeOf(String managerId) {
        final ManagerId id;
        try {
            id = new ManagerId(managerId);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        var now = clock.instant();
        return cooperativeLicenseRepository.findByManagerId(id)
                .filter(license -> license.isInForce(now))
                .map(license -> license.snapshot().id().cooperativeLicenseId());
    }

    @Override
    public Optional<String> findSponsoringLicenseId(String producerId) {
        final ProducerId id;
        try {
            id = new ProducerId(producerId);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        var now = clock.instant();
        return subscriptionRepository.findCurrent(id, now)
                .filter(subscription -> subscription.hasActiveEntitlement(now))
                .map(subscription -> subscription.snapshot())
                .filter(snapshot -> snapshot.mode() == PlanMode.PLAN_COOPERATIVO_PATROCINADO
                        && snapshot.licenseId() != null)
                .map(snapshot -> snapshot.licenseId().cooperativeLicenseId());
    }

    @Override
    public List<String> findSponsoredProducerIds(String licenseId) {
        final CooperativeLicenseId id;
        try {
            id = new CooperativeLicenseId(licenseId);
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
        return subscriptionRepository.findSponsoredProducerIds(id, clock.instant()).stream()
                .map(ProducerId::producerId)
                .toList();
    }

    @Override
    public List<LicenseMember> findLicenseMembers(String licenseId) {
        final CooperativeLicenseId id;
        try {
            id = new CooperativeLicenseId(licenseId);
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
        var now = clock.instant();
        return subscriptionRepository.findSponsoredProducerIds(id, now).stream()
                .flatMap(producerId -> subscriptionRepository.findCurrent(producerId, now)
                        .filter(subscription -> id.equals(subscription.snapshot().licenseId()))
                        .map(subscription -> toMember(subscription, now))
                        .stream())
                .toList();
    }

    private LicenseMember toMember(Subscription subscription, Instant now) {
        var snapshot = subscription.snapshot();
        // The consent given with this very plan; an older plan's consent says nothing about this one.
        var consent = dataSharingConsentRepository.findAllByProducerId(snapshot.producerId()).stream()
                .filter(each -> each.snapshot().subscriptionId().equals(snapshot.id()))
                .max(Comparator.comparing(each -> each.snapshot().acceptedAt()));
        var status = consent.map(each -> each.statusAt(now)).orElse(null);
        var sharing = status == DataSharingConsentStatus.ACTIVE ? "SHARING"
                : status == DataSharingConsentStatus.WITHDRAWN ? "WITHDRAWN" : "NOT_SHARING";
        var maskedCode = snapshot.redeemedCodeId() == null ? null
                : invitationCodeRepository.findById(snapshot.redeemedCodeId())
                .map(code -> "COOP-••" + (code.snapshot().hint() == null ? "••" : code.snapshot().hint()))
                .orElse(null);
        return new LicenseMember(snapshot.producerId().producerId(), snapshot.period().startsAt(),
                snapshot.quota().hectares(), maskedCode, sharing,
                "WITHDRAWN".equals(sharing) ? consent.map(DataSharingConsent::snapshot).orElseThrow().withdrawnAt()
                        : null);
    }
}
