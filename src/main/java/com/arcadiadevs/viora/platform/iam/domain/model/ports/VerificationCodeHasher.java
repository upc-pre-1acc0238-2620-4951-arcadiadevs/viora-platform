package com.arcadiadevs.viora.platform.iam.domain.model.ports;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.VerificationCode;

/**
 * Domain port for hashing and verifying one-time verification codes.
 *
 * <p>Implemented by an infrastructure adapter so the hashing algorithm stays out of the domain core, the same way
 * passwords are hashed outside it. {@code UserAccount} receives this port when consuming a code, which keeps the
 * failed attempt rules inside the aggregate even though the hash is salted and cannot be compared by equality.</p>
 */
public interface VerificationCodeHasher {

    /**
     * Hashes a verification code for storage.
     *
     * @param code the plain verification code
     * @return the encoded hash
     */
    String hash(VerificationCode code);

    /**
     * Checks a submitted verification code against a stored hash.
     *
     * @param code     the code submitted by the user
     * @param codeHash the stored hash
     * @return true if the code matches the hash; false otherwise
     */
    boolean matches(VerificationCode code, String codeHash);
}
