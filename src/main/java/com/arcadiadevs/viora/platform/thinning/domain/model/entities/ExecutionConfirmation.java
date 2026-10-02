package com.arcadiadevs.viora.platform.thinning.domain.model.entities;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.ExecutionConfirmationSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.thinning.domain.services.CaliberProjectionService;
import java.time.Clock;
import java.time.LocalDate;

/** Immutable execution evidence belonging to a single thinning prescription. */
public final class ExecutionConfirmation {
    private final ExecutionConfirmationSnapshot state;

    private ExecutionConfirmation(ExecutionConfirmationSnapshot state) {
        this.state = state;
    }

    public static ExecutionConfirmation create(LocalDate executionDate, RemovedBiomass biomass,
            LaborCrewSize crew, String notes, LocalDate windowClosesOn, LoadBalance loadBalance,
            CaliberCalibration calibration, Clock clock) {
        if (executionDate == null || executionDate.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("thinning.execution.date.invalid");
        }
        if (notes != null && notes.length() > 2000) {
            throw new IllegalArgumentException("thinning.execution.notes.too_long");
        }
        if (windowClosesOn == null) {
            throw new IllegalStateException("thinning.execution.window.missing");
        }
        if (loadBalance == null) {
            throw new IllegalArgumentException("thinning.load_balance.load.invalid");
        }
        var timeliness = executionDate.isAfter(windowClosesOn)
                ? ExecutionTimeliness.LATE : ExecutionTimeliness.OPTIMAL;
        var projection = CaliberProjectionService.project(loadBalance, timeliness, calibration);
        return new ExecutionConfirmation(new ExecutionConfirmationSnapshot(new ConfirmationId(),
                executionDate, biomass.actualRemovalPercentage(), crew.value(), timeliness,
                clock.instant(), biomass.removedKg(), notes, loadBalance, projection));
    }

    public boolean isOpportune() {
        return state.timeliness() == ExecutionTimeliness.OPTIMAL;
    }

    public ExecutionConfirmationSnapshot snapshot() {
        return state;
    }
}
