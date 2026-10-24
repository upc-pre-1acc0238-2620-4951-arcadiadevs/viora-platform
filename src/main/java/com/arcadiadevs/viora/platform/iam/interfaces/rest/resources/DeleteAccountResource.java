package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of an account deletion (P100): the current password, typed again to confirm.
 *
 * @param password the current password
 */
public record DeleteAccountResource(
        @NotBlank(message = "Password cannot be blank")
        @Schema(description = "Current password of the account", example = "Viora-2026")
        String password
) {
}
