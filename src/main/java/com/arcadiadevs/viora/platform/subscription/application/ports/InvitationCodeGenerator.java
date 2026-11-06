package com.arcadiadevs.viora.platform.subscription.application.ports;

/**
 * Port drawing new invitation codes at random. Whether a code was ever issued is checked by the caller.
 */
public interface InvitationCodeGenerator {

    /**
     * Draws a code.
     *
     * @return a code as {@code COOP-} followed by four letters or digits
     */
    String next();
}
