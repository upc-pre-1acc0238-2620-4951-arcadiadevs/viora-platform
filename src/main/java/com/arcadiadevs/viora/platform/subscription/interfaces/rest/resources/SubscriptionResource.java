package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Public representation of a subscription.
 *
 * @param id                the subscription identifier
 * @param mode              {@code PLAN_PRODUCTOR_INDIVIDUAL} or {@code PLAN_COOPERATIVO_PATROCINADO}
 * @param status            the lifecycle status
 * @param quotaHa           the hectares covered
 * @param startsAt          when it comes into force
 * @param endsAt            when it ends, exclusive
 * @param entitlementActive whether the producer can use Viora now
 * @param cooperativeId     the sponsoring cooperative; null for an individual plan
 * @param cooperativeName   the name of the sponsoring cooperative; null for an individual plan
 */
@Schema(name = "SubscriptionResource", description = "Subscription of a producer")
public record SubscriptionResource(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") String id,
        @Schema(example = "PLAN_COOPERATIVO_PATROCINADO") String mode,
        @Schema(example = "ACTIVE") String status,
        @Schema(example = "12.00") BigDecimal quotaHa,
        Instant startsAt,
        Instant endsAt,
        boolean entitlementActive,
        String cooperativeId,
        @Schema(example = "Coop. Olivarera La Yarada") String cooperativeName
) {
}
