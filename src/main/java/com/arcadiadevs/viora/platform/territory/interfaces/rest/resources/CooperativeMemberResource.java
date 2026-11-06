package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A member in the manager's roll. Who does not share shows only their name and state.
 *
 * @param id               the member, their user identifier
 * @param fullName         their name
 * @param sharing          {@code SHARING}, {@code WITHDRAWN} or {@code NOT_SHARING}
 * @param withdrawnAt      when they stopped sharing; null unless withdrawn
 * @param plotCount        their active plots; null unless sharing
 * @param registeredAreaHa the hectares of those plots; null unless sharing
 * @param sectorNames      the sectors of those plots
 * @param worstRiskLevel   the color of their most urgent plot
 * @param worstReason      the first reason of that plot
 */
@Schema(name = "CooperativeMemberResource", description = "Member of the manager's license")
public record CooperativeMemberResource(
        String id,
        @Schema(example = "Teodoro Mamani") String fullName,
        @Schema(example = "SHARING") String sharing,
        Instant withdrawnAt,
        @Schema(example = "2") Integer plotCount,
        @Schema(example = "4.00") BigDecimal registeredAreaHa,
        List<String> sectorNames,
        @Schema(example = "CRITICAL") String worstRiskLevel,
        RiskReasonResource worstReason
) {
}
