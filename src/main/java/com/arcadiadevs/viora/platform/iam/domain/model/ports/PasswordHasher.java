package com.arcadiadevs.viora.platform.iam.domain.model.ports;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.NewPassword;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;

/**
 * Domain port for hashing passwords and checking a plain password against a stored hash.
 *
 * <p>Implemented by an infrastructure adapter so the hashing algorithm stays out of the domain core.
 * {@code UserAccount} receives it when changing or recovering the password, which keeps the current password check
 * and the password history rule inside the aggregate.</p>
 */
public interface PasswordHasher {

    /**
     * Hashes a new password for storage.
     *
     * @param password the new plain password
     * @return the encoded hash
     */
    PasswordHash hash(NewPassword password);

    /**
     * Checks a plain password against a stored hash.
     *
     * @param plainPassword the plain password
     * @param hash          the stored hash
     * @return true if the password matches the hash; false otherwise
     */
    boolean matches(String plainPassword, PasswordHash hash);
}
