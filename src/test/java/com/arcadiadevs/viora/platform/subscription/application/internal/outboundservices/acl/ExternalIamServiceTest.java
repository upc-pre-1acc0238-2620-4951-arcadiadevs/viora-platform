package com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.iam.interfaces.acl.IamContextFacade;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Subscription IAM anti-corruption layer unit tests")
class ExternalIamServiceTest {

    private final IamContextFacade iamContextFacade = mock(IamContextFacade.class);
    private final ExternalIamService service = new ExternalIamService(iamContextFacade);
    private final ProducerId producer = new ProducerId(UUID.randomUUID().toString());

    @Test
    @DisplayName("Only a user IAM knows as a producer is one; an unknown user or another role is not")
    void onlyKnownProducers() {
        when(iamContextFacade.findUserRole(producer.producerId())).thenReturn(Optional.empty());
        assertThat(service.isProducer(producer)).isFalse();

        when(iamContextFacade.findUserRole(producer.producerId())).thenReturn(Optional.of("ROLE_TECHNICAL_MANAGER"));
        assertThat(service.isProducer(producer)).isFalse();

        when(iamContextFacade.findUserRole(producer.producerId())).thenReturn(Optional.of("ROLE_PRODUCER"));
        assertThat(service.isProducer(producer)).isTrue();
        assertThat(service.isProducer(null)).isFalse();
    }
}
