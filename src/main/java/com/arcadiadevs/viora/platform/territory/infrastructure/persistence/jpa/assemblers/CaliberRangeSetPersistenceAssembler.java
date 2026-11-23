package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.CaliberRangeSet;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.CaliberRangeSetSnapshot;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberRange;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberRangeSetId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities.CaliberRangeEmbeddable;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities.CaliberRangeSetPersistenceEntity;

import java.util.UUID;

/**
 * Maps the caliber ranges of a license between the domain and the database.
 */
public final class CaliberRangeSetPersistenceAssembler {

    private CaliberRangeSetPersistenceAssembler() {
    }

    public static CaliberRangeSetPersistenceEntity toPersistenceFromDomain(CaliberRangeSetPersistenceEntity target,
                                                                           CaliberRangeSet domain) {
        if (target == null || domain == null) {
            throw new IllegalArgumentException("territory.caliber_range_set.entity_or_domain.null");
        }
        var snapshot = domain.snapshot();
        target.setId(UUID.fromString(snapshot.id().caliberRangeSetId()));
        target.setLicenseId(UUID.fromString(snapshot.licenseId().licenseId()));
        target.getRanges().clear();
        for (var range : snapshot.ranges()) {
            var row = new CaliberRangeEmbeddable();
            row.setMinFruitsPerKg(range.minFruitsPerKg());
            row.setMaxFruitsPerKg(range.maxFruitsPerKg());
            target.getRanges().add(row);
        }
        return target;
    }

    public static CaliberRangeSet toDomainFromPersistence(CaliberRangeSetPersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("territory.caliber_range_set.entity.null");
        }
        return CaliberRangeSet.reconstitute(new CaliberRangeSetSnapshot(
                new CaliberRangeSetId(entity.getId().toString()), new LicenseId(entity.getLicenseId().toString()),
                entity.getRanges().stream()
                        .map(row -> new CaliberRange(row.getMinFruitsPerKg(), row.getMaxFruitsPerKg()))
                        .toList(),
                entity.getRevision()));
    }
}
