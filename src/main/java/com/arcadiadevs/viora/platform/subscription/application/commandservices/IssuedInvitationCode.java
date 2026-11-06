package com.arcadiadevs.viora.platform.subscription.application.commandservices;

/**
 * A new code of an {@link InvitationCodeBatch}.
 *
 * @param id   the code identifier
 * @param code the code in full
 */
public record IssuedInvitationCode(String id, String code) {
}
