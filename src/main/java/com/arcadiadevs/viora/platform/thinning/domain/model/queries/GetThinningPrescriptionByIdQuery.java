package com.arcadiadevs.viora.platform.thinning.domain.model.queries;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionId;

/**
 * Domain query requesting one thinning prescription by its identifier.
 *
 * @param prescriptionId prescription identifier
 */
public record GetThinningPrescriptionByIdQuery(PrescriptionId prescriptionId) {

    public GetThinningPrescriptionByIdQuery {
        if (prescriptionId == null) {
            throw new IllegalArgumentException("thinning.prescription_id.null");
        }
    }
}
