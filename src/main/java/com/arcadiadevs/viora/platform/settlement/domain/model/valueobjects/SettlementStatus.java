package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

/**
 * Lifecycle of an annual settlement. A settled campaign is immutable; {@link #AUDITED} is reserved for the
 * professional certification of the campaign (TS40).
 */
public enum SettlementStatus {
    SETTLED,
    AUDITED
}
