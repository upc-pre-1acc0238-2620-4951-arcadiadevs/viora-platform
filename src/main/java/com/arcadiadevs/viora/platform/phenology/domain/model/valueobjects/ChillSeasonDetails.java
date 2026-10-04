package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * Value object capturing detailed chronological and operational indicators
 * for an olive orchard plot's winter chill accumulation season.
 *
 * @param seasonStart    the official start date of winter chilling tracking
 * @param completionDate the calendar date when varietal chilling requirement was met, or {@code null} if still accumulating
 * @param idleDays       the count of consecutive recent days with zero chilling portion accumulation
 * @param seasonState    the current biological state of the winter chilling cycle
 */
public record ChillSeasonDetails(
        LocalDate seasonStart,
        LocalDate completionDate,
        Integer idleDays,
        ChillSeasonState seasonState
) {
    /**
     * Compact constructor validating mandatory domain invariants using i18n keys.
     */
    public ChillSeasonDetails {
        if (seasonStart == null) {
            throw new IllegalArgumentException("phenology.season_start.null");
        }
        if (seasonState == null) {
            throw new IllegalArgumentException("phenology.season_state.null");
        }
        if (idleDays == null || idleDays < 0) {
            idleDays = 0;
        }
    }
}
