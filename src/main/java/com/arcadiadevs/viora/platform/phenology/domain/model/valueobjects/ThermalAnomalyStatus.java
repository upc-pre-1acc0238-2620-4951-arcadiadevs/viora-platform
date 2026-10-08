package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

/**
 * Warm winter signal of a chill season (US23), derived from the daily maximum temperatures at the plot.
 *
 * <p>A warm spell is a run of more than {@code 3} consecutive days with a maximum above {@code 24 °C} inside the
 * winter period. It is a local thermal rule, not the official El Niño index.
 * <ul>
 *   <li>{@code NONE}: no warm spell happened in the season.</li>
 *   <li>{@code ACTIVE}: the season is going on and the last evaluated days are a warm spell.</li>
 *   <li>{@code RECORDED}: a warm spell happened earlier in the season (or in the finished season).</li>
 * </ul>
 * </p>
 */
public enum ThermalAnomalyStatus {
    NONE,
    ACTIVE,
    RECORDED;

    /**
     * Daily maximum, in °C, above which a day counts as warm.
     */
    public static final double WARM_DAY_MAX_TEMPERATURE = 24.0;

    /**
     * A warm spell needs strictly more than this number of consecutive warm days.
     */
    public static final int WARM_SPELL_MIN_EXCLUSIVE_DAYS = 3;
}
