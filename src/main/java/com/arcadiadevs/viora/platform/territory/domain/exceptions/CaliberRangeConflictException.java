package com.arcadiadevs.viora.platform.territory.domain.exceptions;

/**
 * A caliber range that cannot sit with the others: it overlaps the previous one or leaves a gap after it.
 */
public class CaliberRangeConflictException extends RuntimeException {

    private final int rangeIndex;

    /**
     * Creates the exception.
     *
     * @param reason     the i18n key of the conflict
     * @param rangeIndex the position of the range in conflict, from 0
     */
    public CaliberRangeConflictException(String reason, int rangeIndex) {
        super(reason);
        this.rangeIndex = rangeIndex;
    }

    /**
     * The position of the range in conflict.
     *
     * @return the index, from 0
     */
    public int rangeIndex() {
        return rangeIndex;
    }
}
