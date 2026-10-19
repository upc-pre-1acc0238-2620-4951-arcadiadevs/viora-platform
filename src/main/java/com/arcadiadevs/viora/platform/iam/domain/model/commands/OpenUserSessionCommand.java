package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for opening a session for an account that was just verified.
 *
 * @param userId the account identifier
 */
public record OpenUserSessionCommand(String userId) {
    public OpenUserSessionCommand {
        if (userId == null) {
            throw new IllegalArgumentException("user.command.user_id.null");
        }
    }
}
