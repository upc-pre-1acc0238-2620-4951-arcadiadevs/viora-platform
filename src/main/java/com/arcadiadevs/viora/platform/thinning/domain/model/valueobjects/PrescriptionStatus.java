package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Lifecycle status of a Fruit Thinning Prescription aggregate root.
 */
public enum PrescriptionStatus {

    /**
     * Sampling rounds are currently being gathered; minimum statistical coverage not yet reached or confirmed.
     */
    SAMPLING_IN_PROGRESS,

    /**
     * Sustainable crop load calculated and advisory issued to the producer.
     */
    PRESCRIBED,

    /**
     * Thinning labor execution scheduled or acknowledged.
     */
    CONFIRMED,

    /**
     * Field execution completed and verified.
     */
    EXECUTED,

    /**
     * Phenological intervention window closed due to pit hardening prior to manual thinning.
     */
    EXPIRED,

    /**
     * Plot was removed or deactivated in Orchard context; pending prescriptions voided.
     */
    VOIDED_BY_PLOT_REMOVAL,

    /**
     * Prescription dismissed or deemed unfeasible by agronomic evaluation.
     */
    REJECTED
}
