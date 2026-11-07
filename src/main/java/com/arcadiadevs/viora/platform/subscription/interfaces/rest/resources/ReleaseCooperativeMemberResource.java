package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload to release a member from the license.
 *
 * @param memberId the member's user identifier
 */
@Schema(name = "ReleaseCooperativeMemberResource", description = "Member to release from the license",
        example = "{\"memberId\": \"5f0c7b0e-8a3d-4c55-9a6e-1d2f3b4c5d6e\"}")
public record ReleaseCooperativeMemberResource(
        @NotBlank(message = "Member cannot be blank")
        @Schema(description = "The member's user identifier")
        String memberId
) {
}
