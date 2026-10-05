package com.arcadiadevs.viora.platform.thinning.domain.model.aggregates;

import com.arcadiadevs.viora.platform.thinning.PrescriptionTestData;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionBlocker;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus;
import com.arcadiadevs.viora.platform.thinning.domain.services.CropLoadBalancingCalculatorService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FruitThinningPrescriptionFullBloomTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("Records the observed full bloom of the campaign")
    void recordsTheFullBloom() {
        var prescription = PrescriptionTestData.sampling(PrescriptionTestData.newPlot());

        prescription.recordFullBloom(LocalDate.of(2026, 10, 15), NOW);

        assertThat(prescription.snapshot().fullBloomOn()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    @DisplayName("Rejects a future date, a date of another campaign and a missing date")
    void rejectsInvalidDates() {
        var prescription = PrescriptionTestData.sampling(PrescriptionTestData.newPlot());

        assertThatThrownBy(() -> prescription.recordFullBloom(LocalDate.of(2026, 11, 2), NOW))
                .hasMessage("thinning.full_bloom.future");
        assertThatThrownBy(() -> prescription.recordFullBloom(LocalDate.of(2025, 10, 15), NOW))
                .hasMessage("thinning.full_bloom.campaign_mismatch");
        assertThatThrownBy(() -> prescription.recordFullBloom(null, NOW))
                .hasMessage("thinning.full_bloom.null");
    }

    @Test
    @DisplayName("Says exactly what is missing until the prescription can be issued")
    void listsTheBlockers() {
        var prescription = PrescriptionTestData.sampling(PrescriptionTestData.newPlot());

        assertThat(prescription.blockers(false)).containsExactly(
                PrescriptionBlocker.SAMPLING_NOT_REPRESENTATIVE,
                PrescriptionBlocker.TARGET_NOT_CONFIGURED,
                PrescriptionBlocker.FULL_BLOOM_MISSING);

        var sampled = PrescriptionTestData.representative(PrescriptionTestData.newPlot());
        assertThat(sampled.blockers(true)).containsExactly(PrescriptionBlocker.FULL_BLOOM_MISSING);

        sampled.recordFullBloom(LocalDate.of(2026, 10, 15), NOW);
        assertThat(sampled.blockers(true)).isEmpty();
    }

    @Test
    @DisplayName("Issues the prescription with the target, the window and where they come from")
    void issuesWithProvenance() {
        var prescription = PrescriptionTestData.representative(PrescriptionTestData.newPlot());

        prescription.determineSustainableCropLoad(
                new CropLoadBalancingCalculatorService(0.4),
                LocalDate.of(2026, 10, 29), LocalDate.of(2026, 12, 3), "test-1", "SYNTHETIC_DEMO");

        var snapshot = prescription.snapshot();
        assertThat(snapshot.status()).isEqualTo(PrescriptionStatus.PRESCRIBED);
        assertThat(snapshot.sustainableLoad().percentageToRemove()).isEqualTo(100.0 * (1.0 - 0.4 / 0.6));
        assertThat(snapshot.sustainableLoad().profileVersion()).isEqualTo("test-1");
        assertThat(snapshot.sustainableLoad().profileStatus()).isEqualTo("SYNTHETIC_DEMO");
        assertThat(prescription.blockers(true)).isEmpty();
    }

    @Test
    @DisplayName("Does not issue before the sampling is representative")
    void needsRepresentativeSampling() {
        var prescription = PrescriptionTestData.sampling(PrescriptionTestData.newPlot());

        assertThatThrownBy(() -> prescription.determineSustainableCropLoad(
                new CropLoadBalancingCalculatorService(0.4),
                LocalDate.of(2026, 10, 29), LocalDate.of(2026, 12, 3), "test-1", "SYNTHETIC_DEMO"))
                .hasMessage("thinning.sampling.not_representative");
    }
}
