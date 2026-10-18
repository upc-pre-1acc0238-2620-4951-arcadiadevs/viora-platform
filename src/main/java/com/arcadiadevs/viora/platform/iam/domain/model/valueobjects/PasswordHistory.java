package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Value object holding the hashes of the passwords an account used before its current one, newest first.
 *
 * <p>Together with the current password it covers the last {@link #REMEMBERED_PASSWORDS} passwords, which a new
 * password must not repeat.</p>
 *
 * @param previousHashes hashes of the previous passwords, newest first
 */
public record PasswordHistory(List<PasswordHash> previousHashes) {

    /** Passwords a new one is checked against, the current one included. */
    public static final int REMEMBERED_PASSWORDS = 5;

    private static final int MAX_PREVIOUS = REMEMBERED_PASSWORDS - 1;

    /**
     * Compact constructor keeping an immutable copy limited to the remembered passwords.
     *
     * @throws IllegalArgumentException if the list or any hash is null
     */
    public PasswordHistory {
        if (previousHashes == null || previousHashes.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("user.password_history.null");
        }
        previousHashes = List.copyOf(previousHashes.subList(0, Math.min(previousHashes.size(), MAX_PREVIOUS)));
    }

    /**
     * Creates an empty history, for an account that never changed its password.
     *
     * @return an empty history
     */
    public static PasswordHistory empty() {
        return new PasswordHistory(List.of());
    }

    /**
     * Returns a history with the given hash added as the newest one, dropping the oldest beyond the limit.
     *
     * @param replacedHash the hash of the password being replaced
     * @return the updated history
     */
    public PasswordHistory remember(PasswordHash replacedHash) {
        var hashes = new ArrayList<PasswordHash>();
        hashes.add(replacedHash);
        hashes.addAll(previousHashes);
        return new PasswordHistory(hashes);
    }
}
