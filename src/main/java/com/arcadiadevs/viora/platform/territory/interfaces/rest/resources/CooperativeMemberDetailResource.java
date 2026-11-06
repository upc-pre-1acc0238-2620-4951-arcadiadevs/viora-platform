package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * One member as the manager reads them. Contact and plots only while they share.
 *
 * @param id               the member, their user identifier
 * @param fullName         their name
 * @param sharing          {@code SHARING}, {@code WITHDRAWN} or {@code NOT_SHARING}
 * @param withdrawnAt      when they stopped sharing; null unless withdrawn
 * @param memberSince      when their sponsored plan started
 * @param quotaHa          the hectares their code covers
 * @param maskedCode       the code they redeemed, masked
 * @param phone            their phone; null unless sharing
 * @param email            their email; null unless sharing
 * @param registeredAreaHa the hectares of their active plots; null unless sharing
 * @param plots            their plots with sector and color; empty unless sharing
 */
@Schema(name = "CooperativeMemberDetailResource", description = "Member of the manager's license, in detail")
public record CooperativeMemberDetailResource(
        String id,
        @Schema(example = "Teodoro Mamani") String fullName,
        @Schema(example = "SHARING") String sharing,
        Instant withdrawnAt,
        Instant memberSince,
        @Schema(example = "8.00") BigDecimal quotaHa,
        @Schema(example = "COOP-••2M") String maskedCode,
        @Schema(example = "+51952184337") String phone,
        @Schema(example = "teodoro.mamani@gmail.com") String email,
        @Schema(example = "4.00") BigDecimal registeredAreaHa,
        List<MemberPlotResource> plots
) {
}
