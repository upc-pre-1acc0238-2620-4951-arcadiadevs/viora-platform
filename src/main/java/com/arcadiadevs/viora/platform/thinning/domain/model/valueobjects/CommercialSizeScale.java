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

    private CommercialSizeScale() {
    }

    /**
     * Returns the size grade label containing a fruits-per-kilogram value, for example {@code "91/100"}.
     *
     * @param fruitsPerKg number of fruits per kilogram, positive
     * @return the grade label, or {@code "<60"} for fruit larger than the first grade of the scale
     */
    public static String gradeOf(double fruitsPerKg) {
        if (!Double.isFinite(fruitsPerKg) || fruitsPerKg <= 0.0) {
            throw new IllegalArgumentException("thinning.caliber.fruits_per_kg.invalid");
        }
        long count = Math.round(fruitsPerKg);
        if (count < SCALE_START) {
            return "<" + SCALE_START;
        }
        long lower = SCALE_START;
        for (int upper : UPPER_BOUNDS) {
            if (count <= upper) {
                return lower + "/" + upper;
            }
            lower = upper + 1L;
        }
        long upper = UPPER_BOUNDS[UPPER_BOUNDS.length - 1];
        while (count > upper) {
            lower = upper + 1;
            upper += OPEN_INTERVAL_STEP;
        }
        return lower + "/" + upper;
    }
}
