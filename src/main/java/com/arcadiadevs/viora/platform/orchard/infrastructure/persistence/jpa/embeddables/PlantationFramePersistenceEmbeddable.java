package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Embeddable mapping plantation frame coordinates.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PlantationFramePersistenceEmbeddable {

    @Column(name = "row_spacing_m", nullable = false)
    private Double rowSpacingM;

    @Column(name = "tree_spacing_m", nullable = false)
    private Double treeSpacingM;

    public PlantationFramePersistenceEmbeddable(Double rowSpacingM, Double treeSpacingM) {
        this.rowSpacingM = rowSpacingM;
        this.treeSpacingM = treeSpacingM;
    }
}
