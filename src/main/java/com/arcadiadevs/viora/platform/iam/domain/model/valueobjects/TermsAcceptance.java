package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import java.time.Instant;

/**
 * Value object recording which version of the terms and privacy policy a user accepted, and when.
 *
 * @param version    the accepted terms version
 * @param acceptedAt the instant the terms were accepted
 */
public record TermsAcceptance(String version, Instant acceptedAt) {

    /**
     * Compact constructor validating the acceptance.
     *
     * @throws IllegalArgumentException if the version is blank or the instant is null
     */
    public TermsAcceptance {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("user.terms.version.null_or_empty");
        }
        if (acceptedAt == null) {
            throw new IllegalArgumentException("user.terms.accepted_at.null");
        }
        version = version.trim();
    }
}
