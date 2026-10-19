package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import com.arcadiadevs.viora.platform.iam.domain.exceptions.WeakPasswordException;

import java.util.ArrayList;

/**
 * Value object representing a plain password chosen by the user, which satisfies the password rules.
 *
 * <p>The same rules apply to sign-up, password change and password recovery: at least {@link #MIN_LENGTH}
 * characters, with letters and digits. It only lives while the request is handled; the aggregate stores its
 * {@link PasswordHash}.</p>
 *
 * @param value the plain password
 */
public record NewPassword(String value) {

    /** Minimum number of characters. */
    public static final int MIN_LENGTH = 8;

    /**
     * Compact constructor checking every password rule.
     *
     * @throws IllegalArgumentException if the password is null
     * @throws WeakPasswordException    if it breaks one or more rules, listing all of them
     */
    public NewPassword {
        if (value == null) {
            throw new IllegalArgumentException("user.password.null");
        }
        var violations = new ArrayList<String>();
        if (value.length() < MIN_LENGTH) {
            violations.add("user.password.min_length");
        }
        if (value.chars().noneMatch(Character::isLetter) || value.chars().noneMatch(Character::isDigit)) {
            violations.add("user.password.letters_and_digits");
        }
        if (!violations.isEmpty()) {
            throw new WeakPasswordException(violations);
        }
    }

    /**
     * Hides the password, so it never reaches a log by accident.
     *
     * @return a fixed mask
     */
    @Override
    public String toString() {
        return "NewPassword[****]";
    }
}
