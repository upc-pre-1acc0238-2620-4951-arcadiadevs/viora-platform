package com.arcadiadevs.viora.platform.iam.infrastructure.hashing.argon2.services;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.PasswordHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.NewPassword;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter hashing and checking passwords with the Argon2id encoder.
 */
@Component
public class Argon2PasswordHasher implements PasswordHasher {

    private final HashingService hashingService;

    /**
     * Creates the adapter.
     *
     * @param hashingService the Argon2id hashing service
     * @throws IllegalArgumentException if the hashing service is null
     */
    public Argon2PasswordHasher(HashingService hashingService) {
        if (hashingService == null) {
            throw new IllegalArgumentException("hashing.service.null");
        }
        this.hashingService = hashingService;
    }

    @Override
    public PasswordHash hash(NewPassword password) {
        return new PasswordHash(hashingService.encode(password.value()));
    }

    @Override
    public boolean matches(String plainPassword, PasswordHash hash) {
        return hashingService.matches(plainPassword, hash.value());
    }
}
