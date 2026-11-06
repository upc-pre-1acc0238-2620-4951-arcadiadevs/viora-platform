package com.arcadiadevs.viora.platform.subscription.application.commandservices;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Result of issuing a batch of invitation codes: the new codes in full, as the manager shares them. Only this answer
 * and the codes screen, while each code can be redeemed, ever show them.
 *
 * @param batchId         the batch identifier
 * @param issuedAt        when the batch was issued
 * @param expiresAt       the last instant its codes can be redeemed, exclusive
 * @param hectaresPerCode the hectares each code covers
 * @param codes           the new codes
 */
public record InvitationCodeBatch(String batchId, Instant issuedAt, Instant expiresAt, BigDecimal hectaresPerCode,
                                  List<IssuedInvitationCode> codes) {
}
