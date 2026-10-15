package com.arcadiadevs.viora.platform.iam.infrastructure.hashing.argon2;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.hashing.HashingService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Interface combining {@link HashingService} and Spring Security's {@link PasswordEncoder} for Argon2id.
 */
public interface Argon2HashingService extends HashingService, PasswordEncoder {
}
