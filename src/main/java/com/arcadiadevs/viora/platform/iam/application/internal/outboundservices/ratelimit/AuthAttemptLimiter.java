package com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.ratelimit;

import java.time.Instant;
import java.util.OptionalLong;

/**
 * Outbound application port limiting how often an account can be tried or emailed, so its password cannot be
 * guessed online and its inbox cannot be flooded with reset links.
 *
 * <p>Keys are the normalized email addresses: the limit follows the account, whatever address the requests come
 * from.</p>
 */
public interface AuthAttemptLimiter {

    /**
     * Reserves a sign-in attempt for an email. A successful sign-in calls {@link #resetSignIn} afterwards, so only
     * failed attempts accumulate.
     *
     * @param email the email being signed in
     * @param at    the instant of the attempt
     * @return empty when the attempt may go on, or the seconds to wait when the email has too many recent failures
     */
    OptionalLong tryAcquireSignIn(String email, Instant at);

    /**
     * Forgets the failed sign-in attempts of an email after it signed in.
     *
     * @param email the email that signed in
     */
    void resetSignIn(String email);

    /**
     * Reserves the sending of a password reset link to an email.
     *
     * @param email the email the link would be sent to
     * @param at    the instant of the request
     * @return true when a link may be sent; false when that email already received the most links allowed recently
     */
    boolean tryAcquirePasswordReset(String email, Instant at);
}
