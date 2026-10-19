package com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.codes;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.VerificationCode;

/**
 * Outbound application port producing fresh one-time verification codes.
 *
 * <p>Keeps the source of randomness in infrastructure, so tests can supply a known code.</p>
 */
public interface VerificationCodeGenerator {

    /**
     * Generates a new random verification code.
     *
     * @return a fresh 6-digit code
     */
    VerificationCode generate();
}
