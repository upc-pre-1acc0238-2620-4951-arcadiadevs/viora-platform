package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for removing the sessions that ended (expired or revoked) longer ago than their retention.
 *
 * <p>It carries no data: the retention is {@code UserSession#RETENTION_AFTER_END}.</p>
 */
public record PurgeEndedUserSessionsCommand() {
}
