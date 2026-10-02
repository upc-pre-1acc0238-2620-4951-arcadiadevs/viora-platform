package com.arcadiadevs.viora.platform.thinning.domain.model.commands;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.*;
import java.time.LocalDate;

/** Validated instruction to record field execution of a prescribed thinning labor. */
public record ConfirmThinningExecutionCommand(String prescriptionId, LocalDate executedDate,
        Double actualRemovalPercentage, Double removedKg, Integer laborCrewSize, String notes) {
    public ConfirmThinningExecutionCommand {
        prescriptionId = new PrescriptionId(prescriptionId).prescriptionId();
        if (executedDate == null) {
            throw new IllegalArgumentException("thinning.execution.date.invalid");
        }
        new RemovedBiomass(removedKg, actualRemovalPercentage);
        new LaborCrewSize(laborCrewSize);
        if (notes != null && notes.length() > 2000) {
            throw new IllegalArgumentException("thinning.execution.notes.too_long");
        }
    }
}
