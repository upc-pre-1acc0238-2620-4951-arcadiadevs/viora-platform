package com.arcadiadevs.viora.platform.profiles.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserSignedUpWithProfileEvent;
import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("CreateProfileOnUserSignedUpHandler Unit Tests")
class CreateProfileOnUserSignedUpHandlerTest {

    private final ProfileCommandService profileCommandService = Mockito.mock(ProfileCommandService.class);
    private final CreateProfileOnUserSignedUpHandler handler = new CreateProfileOnUserSignedUpHandler(profileCommandService);

    @Test
    @DisplayName("Should reject null command service in constructor")
    void shouldRejectNullCommandService() {
        assertThatThrownBy(() -> new CreateProfileOnUserSignedUpHandler(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("profile.command_service.null");
    }

    @Test
    @DisplayName("Should successfully dispatch CreateProfileCommand on valid event")
    void shouldDispatchCreateProfileCommand() {
        var event = new UserSignedUpWithProfileEvent(
                "user-1",
                "Juan Perez",
                "PE",
                "+51987654321",
                Instant.now()
        );
        when(profileCommandService.handle(any(CreateProfileCommand.class)))
                .thenReturn(Result.success("profile-1"));

        assertThatCode(() -> handler.on(event)).doesNotThrowAnyException();
        verify(profileCommandService).handle(any(CreateProfileCommand.class));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when profile creation fails")
    void shouldThrowWhenCreationFails() {
        var event = new UserSignedUpWithProfileEvent(
                "user-1",
                "Juan Perez",
                "PE",
                "+51987654321",
                Instant.now()
        );
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
        verifyNoInteractions(profileCommandService);
    }
}
