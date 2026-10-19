package com.arcadiadevs.viora.platform.profiles.domain.model.commands;

/**
 * Domain command for removing the profile of a user account.
 *
 * @param userId the user identity reference
 */
public record DeleteProfileCommand(String userId) {
    public DeleteProfileCommand {
        if (userId == null) {
            throw new IllegalArgumentException("profile.command.user_id.null");
        }
    }
}
