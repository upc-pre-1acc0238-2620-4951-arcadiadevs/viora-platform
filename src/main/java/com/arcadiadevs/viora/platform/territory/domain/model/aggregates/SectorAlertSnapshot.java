package com.arcadiadevs.viora.platform.territory.domain.model.aggregates;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskLevel;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertFacts;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertType;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;

import java.time.Instant;

/**
 * Persisted state of a {@link SectorAlert}.
 *
 * @param id               the alert
 * @param licenseId        the license
 * @param sectorZoneId     the sector
 * @param type             what it is about
 * @param facts            what it says
 * @param openedAt         when it opened
 * @param updatedAt        when its facts last changed
 * @param normalizedAt     when it normalized; null while open
 * @param levelAtNormalize the sector's color when it normalized; null while open
 * @param revision         the optimistic lock revision
 */
public record SectorAlertSnapshot(SectorAlertId id, LicenseId licenseId, SectorZoneId sectorZoneId,
                                  SectorAlertType type, SectorAlertFacts facts, Instant openedAt, Instant updatedAt,
                                  Instant normalizedAt, RiskLevel levelAtNormalize, Long revision) {
}
