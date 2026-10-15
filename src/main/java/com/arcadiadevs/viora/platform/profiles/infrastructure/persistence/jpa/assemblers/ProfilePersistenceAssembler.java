package com.arcadiadevs.viora.platform.profiles.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.ProfileSnapshot;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.profiles.infrastructure.persistence.jpa.entities.ProfilePersistenceEntity;

import java.util.UUID;

/**
 * Unified assembler translating between Profile domain aggregate and JPA persistence entity.
 */
public final class ProfilePersistenceAssembler {

    private ProfilePersistenceAssembler() {
    }

    /**
     * Converts a domain Profile aggregate into a JPA entity.
     *
     * @param profile domain aggregate
     * @return JPA persistence entity
     */
    public static ProfilePersistenceEntity toPersistenceFromDomain(Profile profile) {
        if (profile == null) {
            return null;
        }
        var snapshot = profile.snapshot();
        var entity = new ProfilePersistenceEntity();
        entity.setId(UUID.fromString(snapshot.id().profileId()));
        entity.setUserId(UUID.fromString(snapshot.userId().userId()));
        entity.setFullName(snapshot.fullName().value());
        entity.setCountry(snapshot.country().code());
        entity.setPhoneNumber(snapshot.phoneNumber().value());
        entity.setCreatedAt(snapshot.createdAt());
        entity.setUpdatedAt(snapshot.updatedAt());
        return entity;
    }

    /**
     * Reconstitutes a domain Profile aggregate from a JPA entity.
     *
     * @param entity JPA persistence entity
     * @return domain aggregate root
     */
    public static Profile toDomainFromPersistence(ProfilePersistenceEntity entity) {
        if (entity == null) {
            return null;
        }
        var snapshot = new ProfileSnapshot(
                new ProfileId(entity.getId().toString()),
                new UserId(entity.getUserId().toString()),
                new FullName(entity.getFullName()),
                new CountryCode(entity.getCountry()),
                new PhoneNumber(entity.getPhoneNumber()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
        return Profile.reconstitute(snapshot);
    }
}
