package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of one annual harvest settlement.
 *
 * @param settlementId UUID string
 */
public record SettlementId(String settlementId) {

    public SettlementId {
        if (settlementId == null || settlementId.isBlank()) {
            throw new IllegalArgumentException("settlement.id.null_or_empty");
        }
        try {
            settlementId = UUID.fromString(settlementId.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("settlement.id.invalid_uuid", e);
        }
    }

    public SettlementId() {
        this(UUID.randomUUID().toString());
    }
}
