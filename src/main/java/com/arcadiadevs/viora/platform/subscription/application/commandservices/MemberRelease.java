package com.arcadiadevs.viora.platform.subscription.application.commandservices;

import java.time.Instant;

/**
 * What releasing a member from a license did.
 *
 * @param memberId       the member
 * @param readingEndedAt when the manager stopped reading the member's data
 * @param planEndsAt     when the member's sponsored plan ends
 * @param seatFreesAt    when the seat and hectares go back to the license
 */
public record MemberRelease(String memberId, Instant readingEndedAt, Instant planEndsAt, Instant seatFreesAt) {
}
