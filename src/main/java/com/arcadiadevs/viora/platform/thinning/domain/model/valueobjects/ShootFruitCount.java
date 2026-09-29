package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Value Object representing the count of evaluated shoots and observed fruit set on an olive branch.
 *
 * @param shootCount   the number of representative shoots counted (must be > 0)
 * @param fruitSetCount the number of set fruits observed (must be >= 0)
 */
public record ShootFruitCount(Integer shootCount, Integer fruitSetCount) {

    /**
     * Compact constructor validating positive counts.
     */
    public ShootFruitCount {
        if (shootCount == null || shootCount <= 0) {
            throw new IllegalArgumentException("thinning.shoot_count.positive");
        }
        if (fruitSetCount == null || fruitSetCount < 0) {
            throw new IllegalArgumentException("thinning.fruit_count.negative");
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
