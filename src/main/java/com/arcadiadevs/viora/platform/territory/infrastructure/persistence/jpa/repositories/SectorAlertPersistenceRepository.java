package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities.SectorAlertPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link SectorAlertPersistenceEntity}.
 */
@Repository
public interface SectorAlertPersistenceRepository extends JpaRepository<SectorAlertPersistenceEntity, UUID> {

    /**
     * Lists the open alerts of a license.
     *
     * @param licenseId the license
     * @return the entities
     */
    List<SectorAlertPersistenceEntity> findAllByLicenseIdAndNormalizedAtIsNull(UUID licenseId);

    /**
     * Lists the alerts of a license that are open or normalized since an instant, newest first.
     *
     * @param licenseId the license
     * @param since     the oldest normalization to include
     * @return the entities
     */
    @Query("select alert from SectorAlertPersistenceEntity alert where alert.licenseId = :licenseId "
            + "and (alert.normalizedAt is null or alert.normalizedAt >= :since) order by alert.openedAt desc")
    List<SectorAlertPersistenceEntity> findOpenOrNormalizedSince(@Param("licenseId") UUID licenseId,
                                                                 @Param("since") Instant since);
}
