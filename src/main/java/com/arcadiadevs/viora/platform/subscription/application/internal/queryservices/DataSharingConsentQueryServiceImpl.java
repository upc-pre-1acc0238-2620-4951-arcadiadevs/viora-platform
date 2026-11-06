package com.arcadiadevs.viora.platform.subscription.application.internal.queryservices;

import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalProfileService;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.DataSharingConsentQueryService;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.DataSharingConsentView;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.DataSharingConsent;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetDataSharingConsentByIdAndProducerIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetDataSharingConsentsByProducerIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.DataSharingConsentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Reads the data sharing consents of a producer, naming the organization and the manager who reads the data now.
 */
@Service
@Transactional(readOnly = true)
public class DataSharingConsentQueryServiceImpl implements DataSharingConsentQueryService {

    private final DataSharingConsentRepository dataSharingConsentRepository;
    private final CooperativeLicenseRepository cooperativeLicenseRepository;
    private final ExternalProfileService externalProfileService;
    private final Clock clock;

    /**
     * Creates the query service.
     *
     * @param dataSharingConsentRepository the data sharing consents
     * @param cooperativeLicenseRepository the cooperative licenses
     * @param externalProfileService       the names of the managers
     * @param clock                        the application clock
     * @throws IllegalArgumentException if any dependency is null
     */
    public DataSharingConsentQueryServiceImpl(DataSharingConsentRepository dataSharingConsentRepository,
                                              CooperativeLicenseRepository cooperativeLicenseRepository,
                                              ExternalProfileService externalProfileService, Clock clock) {
        if (dataSharingConsentRepository == null || cooperativeLicenseRepository == null || externalProfileService == null || clock == null) {
            throw new IllegalArgumentException("subscription.consent_queries.dependency.null");
        }
        this.dataSharingConsentRepository = dataSharingConsentRepository;
        this.cooperativeLicenseRepository = cooperativeLicenseRepository;
        this.externalProfileService = externalProfileService;
        this.clock = clock;
    }

    @Override
    public List<DataSharingConsentView> handle(GetDataSharingConsentsByProducerIdQuery query) {
        var now = clock.instant();
        return dataSharingConsentRepository.findAllByProducerId(query.producerId()).stream()
                .map(consent -> toView(consent, now))
                .toList();
    }

    @Override
    public Optional<DataSharingConsentView> handle(GetDataSharingConsentByIdAndProducerIdQuery query) {
        var now = clock.instant();
        return dataSharingConsentRepository.findAllByProducerId(query.producerId()).stream()
                .filter(consent -> consent.snapshot().id().equals(query.consentId()))
                .findFirst()
                .map(consent -> toView(consent, now));
    }

    private DataSharingConsentView toView(DataSharingConsent consent, Instant now) {
        var snapshot = consent.snapshot();
        var license = cooperativeLicenseRepository.findById(snapshot.licenseId());
        var organization = license.map(found -> found.snapshot().cooperativeName()).orElse(null);
        var manager = license.flatMap(found -> externalProfileService.findFullName(found.snapshot().managerId()))
                .orElse(null);
        return new DataSharingConsentView(snapshot.id().dataSharingConsentId(), organization, manager,
                consent.statusAt(now).name(), snapshot.acceptedAt(), snapshot.withdrawnAt(), snapshot.endsAt(),
                snapshot.textVersion().value());
    }
}
