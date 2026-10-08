package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

/**
 * Outcome of projecting when a season in progress will reach its chill threshold.
 *
 * <p>The projection extrapolates the pace of the last {@code 14} evaluated days.
 * <ul>
 *   <li>{@code PROJECTED}: at that pace the threshold is reached before August 31; the date is given.</li>
 *   <li>{@code NOT_REACHABLE_IN_SEASON}: at that pace (or with no accumulation at all) the threshold is not
 *       reached before the season ends.</li>
 *   <li>{@code INSUFFICIENT_DATA}: fewer than 14 days of the season have been evaluated.</li>
 *   <li>{@code NOT_APPLICABLE}: the threshold was already reached or the season is over.</li>
 * </ul>
 * </p>
 */
public enum ChillProjectionStatus {
    PROJECTED,
    NOT_REACHABLE_IN_SEASON,
    INSUFFICIENT_DATA,
    NOT_APPLICABLE;

    /**
     * Number of recent days whose pace is extrapolated.
     */
    public static final int PACE_WINDOW_DAYS = 14;
}
