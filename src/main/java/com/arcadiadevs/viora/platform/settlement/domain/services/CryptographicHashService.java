package com.arcadiadevs.viora.platform.settlement.domain.services;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.VerificationHash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Pure domain service computing the SHA-256 digest of a payload. */
public final class CryptographicHashService {

    private static final String ALGORITHM = "SHA-256";

    /**
     * Computes the SHA-256 digest of the given bytes.
     *
     * @param content bytes to digest
     * @return the digest as 64 lowercase hexadecimal characters
     * @throws IllegalArgumentException if the content is null
     * @throws IllegalStateException    if the JVM does not provide SHA-256 (every compliant JVM does)
     */
    public VerificationHash sha256(byte[] content) {
        if (content == null) {
            throw new IllegalArgumentException("settlement.certification.hash.content.null");
        }
        try {
            var digest = MessageDigest.getInstance(ALGORITHM).digest(content);
            return new VerificationHash(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("settlement.certification.hash.algorithm.unavailable", e);
        }
    }
}
