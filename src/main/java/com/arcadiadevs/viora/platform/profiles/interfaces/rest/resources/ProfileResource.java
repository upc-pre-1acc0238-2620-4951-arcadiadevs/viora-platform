package com.arcadiadevs.viora.platform.profiles.interfaces.rest.resources;

import java.time.Instant;

/**
 * Public response DTO exposing profile attributes.
 *
 * @param id profile identifier
 * @param userId owner user account identifier
 * @param fullName full name
 * @param country ISO alpha-2 country code
 * @param phoneNumber normalized E.164 phone number
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record ProfileResource(
        String id,
        String userId,
        String fullName,
        String country,
        String phoneNumber,
        Instant createdAt,
        Instant updatedAt
) {
}
