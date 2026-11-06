package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An invitation code as its technical manager sees it.
 *
 * @param id         the code identifier
 * @param batchId    the batch it was issued in; null for codes issued one by one
 * @param issuedAt   when it was issued
 * @param expiresAt  the last instant it can be redeemed, exclusive
 * @param hectares   the hectares it covers
 * @param status     {@code AVAILABLE}, {@code REDEEMED}, {@code EXPIRED} or {@code REVOKED}
 * @param code       the code in full; null unless it can still be redeemed
 * @param maskedCode the code with only its last two characters
 * @param redeemedBy the name of the producer who redeemed it; null unless redeemed
 * @param redeemedAt when it was redeemed; null unless redeemed
 */
@Schema(name = "InvitationCodeResource", description = "Invitation code of the manager's license")
public record InvitationCodeResource(
        String id,
        String batchId,
        Instant issuedAt,
        Instant expiresAt,
        @Schema(example = "5.00") BigDecimal hectares,
        @Schema(example = "AVAILABLE") String status,
        @Schema(example = "COOP-4F9T") String code,
        @Schema(example = "COOP-••9T") String maskedCode,
        @Schema(example = "Teodoro Mamani") String redeemedBy,
        Instant redeemedAt
) {
}
