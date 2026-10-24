package com.arcadiadevs.viora.platform.profiles.application.internal.commandservices;

import com.arcadiadevs.viora.platform.profiles.application.internal.outboundservices.acl.ExternalIamService;
import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.UpdateContactProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.ports.PhoneNumberValidator;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.CountryCode;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.FullName;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.PhoneNumber;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.profiles.domain.repositories.ProfileRepository;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProfileCommandServiceImpl · contact update")
class ProfileCommandServiceImplTest {

    private final ProfileRepository repository = Mockito.mock(ProfileRepository.class);
    private final ExternalIamService iam = Mockito.mock(ExternalIamService.class);
    private final PhoneNumberValidator phones = Mockito.mock(PhoneNumberValidator.class);
    private final ProfileCommandServiceImpl service = new ProfileCommandServiceImpl(
            repository, iam, phones, Mockito.mock(ApplicationEventPublisher.class));
    private final String userId = UUID.randomUUID().toString();
    private final UpdateContactProfileCommand command =
            new UpdateContactProfileCommand(userId, "Victor Ramos", "PE", "987654321");

    {
        when(phones.formatToE164("987654321", "PE")).thenReturn("+51987654321");
        when(repository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("an account that signed up without details gets its profile on the first save")
    void createsTheMissingProfileOfAnExistingAccount() {
        when(repository.findByUserId(new UserId(userId))).thenReturn(Optional.empty());
        when(iam.existsUser(new UserId(userId))).thenReturn(true);

        var result = service.handle(command);

        assertThat(result).isInstanceOf(Result.Success.class);
        var saved = ((Result.Success<Profile, ?>) result).value().snapshot();
        assertThat(saved.fullName().value()).isEqualTo("Victor Ramos");
        assertThat(saved.phoneNumber().value()).isEqualTo("+51987654321");
        verify(repository).save(any(Profile.class));
    }

    @Test
    @DisplayName("an unknown account still answers not found and saves nothing")
    void unknownAccountIsNotFound() {
        when(repository.findByUserId(new UserId(userId))).thenReturn(Optional.empty());
        when(iam.existsUser(new UserId(userId))).thenReturn(false);

        var result = service.handle(command);

        assertThat(result).isInstanceOf(Result.Failure.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("an existing profile is updated in place")
    void updatesAnExistingProfile() {
        var existing = Profile.create(new UserId(userId), new FullName("Old Name"), new CountryCode("PE"),
                new PhoneNumber("+51911111111"));
        when(repository.findByUserId(new UserId(userId))).thenReturn(Optional.of(existing));

        var result = service.handle(command);

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(existing.snapshot().fullName().value()).isEqualTo("Victor Ramos");
        verify(iam, never()).existsUser(any());
    }
}
