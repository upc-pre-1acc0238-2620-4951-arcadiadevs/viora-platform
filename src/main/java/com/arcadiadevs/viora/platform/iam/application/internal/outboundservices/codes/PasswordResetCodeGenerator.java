package com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.codes;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordResetCode;

/**
 * Outbound application port producing fresh password recovery codes.
 *
 * <p>Keeps the source of randomness in infrastructure, so tests can supply a known code.</p>
 */
public interface PasswordResetCodeGenerator {

    /**
     * Generates a new random recovery code.
     *
     * @return a fresh 6-digit code
     */
    PasswordResetCode generate();
}
