package com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phenology ExternalOrchardService ACL Unit Tests")
class ExternalOrchardServiceTest {

    @Mock
    private OrchardContextFacade orchardContextFacade;

    private ExternalOrchardService externalOrchardService;

    private final String plotIdStr = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        externalOrchardService = new ExternalOrchardService(orchardContextFacade);
    }

    @Test
    @DisplayName("Should delegate to OrchardContextFacade when checking active plot")
    void shouldDelegateToOrchardContextFacade() {
        var plotId = new PlotId(plotIdStr);
        when(orchardContextFacade.existsActivePlot(plotIdStr)).thenReturn(true);

        assertThat(externalOrchardService.existsActivePlot(plotId)).isTrue();
    }

    @Test
    @DisplayName("Should return false when PlotId is null")
    void shouldReturnFalseWhenPlotIdIsNull() {
        assertThat(externalOrchardService.existsActivePlot(null)).isFalse();
    }
}
