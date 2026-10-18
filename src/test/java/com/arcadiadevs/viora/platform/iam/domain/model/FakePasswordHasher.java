package com.arcadiadevs.viora.platform.iam.domain.model;

import com.arcadiadevs.viora.platform.iam.domain.model.ports.PasswordHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.NewPassword;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;

/** Deterministic stand-in for the Argon2 adapter: the domain only needs hash and matches semantics. */
public final class FakePasswordHasher implements PasswordHasher {

    /**
     * Returns the hash this fake produces for a plain password.
     *
     * @param plainPassword the plain password
     * @return its fake hash
     */
    public static PasswordHash hashOf(String plainPassword) {
        return new PasswordHash("pw:" + plainPassword);
    }

    @Override
    public PasswordHash hash(NewPassword password) {
        return hashOf(password.value());
    }

    @Override
    public boolean matches(String plainPassword, PasswordHash hash) {
        return hashOf(plainPassword).equals(hash);
    }
}
