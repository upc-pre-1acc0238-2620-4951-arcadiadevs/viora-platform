package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * What releasing a member from the license did.
 *
 * @param memberId       the member
 * @param readingEndedAt when the manager stopped reading the member's data
 * @param planEndsAt     when the member's sponsored plan ends
 * @param seatFreesAt    when the seat and hectares go back to the license
 */
@Schema(name = "MemberReleaseResource", description = "Release of a member from the license")
public record MemberReleaseResource(String memberId, Instant readingEndedAt, Instant planEndsAt,
                                    Instant seatFreesAt) {
}
