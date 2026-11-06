package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A batch of new invitation codes, in full, ready to share.
 *
 * @param batchId         the batch identifier
 * @param issuedAt        when the batch was issued
 * @param expiresAt       the last instant its codes can be redeemed, exclusive
 * @param hectaresPerCode the hectares each code covers
 * @param codes           the new codes
 */
@Schema(name = "InvitationCodeBatchResource", description = "New invitation codes, ready to share")
public record InvitationCodeBatchResource(
        String batchId,
        Instant issuedAt,
        Instant expiresAt,
        @Schema(example = "5.00") BigDecimal hectaresPerCode,
        List<IssuedCodeResource> codes
) {

    /**
     * A new code of the batch.
     *
     * @param id   the code identifier
     * @param code the code in full
     */
    @Schema(name = "IssuedInvitationCodeResource")
    public record IssuedCodeResource(String id, @Schema(example = "COOP-3TQ8") String code) {
    }
}
