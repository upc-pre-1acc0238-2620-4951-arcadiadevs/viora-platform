package com.arcadiadevs.viora.platform.iam.infrastructure.hashing.argon2.services;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.PasswordResetCodeHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordResetCode;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter hashing password recovery codes with the same Argon2id encoder used for passwords.
 *
 * <p>A slow salted hash matters here because a 6-digit code has only a million possible values.</p>
 */
@Component
public class Argon2PasswordResetCodeHasher implements PasswordResetCodeHasher {

    private final HashingService hashingService;

    /**
     * Creates the adapter.
     *
     * @param hashingService the Argon2id hashing service
     * @throws IllegalArgumentException if the hashing service is null
     */
    public Argon2PasswordResetCodeHasher(HashingService hashingService) {
        if (hashingService == null) {
            throw new IllegalArgumentException("hashing.service.null");
        }
        this.hashingService = hashingService;
    }

    @Override
    public String hash(PasswordResetCode code) {
        return hashingService.encode(code.value());
    }

    @Override
    public boolean matches(PasswordResetCode code, String codeHash) {
        return hashingService.matches(code.value(), codeHash);
    }
}
