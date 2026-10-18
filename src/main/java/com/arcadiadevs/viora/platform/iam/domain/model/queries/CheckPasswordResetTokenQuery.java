package com.arcadiadevs.viora.platform.iam.domain.model.queries;

/**
 * Domain query checking whether a password reset link can still be used, before asking for the new password.
 *
 * @param resetToken the token carried by the reset link
 */
public record CheckPasswordResetTokenQuery(String resetToken) {
    public CheckPasswordResetTokenQuery {
        if (resetToken == null) {
            throw new IllegalArgumentException("user.reset_token.null");
        }
    }
}
