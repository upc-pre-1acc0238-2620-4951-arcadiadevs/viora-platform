package com.arcadiadevs.viora.platform.thinning.application.internal.security;

import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.thinning.PrescriptionTestData;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionId;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionAccessPolicyTest {

    private static final String OWNER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String OTHER = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";

    @Mock FruitThinningPrescriptionRepository repository;
    @Mock ExternalOrchardService orchard;
    @Mock CurrentUserProvider currentUser;

    private PrescriptionAccessPolicy policy;
    private String prescriptionId;

    @BeforeEach
    void setUp() {
        policy = new PrescriptionAccessPolicy(repository, orchard, currentUser);
        var plotId = PrescriptionTestData.newPlot();
        var prescription = PrescriptionTestData.sampling(plotId);
        prescriptionId = prescription.snapshot().id().prescriptionId();
        lenient().when(repository.findById(new PrescriptionId(prescriptionId))).thenReturn(Optional.of(prescription));
        lenient().when(orchard.findPlotOwnerId(plotId)).thenReturn(Optional.of(OWNER));
    }

    @Test
    void letsTheOwnerOfThePlotIn() {
        when(currentUser.requireCurrentUserId()).thenReturn(OWNER);

        assertThat(policy.isAccessible(prescriptionId)).isTrue();
    }

    @Test
    void keepsAnotherProducerOut() {
        when(currentUser.requireCurrentUserId()).thenReturn(OTHER);

        assertThat(policy.isAccessible(prescriptionId)).isFalse();
    }

    @Test
    void letsUnknownAndMalformedIdsThroughSoTheEndpointAnswersItsOwn404Or400() {
        assertThat(policy.isAccessible("not-a-uuid")).isTrue();
        assertThat(policy.isAccessible("7c9e6679-7425-40de-944b-e07fc1f90ae7")).isTrue();
        verifyNoInteractions(currentUser);
    }
}
