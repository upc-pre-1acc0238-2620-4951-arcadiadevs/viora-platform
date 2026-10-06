package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

/**
 * Optional client supplied key that makes settling a campaign idempotent: the same key replayed for the same plot
 * and campaign year returns the settlement already registered instead of creating a second one.
 *
 * <p>Like {@link MillTicketNumber}, the absence of a key is an instance without a value and not a {@code null}
 * reference, so {@link #isPresent()} answers whether the settlement was keyed. Only producers who integrate a
 * mobile client that retries send one; everything else settles without it and keeps the plain one-shot behaviour.</p>
 *
 * @param value the trimmed idempotency key, or null when the caller sent none
 */
public record IdempotencyKey(String value) {

    /** Longest idempotency key accepted; it is what the settlement column stores. */
    public static final int MAX_LENGTH = 64;

    /**
     * Builds an idempotency key from raw input, trimming it and treating a blank value as no key at all.
     *
     * @param raw the raw key, possibly null or blank
     * @return the key, absent when the raw value is null or blank
     * @throws IllegalArgumentException if the trimmed value is longer than {@value #MAX_LENGTH} characters
     */
    public static IdempotencyKey of(String raw) {
        return new IdempotencyKey(normalize(raw));
    }

    /**
     * Rebuilds an idempotency key from persistence.
     *
     * @param value the persisted key, or null when the settlement was not keyed
     * @throws IllegalArgumentException if the value is longer than {@value #MAX_LENGTH} characters
     */
    public IdempotencyKey {
        if (value != null && value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("settlement.idempotency_key.too_long");
        }
    }

    /**
     * Tells whether an idempotency key is actually present.
     *
     * @return {@code true} when the caller sent an idempotency key
     */
    public boolean isPresent() {
        return value != null;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        var trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
