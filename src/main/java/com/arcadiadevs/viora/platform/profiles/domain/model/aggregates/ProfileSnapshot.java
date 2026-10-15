package com.arcadiadevs.viora.platform.profiles.domain.model.aggregates;

import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.*;

import java.time.Instant;

/**
 * Immutable state snapshot of a {@link Profile} aggregate root.
 *
 * @param id profile identifier VO
 * @param userId logical user identifier VO
 * @param fullName full name VO
 * @param country country code VO
 * @param phoneNumber phone number VO
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record ProfileSnapshot(
        ProfileId id,
        UserId userId,
        FullName fullName,
        CountryCode country,
        PhoneNumber phoneNumber,
        Instant createdAt,
        Instant updatedAt
) {
    public ProfileSnapshot {
        if (id == null) {
            throw new IllegalArgumentException("profile.snapshot.id.null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("profile.snapshot.user_id.null");
        }
        if (fullName == null) {
            throw new IllegalArgumentException("profile.snapshot.full_name.null");
        }
        if (country == null) {
            throw new IllegalArgumentException("profile.snapshot.country.null");
        }
        if (phoneNumber == null) {
            throw new IllegalArgumentException("profile.snapshot.phone.null");
        }
    }
}
