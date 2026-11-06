package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorStatus;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities.PlotSectorAssignmentPersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PlotSectorAssignmentPersistenceEntity}.
 */
@Repository
public interface PlotSectorAssignmentPersistenceRepository
        extends JpaRepository<PlotSectorAssignmentPersistenceEntity, UUID> {

    /**
     * Finds the sector of a plot in a license and locks its row until the transaction ends.
     *
     * @param licenseId the license
     * @param plotId    the plot
     * @return the entity, or empty
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from PlotSectorAssignmentPersistenceEntity assignment "
            + "where assignment.licenseId = :licenseId and assignment.plotId = :plotId")
    Optional<PlotSectorAssignmentPersistenceEntity> findLockedByLicenseIdAndPlotId(
            @Param("licenseId") UUID licenseId, @Param("plotId") UUID plotId);

    /**
     * Lists the pending plots of a license with a normalized written name, locking their rows.
     *
     * @param licenseId      the license
     * @param status         the pending status
     * @param normalizedName the normalized written name
     * @return the entities
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from PlotSectorAssignmentPersistenceEntity assignment "
            + "where assignment.licenseId = :licenseId and assignment.status = :status "
            + "and assignment.normalizedProposedName = :normalizedName")
    List<PlotSectorAssignmentPersistenceEntity> findLockedByLicenseIdAndStatusAndNormalizedProposedName(
            @Param("licenseId") UUID licenseId, @Param("status") PlotSectorStatus status,
            @Param("normalizedName") String normalizedName);

    /**
     * Lists the plots of a license.
     *
     * @param licenseId the license
     * @return the entities
     */
    List<PlotSectorAssignmentPersistenceEntity> findAllByLicenseId(UUID licenseId);

    /**
     * Lists a producer's plots in a license.
     *
     * @param licenseId  the license
     * @param producerId the producer
     * @return the entities
     */
    List<PlotSectorAssignmentPersistenceEntity> findAllByLicenseIdAndProducerId(UUID licenseId, UUID producerId);

    /**
     * Removes every row of a producer.
     *
     * @param producerId the producer
     */
    @Modifying
    @Query("delete from PlotSectorAssignmentPersistenceEntity assignment where assignment.producerId = :producerId")
    void deleteAllByProducerId(@Param("producerId") UUID producerId);
}
