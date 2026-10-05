package com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices;

import com.arcadiadevs.viora.platform.thinning.PrescriptionTestData;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionBlocker;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ThinningPrescriptionIssuerTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ExternalOrchardService orchard;

    @Mock
    private ThinningProfiles profiles;

    private ThinningPrescriptionIssuer issuer;

    @BeforeEach
    void setUp() {
        issuer = new ThinningPrescriptionIssuer(orchard, profiles);
        lenient().when(orchard.findPlotVariety(any())).thenReturn(Optional.of("SEVILLANA"));
        lenient().when(profiles.forVariety("SEVILLANA")).thenReturn(Optional.of(PrescriptionTestData.profile()));
    }

    @Test
    @DisplayName("Issues the prescription once sampling, profile and full bloom are there, counting the window from the bloom")
    void issuesWhenReady() {
        var prescription = PrescriptionTestData.representative(PrescriptionTestData.newPlot());
        prescription.recordFullBloom(LocalDate.of(2026, 10, 15), NOW);

        boolean issued = issuer.issueIfReady(prescription, false);

        var load = prescription.snapshot().sustainableLoad();
        assertThat(issued).isTrue();
        assertThat(prescription.snapshot().status()).isEqualTo(PrescriptionStatus.PRESCRIBED);
        assertThat(load.windowOpensOn()).isEqualTo(LocalDate.of(2026, 10, 29));
        assertThat(load.windowClosesOn()).isEqualTo(LocalDate.of(2026, 12, 3));
        assertThat(load.targetFruitsPerShoot()).isEqualTo(0.4);
        assertThat(load.profileVersion()).isEqualTo("test-1");
    }

    @Test
    @DisplayName("Without the full bloom it issues nothing and says so")
    void needsTheFullBloom() {
        var prescription = PrescriptionTestData.representative(PrescriptionTestData.newPlot());

        assertThat(issuer.issueIfReady(prescription, false)).isFalse();
        assertThat(issuer.blockers(prescription)).containsExactly(PrescriptionBlocker.FULL_BLOOM_MISSING);
    }

    @Test
    @DisplayName("Without a profile for the variety it issues nothing and says so")
    void needsAProfile() {
        lenient().when(profiles.forVariety("SEVILLANA")).thenReturn(Optional.empty());
        var prescription = PrescriptionTestData.representative(PrescriptionTestData.newPlot());
        prescription.recordFullBloom(LocalDate.of(2026, 10, 15), NOW);

        assertThat(issuer.issueIfReady(prescription, false)).isFalse();
        assertThat(issuer.blockers(prescription)).containsExactly(PrescriptionBlocker.TARGET_NOT_CONFIGURED);
    }

    @Test
    @DisplayName("Without representative sampling it issues nothing")
    void needsRepresentativeSampling() {
        var prescription = PrescriptionTestData.sampling(PrescriptionTestData.newPlot());
        prescription.recordFullBloom(LocalDate.of(2026, 10, 15), NOW);

        assertThat(issuer.issueIfReady(prescription, false)).isFalse();
        assertThat(issuer.blockers(prescription)).containsExactly(PrescriptionBlocker.SAMPLING_NOT_REPRESENTATIVE);
    }

    @Test
    @DisplayName("A corrected full bloom recalculates the window of an issued prescription, only when asked to")
    void reissuesOnlyWhenAsked() {
        var prescription = PrescriptionTestData.representative(PrescriptionTestData.newPlot());
        prescription.recordFullBloom(LocalDate.of(2026, 10, 15), NOW);
        issuer.issueIfReady(prescription, false);
        prescription.recordFullBloom(LocalDate.of(2026, 10, 20), NOW);

        assertThat(issuer.issueIfReady(prescription, false)).isFalse();
        assertThat(prescription.snapshot().sustainableLoad().windowOpensOn()).isEqualTo(LocalDate.of(2026, 10, 29));

        assertThat(issuer.issueIfReady(prescription, true)).isTrue();
        assertThat(prescription.snapshot().sustainableLoad().windowOpensOn()).isEqualTo(LocalDate.of(2026, 11, 3));
    }
}
