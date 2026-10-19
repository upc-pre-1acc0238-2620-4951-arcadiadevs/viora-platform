package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for discarding the pending accounts abandoned before verifying their email.
 *
 * <p>It carries no data: which accounts are abandoned is decided by {@code UserAccount#isAbandonedUnverified}.</p>
 */
public record DiscardUnverifiedUserAccountsCommand() {
}
