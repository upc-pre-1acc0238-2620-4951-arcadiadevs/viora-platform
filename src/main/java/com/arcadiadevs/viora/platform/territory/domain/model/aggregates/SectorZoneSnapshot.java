package com.arcadiadevs.viora.platform.territory.domain.model.aggregates;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;

/**
 * Persisted state of a {@link SectorZone}.
 *
 * @param id        the sector zone
 * @param licenseId the license whose list it belongs to
 * @param name      its name
 * @param revision  the optimistic lock revision
 */
public record SectorZoneSnapshot(SectorZoneId id, LicenseId licenseId, SectorZoneName name, Long revision) {
}
