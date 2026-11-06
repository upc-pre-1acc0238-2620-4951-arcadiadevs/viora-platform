package com.arcadiadevs.viora.platform.subscription.application.queryservices;

import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetDataSharingConsentByIdAndProducerIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetDataSharingConsentsByProducerIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Application query service port of the data sharing consents, as their producer sees them.
 */
public interface DataSharingConsentQueryService {

    /**
     * Retrieves the consents of a producer, newest first.
     *
     * @param query the query containing the producer
     * @return the consents, empty when the producer never shared
     */
    List<DataSharingConsentView> handle(GetDataSharingConsentsByProducerIdQuery query);

    /**
     * Retrieves a consent only if it belongs to the producer.
     *
     * @param query the query containing the consent and the producer
     * @return the consent, or empty for both a missing consent and one of another producer
     */
    Optional<DataSharingConsentView> handle(GetDataSharingConsentByIdAndProducerIdQuery query);
}
