package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/** Number of workers who performed the thinning labor. */
public record LaborCrewSize(Integer value) {
    public LaborCrewSize {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("thinning.execution.crew.invalid");
        }
    }
}
