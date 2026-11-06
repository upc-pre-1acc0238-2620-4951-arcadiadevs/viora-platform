package com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.profiles.interfaces.acl.ProfileContextFacade;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Subscription Profiles anti-corruption layer unit tests")
class ExternalProfileServiceTest {

    private final ProfileContextFacade profileContextFacade = mock(ProfileContextFacade.class);
    private final ExternalProfileService service = new ExternalProfileService(profileContextFacade);
    private final ProducerId producer = new ProducerId(UUID.randomUUID().toString());

    @Test
    @DisplayName("A producer has a profile only when Profiles has one")
    void profileComesFromProfiles() {
        when(profileContextFacade.existsProfileByUserId(producer.producerId())).thenReturn(false);
        assertThat(service.hasProfile(producer)).isFalse();

        when(profileContextFacade.existsProfileByUserId(producer.producerId())).thenReturn(true);
        assertThat(service.hasProfile(producer)).isTrue();
        assertThat(service.hasProfile(null)).isFalse();
    }
}
