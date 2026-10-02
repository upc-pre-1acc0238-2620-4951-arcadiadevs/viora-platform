package com.arcadiadevs.viora.platform.settlement.domain.model.events;

import java.time.Instant;

/**
 * Published when a campaign harvest is settled. Scalar payload only (published language).
 *
 * <p>Consumers: Thinning registers the caliber calibration observation when {@code commercialFruitsPerKg}
 * is present; Phenology is the planned consumer for the historical series.</p>
 */
public record CampaignHarvestSettledEvent(
        String eventId,
        String reportId,
        String settlementId,
        String plotId,
        Integer campaignYear,
        Double greenOlivesKg,
        Double blackOlivesKg,
        Double totalYieldKg,
        Double commercialFruitsPerKg,
        Instant occurredOn
) {
}
