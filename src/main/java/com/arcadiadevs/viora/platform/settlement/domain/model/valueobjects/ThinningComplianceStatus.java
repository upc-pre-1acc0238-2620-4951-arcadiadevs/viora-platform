package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

/**
 * Thinning evidence known by Settlement for the settled campaign.
 *
 * <p>{@link #NOT_RECORDED} is not a non-compliance: a balanced plot may need no thinning at all.</p>
 */
public enum ThinningComplianceStatus {
    EXECUTED_ON_TIME,
    EXECUTED_LATE,
    NOT_RECORDED
}
