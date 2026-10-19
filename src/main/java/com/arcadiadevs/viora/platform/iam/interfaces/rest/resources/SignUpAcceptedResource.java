package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

/**
 * Response DTO returned when a sign-up is accepted: the account is pending until its email address is verified, so
 * no session is issued yet.
 *
 * @param userId identifier of the pending account
 * @param email  the email address the verification code was sent to
 * @param status the account status, {@code PENDING_VERIFICATION}
 */
public record SignUpAcceptedResource(
        String userId,
        String email,
        String status
) {
}
