package com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.thinning.PrescriptionTestData;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionBlocker;
import com.arcadiadevs.viora.platform.thinning.domain.services.CropLoadBalancingCalculatorService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionResourceFromEntityAssemblerTest {

    @Test
    @DisplayName("A prescription still being sampled shows no figures and what is missing")
    void showsTheBlockersWhileSampling() {
        var prescription = PrescriptionTestData.sampling(PrescriptionTestData.newPlot());

        var resource = PrescriptionResourceFromEntityAssembler.toResource(
                prescription.snapshot(), List.of(PrescriptionBlocker.TARGET_NOT_CONFIGURED));

        assertThat(resource.status()).isEqualTo("SAMPLING_IN_PROGRESS");
        assertThat(resource.targetFruitsPerShoot()).isNull();
        assertThat(resource.percentageToRemove()).isNull();
        assertThat(resource.windowOpensOn()).isNull();
        assertThat(resource.windowBasis()).isNull();
        assertThat(resource.isWindowOpen()).isFalse();
        assertThat(resource.blockers()).containsExactly("TARGET_NOT_CONFIGURED");
        assertThat(resource.loadUnit()).isEqualTo("FRUITS_PER_SHOOT");
    }

    @Test
    @DisplayName("An issued prescription shows its figures rounded, its window, its basis and its profile")
    void showsAnIssuedPrescription() {
        var today = LocalDate.now(ZoneOffset.UTC);
        var prescription = PrescriptionTestData.representative(PrescriptionTestData.newPlot());
        prescription.determineSustainableCropLoad(new CropLoadBalancingCalculatorService(0.4),
                today.minusDays(1), today.plusDays(10), "test-1", "SYNTHETIC_DEMO");

        var resource = PrescriptionResourceFromEntityAssembler.toResource(prescription.snapshot(), List.of());

        assertThat(resource.status()).isEqualTo("PRESCRIBED");
        assertThat(resource.targetFruitsPerShoot()).isEqualTo(0.4);
        assertThat(resource.percentageToRemove()).isEqualTo(33.33);
        assertThat(resource.windowBasis()).isEqualTo("FULL_BLOOM_PLUS_PROFILE_OFFSETS");
        assertThat(resource.profileVersion()).isEqualTo("test-1");
        assertThat(resource.profileStatus()).isEqualTo("SYNTHETIC_DEMO");
        assertThat(resource.isWindowOpen()).isTrue();
        assertThat(resource.blockers()).isEmpty();
    }

    @Test
    @DisplayName("The window is not open before it opens or after it closes")
    void windowIsOpenOnlyBetweenItsDates() {
        var today = LocalDate.now(ZoneOffset.UTC);
        var early = PrescriptionTestData.representative(PrescriptionTestData.newPlot());
        early.determineSustainableCropLoad(new CropLoadBalancingCalculatorService(0.4),
                today.plusDays(3), today.plusDays(30), "test-1", "SYNTHETIC_DEMO");
        var late = PrescriptionTestData.representative(PrescriptionTestData.newPlot());
        late.determineSustainableCropLoad(new CropLoadBalancingCalculatorService(0.4),
                today.minusDays(30), today.minusDays(1), "test-1", "SYNTHETIC_DEMO");

        assertThat(PrescriptionResourceFromEntityAssembler.toResource(early.snapshot(), List.of()).isWindowOpen()).isFalse();
        assertThat(PrescriptionResourceFromEntityAssembler.toResource(late.snapshot(), List.of()).isWindowOpen()).isFalse();
    }
}
