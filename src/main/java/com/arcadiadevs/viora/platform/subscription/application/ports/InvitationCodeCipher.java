package com.arcadiadevs.viora.platform.subscription.application.ports;

/**
 * Port keeping an invitation code encrypted while it can be redeemed, so the manager can share it again without the
 * database ever holding it in clear.
 */
public interface InvitationCodeCipher {

    /**
     * Tells whether a key is configured; without one, codes cannot be issued.
     *
     * @return true if codes can be encrypted and decrypted
     */
    boolean isAvailable();

    /**
     * Encrypts a code.
     *
     * @param code the normalized code
     * @return the sealed code, safe to store
     * @throws IllegalStateException if no key is configured
     */
    String encrypt(String code);

    /**
     * Decrypts a code sealed by {@link #encrypt}.
     *
     * @param sealedCode the sealed code
     * @return the code
     * @throws IllegalStateException if no key is configured or the sealed code cannot be read with it
     */
    String decrypt(String sealedCode);
}
