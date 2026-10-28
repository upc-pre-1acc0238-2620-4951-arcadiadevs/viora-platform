package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Commercial size scale of table olives, expressed as number of fruits per kilogram.
 *
 * <p>Source: International Olive Council, Trade Standard Applying to Table Olives, COI/OT/NC no. 1 (2004),
 * section 2.5 "Sizing": 60/70 to 111/120 in steps of 10, 121/140 to 181/200 in steps of 20,
 * 201/230 to 381/410 in steps of 30 and, above 410, intervals of 50 fruits.</p>
 */
public final class CommercialSizeScale {

    /** Smallest count of the scale; fewer fruits per kilogram fall outside it. */
    public static final int SCALE_START = 60;

    private static final int[] UPPER_BOUNDS = {
            70, 80, 90, 100, 110, 120,
            140, 160, 180, 200,
            230, 260, 290, 320, 350, 380, 410
    };

    private static final int OPEN_INTERVAL_STEP = 50;

    /**
     * Largest count the scale names; above it the label is {@code ">2000"}. Even the smallest table olive weighs
     * well over half a gram, so a bigger count is a typing error, never a real caliber.
     */
    public static final int MAX_FRUITS_PER_KG = 2000;

    private CommercialSizeScale() {
    }

    /**
     * Returns the size grade label containing a fruits-per-kilogram value, for example {@code "91/100"}.
     *
     * @param fruitsPerKg number of fruits per kilogram, positive
     * @return the grade label, {@code "<60"} for fruit larger than the first grade of the scale, or
     *         {@code ">2000"} above {@link #MAX_FRUITS_PER_KG}
     */
    public static String gradeOf(double fruitsPerKg) {
        if (!Double.isFinite(fruitsPerKg) || fruitsPerKg <= 0.0) {
            throw new IllegalArgumentException("thinning.caliber.fruits_per_kg.invalid");
        }
        long count = Math.round(fruitsPerKg);
        if (count < SCALE_START) {
            return "<" + SCALE_START;
        }
        if (count > MAX_FRUITS_PER_KG) {
            return ">" + MAX_FRUITS_PER_KG;
        }
        long lower = SCALE_START;
        for (int upper : UPPER_BOUNDS) {
            if (count <= upper) {
                return lower + "/" + upper;
            }
            lower = upper + 1L;
        }
        // Above the last bound the intervals are open-ended, 50 fruits wide: 411/460, 461/510 and so on.
        long lastUpper = UPPER_BOUNDS[UPPER_BOUNDS.length - 1];
        long intervals = (count - lastUpper - 1) / OPEN_INTERVAL_STEP + 1;
        long upper = lastUpper + intervals * OPEN_INTERVAL_STEP;
        return (upper - OPEN_INTERVAL_STEP + 1) + "/" + upper;
    }
}
