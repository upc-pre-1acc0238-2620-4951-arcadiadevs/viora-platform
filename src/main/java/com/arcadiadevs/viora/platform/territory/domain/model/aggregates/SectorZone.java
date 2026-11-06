package com.arcadiadevs.viora.platform.territory.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;

/**
 * Aggregate root of a sector zone: one entry of the sector list of a cooperative license, such as «Magollo».
 * <p>A sector is a label of plots, not a frontier: nobody draws where it ends. Each license keeps its own list, its
 * manager adds and renames entries and, in the MVP, never deletes them. Names are unique within a license ignoring
 * accents and case; the application checks it against the other zones and the database enforces it.</p>
 */
public class SectorZone extends AbstractDomainAggregateRoot<SectorZone> {

    private final SectorZoneId id;
    private final LicenseId licenseId;
    private SectorZoneName name;
    private final Long revision;

    private SectorZone(SectorZoneSnapshot snapshot) {
        this.id = snapshot.id();
        this.licenseId = snapshot.licenseId();
        this.name = snapshot.name();
        this.revision = snapshot.revision();
    }

    /**
     * Adds a sector zone to the list of a license.
     *
     * @param licenseId the license
     * @param name      the name of the sector
     * @return the new sector zone
     * @throws IllegalArgumentException if an argument is null
     */
    public static SectorZone create(LicenseId licenseId, SectorZoneName name) {
        if (licenseId == null || name == null) {
            throw new IllegalArgumentException("territory.sector_zone.creation.incomplete");
        }
        return new SectorZone(new SectorZoneSnapshot(new SectorZoneId(), licenseId, name, 0L));
    }

    /**
     * Rebuilds a sector zone from its persisted state.
     *
     * @param snapshot the persisted state
     * @return the sector zone
     * @throws IllegalArgumentException if the snapshot is null or misses a part
     */
    public static SectorZone reconstitute(SectorZoneSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("territory.sector_zone.snapshot.null");
        }
        if (snapshot.id() == null || snapshot.licenseId() == null || snapshot.name() == null) {
            throw new IllegalArgumentException("territory.sector_zone.snapshot.incomplete");
        }
        return new SectorZone(snapshot);
    }

    /**
     * Renames the sector. Every plot in it shows the new name, since plots point to the zone and not to its name.
     *
     * @param newName the new name
     * @throws IllegalArgumentException if the name is null
     */
    public void rename(SectorZoneName newName) {
        if (newName == null) {
            throw new IllegalArgumentException("territory.sector_zone.name.blank");
        }
        this.name = newName;
    }

    /**
     * Tells whether this zone belongs to a license.
     *
     * @param license the license
     * @return true if it is in the list of that license
     */
    public boolean belongsTo(LicenseId license) {
        return licenseId.equals(license);
    }

    /**
     * Returns the current state of the sector zone.
     *
     * @return the snapshot
     */
    public SectorZoneSnapshot snapshot() {
        return new SectorZoneSnapshot(id, licenseId, name, revision);
    }
}
