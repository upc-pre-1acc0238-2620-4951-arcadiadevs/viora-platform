package com.arcadiadevs.viora.platform.profiles.domain.model.aggregates;

import com.arcadiadevs.viora.platform.profiles.domain.model.events.ContactProfileUpdatedEvent;
import com.arcadiadevs.viora.platform.profiles.domain.model.events.ProfileCreatedEvent;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.Instant;

/**
 * Pure Domain Aggregate Root representing a user's civil identity and contact profile.
 *
 * <p>Encapsulates person naming, residency country, and E.164 verified phone number invariants.
 * Zero JPA, Spring annotations, or setters.</p>
 */
public class Profile extends AbstractDomainAggregateRoot<Profile> {

    private final ProfileId id;
    private final UserId userId;
    private FullName fullName;
    private CountryCode country;
    private PhoneNumber phoneNumber;
    private final Instant createdAt;
    private Instant updatedAt;

    private Profile(
            ProfileId id,
            UserId userId,
            FullName fullName,
            CountryCode country,
            PhoneNumber phoneNumber,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.country = country;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method initializing a new user profile.
     *
     * @param userId owner user identifier
     * @param fullName valid full name
     * @param country valid ISO alpha-2 country
     * @param phoneNumber valid E.164 phone number
     * @return newly created Profile aggregate
     */
    public static Profile create(UserId userId, FullName fullName, CountryCode country, PhoneNumber phoneNumber) {
        if (userId == null) {
            throw new IllegalArgumentException("profile.user_id.null");
        }
        if (fullName == null) {
            throw new IllegalArgumentException("profile.full_name.null");
        }
        if (country == null) {
            throw new IllegalArgumentException("profile.country.null");
        }
        if (phoneNumber == null) {
            throw new IllegalArgumentException("profile.phone.null");
        }

        var id = new ProfileId();
        var now = Instant.now();
        var profile = new Profile(id, userId, fullName, country, phoneNumber, now, now);
        profile.registerDomainEvent(new ProfileCreatedEvent(id.profileId(), userId.userId(), fullName.value(), now));
        return profile;
    }

    /**
     * Reconstitutes an existing aggregate root from its persistence snapshot.
     *
     * @param snapshot immutable snapshot
     * @return reconstituted Profile aggregate
     */
    public static Profile reconstitute(ProfileSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("profile.snapshot.null");
        }
        return new Profile(
                snapshot.id(),
                snapshot.userId(),
                snapshot.fullName(),
                snapshot.country(),
                snapshot.phoneNumber(),
                snapshot.createdAt(),
                snapshot.updatedAt()
        );
    }

    /**
     * Updates personal and contact details.
     *
     * @param newFullName updated full name
     * @param newCountry updated country code
     * @param newPhoneNumber updated phone number
     */
    public void updateContact(FullName newFullName, CountryCode newCountry, PhoneNumber newPhoneNumber) {
        if (newFullName == null) {
            throw new IllegalArgumentException("profile.full_name.null");
        }
        if (newCountry == null) {
            throw new IllegalArgumentException("profile.country.null");
        }
        if (newPhoneNumber == null) {
            throw new IllegalArgumentException("profile.phone.null");
        }

        this.fullName = newFullName;
        this.country = newCountry;
        this.phoneNumber = newPhoneNumber;
        this.updatedAt = Instant.now();

        registerDomainEvent(new ContactProfileUpdatedEvent(
                this.id.profileId(),
                this.userId.userId(),
                newFullName.value(),
                newCountry.code(),
                newPhoneNumber.value(),
                this.updatedAt
        ));
    }

    /**
     * Exposes current aggregate state as an immutable snapshot.
     *
     * @return ProfileSnapshot
     */
    public ProfileSnapshot snapshot() {
        return new ProfileSnapshot(
                this.id,
                this.userId,
                this.fullName,
                this.country,
                this.phoneNumber,
                this.createdAt,
                this.updatedAt
        );
    }
}
