package com.arcadiadevs.viora.platform.profiles.domain.model;

import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.events.ContactProfileUpdatedEvent;
import com.arcadiadevs.viora.platform.profiles.domain.model.events.ProfileCreatedEvent;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Profile Aggregate Root and Value Objects Domain Unit Tests")
class ProfileTest {

    private final UserId userId = new UserId(UUID.randomUUID().toString());
    private final FullName fullName = new FullName("Carlos Mendoza Alarcón");
    private final CountryCode country = new CountryCode("PE");
    private final PhoneNumber phone = new PhoneNumber("+51987654321");

    @Test
    @DisplayName("Should successfully create Profile and publish ProfileCreatedEvent")
    void shouldSuccessfullyCreateProfile() {
        var profile = Profile.create(userId, fullName, country, phone);

        assertThat(profile).isNotNull();
        var snapshot = profile.snapshot();
        assertThat(snapshot.id()).isNotNull();
        assertThat(snapshot.userId().userId()).isEqualTo(userId.userId());
        assertThat(snapshot.fullName().value()).isEqualTo("Carlos Mendoza Alarcón");
        assertThat(snapshot.country().code()).isEqualTo("PE");
        assertThat(snapshot.phoneNumber().value()).isEqualTo("+51987654321");

        assertThat(profile.domainEvents()).hasSize(1);
        var event = (ProfileCreatedEvent) profile.domainEvents().iterator().next();
        assertThat(event.profileId()).isEqualTo(snapshot.id().profileId());
        assertThat(event.userId()).isEqualTo(userId.userId());
        assertThat(event.fullName()).isEqualTo("Carlos Mendoza Alarcón");
    }

    @Test
    @DisplayName("Should validate FullName length and non-blank constraint")
    void shouldValidateFullName() {
        assertThatThrownBy(() -> new FullName(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("profile.full_name.null_or_empty");

        assertThatThrownBy(() -> new FullName("A"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("profile.full_name.length_invalid");
    }

    @Test
    @DisplayName("Should validate CountryCode ISO-3166-1 alpha-2 validity")
    void shouldValidateCountryCode() {
        var valid = new CountryCode("es");
        assertThat(valid.code()).isEqualTo("ES");

        assertThatThrownBy(() -> new CountryCode("XYZ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("profile.country.invalid_iso_alpha2");
    }

    @Test
    @DisplayName("Should validate PhoneNumber international E.164 syntax")
    void shouldValidatePhoneNumber() {
        var validPhone = new PhoneNumber("+51987654321");
        assertThat(validPhone.value()).isEqualTo("+51987654321");

        assertThatThrownBy(() -> new PhoneNumber("12345"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("profile.phone.invalid_e164");

        assertThatThrownBy(() -> new PhoneNumber(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("profile.phone.null_or_empty");
    }

    @Test
    @DisplayName("Should update contact details and publish ContactProfileUpdatedEvent")
    void shouldUpdateContactDetails() {
        var profile = Profile.create(userId, fullName, country, phone);
        profile.clearDomainEvents();

        var newName = new FullName("Carlos Alberto Mendoza");
        var newCountry = new CountryCode("ES");
        var newPhone = new PhoneNumber("+34612345678");

        profile.updateContact(newName, newCountry, newPhone);

        var snapshot = profile.snapshot();
        assertThat(snapshot.fullName().value()).isEqualTo("Carlos Alberto Mendoza");
        assertThat(snapshot.country().code()).isEqualTo("ES");
        assertThat(snapshot.phoneNumber().value()).isEqualTo("+34612345678");

        assertThat(profile.domainEvents()).hasSize(1);
        var event = (ContactProfileUpdatedEvent) profile.domainEvents().iterator().next();
        assertThat(event.fullName()).isEqualTo("Carlos Alberto Mendoza");
        assertThat(event.country()).isEqualTo("ES");
        assertThat(event.phoneNumber()).isEqualTo("+34612345678");
    }
}
