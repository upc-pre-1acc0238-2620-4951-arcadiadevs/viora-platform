package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Value Object representing the count of evaluated shoots and observed fruit set on an olive branch.
 *
 * @param shootCount   the number of representative shoots counted (must be > 0)
 * @param fruitSetCount the number of set fruits observed (must be >= 0)
 */
public record ShootFruitCount(Integer shootCount, Integer fruitSetCount) {

    /** Far above the 30 to 50 shoots counted per tree; a bigger count is a typing error. */
    public static final int MAX_SHOOT_COUNT = 1000;

    /** Far above what a tree can set; a bigger count is a typing error. */
    public static final int MAX_FRUIT_SET_COUNT = 100_000;

    /**
     * Compact constructor validating positive counts.
     */
    public ShootFruitCount {
        if (shootCount == null || shootCount <= 0) {
            throw new IllegalArgumentException("thinning.shoot_count.positive");
        }
        if (shootCount > MAX_SHOOT_COUNT) {
            throw new IllegalArgumentException("thinning.shoot_count.too_large");
        }
        if (fruitSetCount == null || fruitSetCount < 0) {
            throw new IllegalArgumentException("thinning.fruit_count.negative");
        }
        if (fruitSetCount > MAX_FRUIT_SET_COUNT) {
            throw new IllegalArgumentException("thinning.fruit_count.too_large");
        }
    }

    /**
     * Computes the mean number of fruits per shoot.
     *
     * @return mean fruits per shoot
     */
    public double fruitsPerShoot() {
        return (double) fruitSetCount / shootCount;
    }
}
