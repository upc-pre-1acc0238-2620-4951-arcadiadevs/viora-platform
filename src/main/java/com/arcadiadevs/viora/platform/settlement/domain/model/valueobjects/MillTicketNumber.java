package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

/**
 * Optional ticket number of the mill that received the delivered olives, for example a weighbridge ticket.
 *
 * <p>The absence of a ticket is modelled by an instance without a value rather than by a {@code null} reference:
 * a settlement always holds a ticket object, and {@link #isPresent()} tells whether it carries one. That keeps the
 * invariant of the record a single rule (a value is either null or at most {@value #MAX_LENGTH} characters) instead
 * of spreading "ticket or no ticket" over two representations.</p>
 *
 * @param value the trimmed ticket number, or null when the mill did not issue one
 */
public record MillTicketNumber(String value) {

    /** Longest ticket number accepted; it is what the mill column stores. */
    public static final int MAX_LENGTH = 30;

    /**
     * Builds a ticket number from raw input, trimming it and treating a blank value as no ticket at all.
     *
     * @param raw the raw ticket number, possibly null or blank
     * @return the ticket number, absent when the raw value is null or blank
     * @throws IllegalArgumentException if the trimmed value is longer than {@value #MAX_LENGTH} characters
     */
    public static MillTicketNumber of(String raw) {
        return new MillTicketNumber(normalize(raw));
    }

    /**
     * Rebuilds a ticket number from persistence.
     *
     * @param value the persisted ticket number, or null when there was none
     * @throws IllegalArgumentException if the value is longer than {@value #MAX_LENGTH} characters
     */
    public MillTicketNumber {
        if (value != null && value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("settlement.mill_ticket.too_long");
        }
    }

    /**
     * Tells whether a ticket number is actually present.
     *
     * @return {@code true} when the mill issued a ticket number
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
