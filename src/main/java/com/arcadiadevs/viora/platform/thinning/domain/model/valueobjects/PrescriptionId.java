package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value Object representing the domain identifier of a Fruit Thinning Prescription aggregate root.
 *
 * @param prescriptionId the string UUID value
 */
public record PrescriptionId(String prescriptionId) {

    /**
     * Compact constructor validating that prescriptionId is a non-null valid UUID.
     */
    public PrescriptionId {
        if (prescriptionId == null || prescriptionId.trim().isEmpty()) {
            throw new IllegalArgumentException("thinning.prescription.id.null_or_empty");
        }
        try {
            prescriptionId = UUID.fromString(prescriptionId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("thinning.prescription.id.invalid_uuid", e);
        }
    }

    /**
     * Constructs a new randomly generated PrescriptionId.
     */
    public PrescriptionId() {
        this(UUID.randomUUID().toString());
    }
}
