package com.arcadiadevs.viora.platform.profiles.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserSignedUpWithProfileEvent;
import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.application.queryservices.ProfileQueryService;
import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.UpdateContactProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("CreateProfileOnUserSignedUpHandler Unit Tests")
class CreateProfileOnUserSignedUpHandlerTest {

    private final ProfileCommandService profileCommandService = Mockito.mock(ProfileCommandService.class);
    private final ProfileQueryService profileQueryService = Mockito.mock(ProfileQueryService.class);
    private final CreateProfileOnUserSignedUpHandler handler =
            new CreateProfileOnUserSignedUpHandler(profileCommandService, profileQueryService);

    private final UserSignedUpWithProfileEvent event = new UserSignedUpWithProfileEvent(
            UUID.randomUUID().toString(), "Juan Perez", "PE", "+51987654321", Instant.now());

    @Test
    @DisplayName("Should reject null dependencies in constructor")
    void shouldRejectNullDependencies() {
        assertThatThrownBy(() -> new CreateProfileOnUserSignedUpHandler(null, profileQueryService))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("profile.command_service.null");
        assertThatThrownBy(() -> new CreateProfileOnUserSignedUpHandler(profileCommandService, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("profile.query_service.null");
    }

    @Test
    @DisplayName("Should create the profile on the first sign-up")
    void shouldDispatchCreateProfileCommand() {
        when(profileQueryService.handle(any(GetProfileByUserIdQuery.class))).thenReturn(Optional.empty());
        when(profileCommandService.handle(any(CreateProfileCommand.class))).thenReturn(Result.success("profile-1"));

        assertThatCode(() -> handler.on(event)).doesNotThrowAnyException();
        verify(profileCommandService).handle(any(CreateProfileCommand.class));
        verify(profileCommandService, never()).handle(any(UpdateContactProfileCommand.class));
    }

    @Test
    @DisplayName("Should update the existing profile when a pending account signs up again")
    void shouldUpdateExistingProfile() {
        var profile = Mockito.mock(Profile.class);
        when(profileQueryService.handle(any(GetProfileByUserIdQuery.class))).thenReturn(Optional.of(profile));
        when(profileCommandService.handle(any(UpdateContactProfileCommand.class))).thenReturn(Result.success(profile));

        assertThatCode(() -> handler.on(event)).doesNotThrowAnyException();
        verify(profileCommandService).handle(any(UpdateContactProfileCommand.class));
        verify(profileCommandService, never()).handle(any(CreateProfileCommand.class));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when profile creation fails")
    void shouldThrowWhenCreationFails() {
        when(profileQueryService.handle(any(GetProfileByUserIdQuery.class))).thenReturn(Optional.empty());
        when(profileCommandService.handle(any(CreateProfileCommand.class)))
                .thenReturn(Result.failure(ApplicationError.businessRuleViolation("profile.failed", "Error")));

        assertThatThrownBy(() -> handler.on(event))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Business rule violation: profile.failed");
    }

    @Test
    @DisplayName("Should handle null event safely")
    void shouldHandleNullEventSafely() {
        assertThatCode(() -> handler.on(null)).doesNotThrowAnyException();
        verifyNoInteractions(profileCommandService, profileQueryService);
    }
}
