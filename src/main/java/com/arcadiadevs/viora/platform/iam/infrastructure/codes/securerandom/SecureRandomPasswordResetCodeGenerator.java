package com.arcadiadevs.viora.platform.iam.infrastructure.codes.securerandom;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.codes.PasswordResetCodeGenerator;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordResetCode;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Infrastructure adapter generating 6-digit recovery codes from a cryptographically strong random source.
 */
@Component
public class SecureRandomPasswordResetCodeGenerator implements PasswordResetCodeGenerator {

    private static final int CODE_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public PasswordResetCode generate() {
        return new PasswordResetCode(String.format(Locale.ROOT, "%06d", secureRandom.nextInt(CODE_BOUND)));
    }
}
