package com.arcadiadevs.viora.platform.iam.infrastructure.codes.securerandom;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.codes.VerificationCodeGenerator;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.VerificationCode;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Infrastructure adapter generating 6-digit verification codes from a cryptographically strong random source.
 */
@Component
public class SecureRandomVerificationCodeGenerator implements VerificationCodeGenerator {

    private static final int CODE_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public VerificationCode generate() {
        return new VerificationCode(String.format(Locale.ROOT, "%06d", secureRandom.nextInt(CODE_BOUND)));
    }
}
