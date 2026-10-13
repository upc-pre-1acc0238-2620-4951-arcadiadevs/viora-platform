package com.arcadiadevs.viora.platform.iam.domain.model.queries;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordResetToken;

/**
 * Domain query record for resolving a user account by an active password reset token value object.
 *
 * @param token the password reset token value object
 */
public record GetUserAccountByResetTokenQuery(PasswordResetToken token) {

    public GetUserAccountByResetTokenQuery {
        if (token == null) {
            throw new IllegalArgumentException("user.reset_token.null");
        }
    }

    /**
     * Convenience constructor creating the query from a raw token string.
     *
     * @param rawToken the raw recovery token string
     */
    public GetUserAccountByResetTokenQuery(String rawToken) {
        this(new PasswordResetToken(rawToken));
    }
}
