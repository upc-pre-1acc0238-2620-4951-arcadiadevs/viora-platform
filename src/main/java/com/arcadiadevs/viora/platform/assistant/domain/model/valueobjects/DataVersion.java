package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * Fingerprint of the figures an answer was written from. Two reads of the same data give the same version, so an
 * answer is up to date exactly while the version it kept equals the current one.
 *
 * @param value {@code sha256:} followed by the hexadecimal digest
 */
public record DataVersion(String value) {

    private static final String PREFIX = "sha256:";

    /**
     * Compact constructor validating the format.
     */
    public DataVersion {
        if (value == null || !value.startsWith(PREFIX) || value.length() <= PREFIX.length()) {
            throw new IllegalArgumentException("assistant.data_version.invalid");
        }
    }
}
