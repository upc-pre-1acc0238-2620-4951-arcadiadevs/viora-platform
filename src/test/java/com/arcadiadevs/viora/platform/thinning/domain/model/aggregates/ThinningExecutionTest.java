package com.arcadiadevs.viora.platform.thinning.domain.model.aggregates;

import com.arcadiadevs.viora.platform.thinning.domain.model.events.ThinningExecutionConfirmedEvent;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.thinning.domain.services.CaliberModelFittingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import static com.arcadiadevs.viora.platform.thinning.ThinningExecutionFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class ThinningExecutionTest {
    private static final LocalDate CUTOFF = LocalDate.of(2026, 9, 25);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC);

    @ParameterizedTest
    @CsvSource({"2026-09-24,OPTIMAL", "2026-09-25,OPTIMAL", "2026-09-26,LATE"})
    void qualifiesExecutionAndPublishesCompleteEvidence(LocalDate date, ExecutionTimeliness timeliness) {
        var prescription = prescribed(CUTOFF);
        prescription.confirmExecution(date, 420, 25, 4, "Field notes", CLOCK);
        var snapshot = prescription.snapshot();
        var confirmation = snapshot.executionConfirmation();
        assertEquals(PrescriptionStatus.EXECUTED, snapshot.status());
        assertEquals(timeliness, confirmation.timeliness());
        assertEquals(420.0, confirmation.removedKg());
        assertEquals("Field notes", confirmation.notes());
        assertEquals(CLOCK.instant(), confirmation.recordedAt());
        assertEquals(1, prescription.domainEvents().size());
        var event = (ThinningExecutionConfirmedEvent) prescription.domainEvents().iterator().next();
        assertEquals(snapshot.id().prescriptionId(), event.prescriptionId());
        assertEquals(snapshot.plotId().plotId(), event.plotId());
        assertEquals(confirmation.id().confirmationId(), event.confirmationId());
        assertEquals(2026, event.campaignYear());
        assertEquals(date, event.executedDate());
        assertEquals(420.0, event.removedKg());
        assertEquals(4, event.laborCrewSize());
        assertEquals(25.0, event.removalPercentage());
        assertEquals(timeliness.name(), event.timeliness());
        var balance = confirmation.loadBalance();
        assertEquals(SAMPLED_FRUITS_PER_SHOOT, balance.preThinningFruitsPerShoot());
        assertEquals(6.3, balance.residualFruitsPerShoot(), 1e-9);
        assertEquals(TARGET_FRUITS_PER_SHOOT, balance.targetFruitsPerShoot());
        assertEquals(0.3, balance.deltaFruitsPerShoot(), 1e-9);
        assertEquals(1.05, balance.loadRatio(), 1e-12);
        assertEquals(LoadState.MODERATE_OVERLOAD, balance.loadState());
        assertEquals(timeliness == ExecutionTimeliness.LATE
                        ? CaliberProjectionStatus.NOT_ESTIMATED_LATE : CaliberProjectionStatus.NOT_CALIBRATED,
                confirmation.caliberProjection().status());
    }

    @Test
    void projectsCaliberWhenThePlotVarietyIsCalibrated() {
        var prescription = prescribed(CUTOFF);
        var calibration = CaliberModelFittingService.calibrate("SEVILLANA", exactObservations("SEVILLANA"));
        prescription.confirmExecution(CUTOFF, 420, 25, 4, null, calibration, CLOCK);
        var projection = prescription.snapshot().executionConfirmation().caliberProjection();
        // Residual 6.3 fruits/shoot: W = 10 x (6.3/6)^-0.6 = 9.71 g -> 103.0 fruits/kg
        assertEquals(CaliberProjectionStatus.ESTIMATED, projection.status());
        assertEquals(103.0, projection.mostLikelyFruitsPerKg(), 0.05);
        assertEquals("101/110", projection.mostLikelySizeGrade());
        assertEquals(8, projection.calibrationObservations());
    }

    @Test
    void requiresRepresentativeSamplingToComputeTheLoadBalance() {
        var prescription = prescribedWithoutSampling(CUTOFF);
        assertThrows(IllegalStateException.class,
                () -> prescription.confirmExecution(CUTOFF, 1, 25, 1, null, CLOCK));
        assertNull(prescription.snapshot().executionConfirmation());
        assertTrue(prescription.domainEvents().isEmpty());
    }

    @Test
    void fullRemovalLeavesNoCaliberToProject() {
        var prescription = prescribed(CUTOFF);
        prescription.confirmExecution(CUTOFF, 900, 100, 4, null,
                CaliberModelFittingService.calibrate("SEVILLANA", exactObservations("SEVILLANA")), CLOCK);
        var confirmation = prescription.snapshot().executionConfirmation();
        assertEquals(0.0, confirmation.loadBalance().residualFruitsPerShoot());
        assertEquals(CaliberProjectionStatus.NOT_APPLICABLE, confirmation.caliberProjection().status());
    }

    @ParameterizedTest
    @EnumSource(value = PrescriptionStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PRESCRIBED")
    void rejectsNonPrescribedStates(PrescriptionStatus status) {
        var prescription = withStatus(status, CUTOFF);
        assertThrows(IllegalStateException.class,
                () -> prescription.confirmExecution(CUTOFF, 1, 25, 1, null, CLOCK));
        assertEquals(status, prescription.snapshot().status());
        assertTrue(prescription.domainEvents().isEmpty());
    }

    @Test
    void rejectsSecondConfirmationWithoutReplacingEvidence() {
        var prescription = prescribed(CUTOFF);
        prescription.confirmExecution(CUTOFF, 1, 25, 1, null, CLOCK);
        var original = prescription.snapshot().executionConfirmation();
        assertThrows(IllegalStateException.class,
                () -> prescription.confirmExecution(CUTOFF, 2, 50, 2, "changed", CLOCK));
        assertEquals(original, prescription.snapshot().executionConfirmation());
        assertEquals(1, prescription.domainEvents().size());
    }

    @Test
    void requiresWindowWithoutGuessingOne() {
        var prescription = prescribed(null);
        assertThrows(IllegalStateException.class,
                () -> prescription.confirmExecution(CUTOFF, 1, 25, 1, null, CLOCK));
        assertNull(prescription.snapshot().executionConfirmation());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "2026-10-03")
    void rejectsMissingOrFutureDate(String date) {
        var prescription = prescribed(CUTOFF);
        assertThrows(IllegalArgumentException.class, () -> prescription.confirmExecution(
                date == null ? null : LocalDate.parse(date), 1, 25, 1, null, CLOCK));
        assertEquals(PrescriptionStatus.PRESCRIBED, prescription.snapshot().status());
        assertTrue(prescription.domainEvents().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, 101, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidPercentages(double percentage) {
        assertThrows(IllegalArgumentException.class, () -> new RemovedBiomass(1.0, percentage));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidBiomass(double kg) {
        assertThrows(IllegalArgumentException.class, () -> new RemovedBiomass(kg, 25.0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 100})
    void acceptsPercentageBoundaries(double percentage) {
        var prescription = prescribed(CUTOFF);
        prescription.confirmExecution(CUTOFF, 0, percentage, 1, null, CLOCK);
        assertEquals(percentage, prescription.snapshot().executionConfirmation().actualRemovalPercentage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void requiresPositiveCrew(Integer crew) {
        assertThrows(IllegalArgumentException.class, () -> new LaborCrewSize(crew));
    }
}
