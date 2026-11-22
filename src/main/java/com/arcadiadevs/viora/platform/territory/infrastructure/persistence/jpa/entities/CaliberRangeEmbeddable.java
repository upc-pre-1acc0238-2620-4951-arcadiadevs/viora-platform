package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One caliber range of a license, stored in order with the others.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class CaliberRangeEmbeddable {

    @Column(name = "min_fruits_per_kg", nullable = false)
    private Integer minFruitsPerKg;

    @Column(name = "max_fruits_per_kg", nullable = false)
    private Integer maxFruitsPerKg;
}
