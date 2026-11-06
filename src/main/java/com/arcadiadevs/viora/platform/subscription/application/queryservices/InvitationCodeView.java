package com.arcadiadevs.viora.platform.subscription.application.queryservices;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read model of an invitation code as its technical manager sees it: in full while it can be redeemed, masked once it
 * was redeemed, expired or revoked, and with the name of the producer who redeemed it.
 * <p>It is not part of the domain: it combines the code, decrypted for the manager, with the name the Profiles context
 * keeps, only to answer the codes screen.</p>
 *
 * @param id         the code identifier
 * @param batchId    the batch it was issued in; null for codes issued one by one
 * @param issuedAt   when it was issued
 * @param expiresAt  the last instant it can be redeemed, exclusive
 * @param hectares   the hectares it covers
 * @param status     {@code AVAILABLE}, {@code REDEEMED}, {@code EXPIRED} or {@code REVOKED}
 * @param code       the code in full; null unless it can still be redeemed
 * @param maskedCode the code with only its last two characters, such as {@code COOP-••9T}
 * @param redeemedBy the name of the producer who redeemed it; null unless redeemed
 * @param redeemedAt when it was redeemed; null unless redeemed
 */
public record InvitationCodeView(String id, String batchId, Instant issuedAt, Instant expiresAt, BigDecimal hectares,
                                 String status, String code, String maskedCode, String redeemedBy,
                                 Instant redeemedAt) {
}
