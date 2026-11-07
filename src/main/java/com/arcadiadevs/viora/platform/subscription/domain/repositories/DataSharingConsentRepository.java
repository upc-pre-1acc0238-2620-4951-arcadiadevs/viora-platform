package com.arcadiadevs.viora.platform.subscription.domain.repositories;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.DataSharingConsent;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.DataSharingConsentId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port of the data sharing consents.
 */
public interface DataSharingConsentRepository {

    /**
     * Finds a consent and locks it until the transaction ends, such as to withdraw it.
     *
     * @param id the consent identity
     * @return the consent, or empty if it does not exist
     */
    Optional<DataSharingConsent> findByIdForUpdate(DataSharingConsentId id);

    /**
     * Lists the consents of a producer, newest first.
     *
     * @param producerId the producer
     * @return the consents
     */
    List<DataSharingConsent> findAllByProducerId(ProducerId producerId);

    /**
     * Stores a consent.
     *
     * @param consent the consent to store
     * @return the stored consent
     */
    DataSharingConsent save(DataSharingConsent consent);

    /**
     * Lists the consents given to a license.
     *
     * @param licenseId the license
     * @return the consents
     */
    List<DataSharingConsent> findAllByLicenseId(CooperativeLicenseId licenseId);
}
