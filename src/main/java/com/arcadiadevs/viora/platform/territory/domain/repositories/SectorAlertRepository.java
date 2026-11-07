package com.arcadiadevs.viora.platform.territory.domain.repositories;

import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorAlert;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;

import java.time.Instant;
import java.util.List;

/**
 * Repository port of the sector alerts.
 */
public interface SectorAlertRepository {

    /**
     * Lists the open alerts of a license.
     *
     * @param licenseId the license
     * @return the open alerts
     */
    List<SectorAlert> findOpenByLicenseId(LicenseId licenseId);

    /**
     * Lists the alerts of a license that are open or normalized since an instant, newest first.
     *
     * @param licenseId the license
     * @param since     the oldest normalization to include
     * @return the alerts
     */
    List<SectorAlert> findOpenOrNormalizedSince(LicenseId licenseId, Instant since);

    /**
     * Stores an alert.
     *
     * @param alert the alert
     * @return the stored alert
     */
    SectorAlert save(SectorAlert alert);
}
