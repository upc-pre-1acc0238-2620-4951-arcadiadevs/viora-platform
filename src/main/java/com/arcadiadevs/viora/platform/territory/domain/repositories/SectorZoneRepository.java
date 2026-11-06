package com.arcadiadevs.viora.platform.territory.domain.repositories;

import com.arcadiadevs.viora.platform.territory.domain.exceptions.SectorZoneNameTakenException;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZone;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;

import java.util.List;
import java.util.Optional;

/**
 * Repository port of the sector zones.
 */
public interface SectorZoneRepository {

    /**
     * Finds a sector zone of a license.
     *
     * @param id        the sector zone
     * @param licenseId the license it must belong to
     * @return the zone, or empty if it does not exist or belongs to another license
     */
    Optional<SectorZone> findByIdAndLicenseId(SectorZoneId id, LicenseId licenseId);

    /**
     * Finds the zone of a license with the same name, ignoring accents and case.
     *
     * @param licenseId the license
     * @param name      the name to compare
     * @return the zone, or empty if the list has no such name
     */
    Optional<SectorZone> findByLicenseIdAndName(LicenseId licenseId, SectorZoneName name);

    /**
     * Lists the zones of a license.
     *
     * @param licenseId the license
     * @return the zones, in no particular order
     */
    List<SectorZone> findAllByLicenseId(LicenseId licenseId);

    /**
     * Stores a zone and writes it at once, so a name taken by another zone fails here.
     *
     * @param zone the zone
     * @return the stored zone
     * @throws SectorZoneNameTakenException if the license already has that name
     */
    SectorZone save(SectorZone zone);
}
