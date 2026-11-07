package com.arcadiadevs.viora.platform.territory.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskLevel;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertFacts;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertType;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;

import java.time.Instant;

/**
 * Aggregate root of an alert about a sector of a license, as the manager's alert center shows it: it opens when a
 * sector turns critical or frost comes, keeps its facts up to date while it lasts, and normalizes once the cause is
 * gone, remembering how the sector was left («Magollo volvió a Moderado»).
 * <p>It is its own root because it outlives the colors it summarizes: the colors are computed each time, the alert is
 * what lets the manager see when a sector changed. A sector has at most one open alert of each type.</p>
 */
public class SectorAlert extends AbstractDomainAggregateRoot<SectorAlert> {

    private final SectorAlertId id;
    private final LicenseId licenseId;
    private final SectorZoneId sectorZoneId;
    private final SectorAlertType type;
    private SectorAlertFacts facts;
    private final Instant openedAt;
    private Instant updatedAt;
    private Instant normalizedAt;
    private RiskLevel levelAtNormalize;
    private final Long revision;

    private SectorAlert(SectorAlertSnapshot snapshot) {
        this.id = snapshot.id();
        this.licenseId = snapshot.licenseId();
        this.sectorZoneId = snapshot.sectorZoneId();
        this.type = snapshot.type();
        this.facts = snapshot.facts();
        this.openedAt = snapshot.openedAt();
        this.updatedAt = snapshot.updatedAt();
        this.normalizedAt = snapshot.normalizedAt();
        this.levelAtNormalize = snapshot.levelAtNormalize();
        this.revision = snapshot.revision();
    }

    /**
     * Opens an alert about a sector.
     *
     * @param licenseId    the license
     * @param sectorZoneId the sector
     * @param type         what it is about
     * @param facts        what it says
     * @param at           when it opens
     * @return the open alert
     * @throws IllegalArgumentException if a part is null
     */
    public static SectorAlert open(LicenseId licenseId, SectorZoneId sectorZoneId, SectorAlertType type,
                                   SectorAlertFacts facts, Instant at) {
        if (licenseId == null || sectorZoneId == null || type == null || facts == null || at == null) {
            throw new IllegalArgumentException("territory.sector_alert.opening.incomplete");
        }
        return new SectorAlert(new SectorAlertSnapshot(new SectorAlertId(), licenseId, sectorZoneId, type, facts,
                at, at, null, null, 0L));
    }

    /**
     * Rebuilds an alert from its persisted state.
     *
     * @param snapshot the persisted state
     * @return the alert
     * @throws IllegalArgumentException if the snapshot is null or misses a part
     */
    public static SectorAlert reconstitute(SectorAlertSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("territory.sector_alert.snapshot.null");
        }
        if (snapshot.id() == null || snapshot.licenseId() == null || snapshot.sectorZoneId() == null
                || snapshot.type() == null || snapshot.facts() == null || snapshot.openedAt() == null
                || snapshot.updatedAt() == null) {
            throw new IllegalArgumentException("territory.sector_alert.snapshot.incomplete");
        }
        return new SectorAlert(snapshot);
    }

    /**
     * Updates what an open alert says, such as one more plot in red. A normalized alert never changes.
     *
     * @param newFacts what it says now
     * @param at       when
     * @return true if something changed
     */
    public boolean refresh(SectorAlertFacts newFacts, Instant at) {
        if (!isOpen() || newFacts == null || newFacts.equals(facts)) {
            return false;
        }
        this.facts = newFacts;
        this.updatedAt = at;
        return true;
    }

    /**
     * Closes the alert once its cause is gone. A normalized alert stays normalized.
     *
     * @param sectorLevel the sector's color now
     * @param at          when it normalizes
     * @return true if it was open
     */
    public boolean normalize(RiskLevel sectorLevel, Instant at) {
        if (!isOpen() || at == null) {
            return false;
        }
        this.normalizedAt = at;
        this.levelAtNormalize = sectorLevel;
        this.updatedAt = at;
        return true;
    }

    /**
     * Tells whether the alert is still open.
     *
     * @return true until it normalizes
     */
    public boolean isOpen() {
        return normalizedAt == null;
    }

    /**
     * Returns the current state of the alert.
     *
     * @return the snapshot
     */
    public SectorAlertSnapshot snapshot() {
        return new SectorAlertSnapshot(id, licenseId, sectorZoneId, type, facts, openedAt, updatedAt, normalizedAt,
                levelAtNormalize, revision);
    }
}
