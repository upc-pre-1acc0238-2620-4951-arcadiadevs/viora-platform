package com.arcadiadevs.viora.platform.iam.domain.model.ports;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordResetCode;

/**
 * Domain port for hashing and verifying password recovery codes.
 *
 * <p>Implemented by an infrastructure adapter so the hashing algorithm stays out of the domain core, the same way
 * passwords are hashed outside it. {@code UserAccount} receives this port when consuming a code, which keeps the
 * failed attempt rule inside the aggregate even though the hash is salted and cannot be compared by equality.</p>
 */
public interface PasswordResetCodeHasher {

    /**
     * Hashes a recovery code for storage.
     *
     * @param code the plain recovery code
     * @return the encoded hash
     */
    String hash(PasswordResetCode code);

    /**
     * Checks a submitted recovery code against a stored hash.
     *
     * @param code     the code submitted by the user
     * @param codeHash the stored hash
     * @return true if the code matches the hash; false otherwise
     */
    boolean matches(PasswordResetCode code, String codeHash);
}
