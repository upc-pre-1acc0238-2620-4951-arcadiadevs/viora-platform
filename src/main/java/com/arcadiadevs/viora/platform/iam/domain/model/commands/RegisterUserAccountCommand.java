package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for registering a new user account.
 *
 * <p>The three profile fields are optional onboarding details: they are either all present or all null.</p>
 *
 * @param email        raw email string
 * @param passwordHash derived password hash string
 * @param role         target role string
 * @param fullName     optional full civil name captured at sign-up
 * @param country      optional country ISO alpha-2 code captured at sign-up
 * @param phoneNumber  optional telephone number captured at sign-up
 */
public record RegisterUserAccountCommand(
        String email,
        String passwordHash,
        String role,
        String fullName,
        String country,
        String phoneNumber
) {
    public RegisterUserAccountCommand {
        if (email == null) {
            throw new IllegalArgumentException("user.command.email.null");
        }
        if (passwordHash == null) {
            throw new IllegalArgumentException("user.command.password_hash.null");
        }
        if (role == null) {
            throw new IllegalArgumentException("user.command.role.null");
        }
    }

    /**
     * Creates a registration command without onboarding profile details.
     *
     * @param email        raw email string
     * @param passwordHash derived password hash string
     * @param role         target role string
     */
    public RegisterUserAccountCommand(String email, String passwordHash, String role) {
        this(email, passwordHash, role, null, null, null);
    }

    /**
     * Checks whether the command carries onboarding profile details.
     *
     * @return true if any profile field is present
     */
    public boolean hasProfileDetails() {
        return fullName != null || country != null || phoneNumber != null;
    }
}
