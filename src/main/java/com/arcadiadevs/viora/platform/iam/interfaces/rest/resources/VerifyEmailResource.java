package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Resource payload for verifying the email address of a pending account.
 *
 * @param email the email address the code was sent to
 * @param code  the 6-digit verification code
 */
public record VerifyEmailResource(
        @NotBlank(message = "user.email.blank")
        @Email(message = "user.email.invalid")
        String email,
        @NotBlank(message = "user.email_verification.code.blank")
        String code
) {
}
