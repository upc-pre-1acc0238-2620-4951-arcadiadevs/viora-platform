package com.arcadiadevs.viora.platform.iam.domain.exceptions;

import java.util.List;

/**
 * Domain exception thrown when a new password breaks one or more password rules.
 *
 * <p>It carries every broken rule at once, so the client can mark each requirement instead of fixing them one by
 * one. It extends {@link IllegalArgumentException} because a weak password is invalid input.</p>
 */
public class WeakPasswordException extends IllegalArgumentException {

    private final List<String> violations;

    /**
     * Constructs the exception with the i18n keys of the broken rules.
     *
     * @param violations the broken rules, never empty
     */
    public WeakPasswordException(List<String> violations) {
        super("user.password.weak");
        this.violations = List.copyOf(violations);
    }

    /**
     * Returns the i18n keys of the broken password rules.
     *
     * @return the broken rules
     */
    public List<String> violations() {
        return violations;
    }
}
