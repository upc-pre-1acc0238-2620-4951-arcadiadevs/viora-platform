package com.arcadiadevs.viora.platform.profiles.application.internal.queryservices;

import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.CountryCode;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.FullName;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.PhoneNumber;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.profiles.domain.repositories.ProfileRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("ProfileQueryService Application Unit Tests")
class ProfileQueryServiceImplTest {

    private final ProfileRepository repository = Mockito.mock(ProfileRepository.class);
    private final ProfileQueryServiceImpl queryService = new ProfileQueryServiceImpl(repository);
    private final String owner = UUID.randomUUID().toString();

    @Test
    @DisplayName("Answers the profile, whatever the case of the identifier")
    void answersOwner() {
        var profile = Profile.create(new UserId(owner), new FullName("Ana Quispe"), new CountryCode("PE"),
                new PhoneNumber("+51987654321"));
        when(repository.findByUserId(any(UserId.class))).thenReturn(Optional.of(profile));

        var result = queryService.handle(new GetProfileByUserIdQuery(owner.toUpperCase()));

        assertThat(result).isInstanceOf(Result.Success.class);
    }

    @Test
    @DisplayName("Answers not found for an account without profile")
    void notFoundForOwnerWithoutProfile() {
        when(repository.findByUserId(any(UserId.class))).thenReturn(Optional.empty());

        var result = queryService.handle(new GetProfileByUserIdQuery(owner));

        assertThat(((Result.Failure<Profile, ApplicationError>) result).error().code()).isEqualTo("PROFILE_NOT_FOUND");
    }
}
