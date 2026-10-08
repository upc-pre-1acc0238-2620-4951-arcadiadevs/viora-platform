package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

/**
 * Biological and operational state of the winter chilling accumulation cycle for an olive plot.
 *
 * <p>State lifecycle:
 * <ul>
 *   <li>{@code IN_PROGRESS}: today is within the winter chilling period (June 1 - August 31) and the
 *       accumulated portions have not reached the varietal threshold yet.</li>
 *   <li>{@code HALTED}: same as {@code IN_PROGRESS}, but a warm spell is going on right now (daily maximum above
 *       24 °C for more than 3 days in a row), so the cold that was building up is being lost.</li>
 *   <li>{@code COMPLETED}: within the period, the accumulated portions already reached the threshold.</li>
 *   <li>{@code OFF_SEASON}: today is outside the period; the metric then describes the last finished winter.</li>
 * </ul>
 * </p>
 */
public enum ChillSeasonState {
    IN_PROGRESS,
    HALTED,
    COMPLETED,
    OFF_SEASON
}
