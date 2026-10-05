package com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity representing the persistence table {@code crop_load.tree_sampling_records}.
 */
@Entity
@Table(name = "tree_sampling_records")
public class TreeSamplingRecordPersistenceEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private SamplingRoundPersistenceEntity round;

    @Column(name = "tree_tag", nullable = false, length = 50)
    private String treeTag;

    @Column(name = "shoot_count", nullable = false)
    private Integer shootCount;

    @Column(name = "fruit_set_count", nullable = false)
    private Integer fruitSetCount;

    @Column(name = "trunk_diameter_mm", nullable = true)
    private Double trunkDiameterMm;

    @Column(name = "sampling_date", nullable = false)
    private LocalDate samplingDate;

    public TreeSamplingRecordPersistenceEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SamplingRoundPersistenceEntity getRound() {
        return round;
    }

    public void setRound(SamplingRoundPersistenceEntity round) {
        this.round = round;
    }

    public String getTreeTag() {
        return treeTag;
    }

    public void setTreeTag(String treeTag) {
        this.treeTag = treeTag;
    }

    public Integer getShootCount() {
        return shootCount;
    }

    public void setShootCount(Integer shootCount) {
        this.shootCount = shootCount;
    }

    public Integer getFruitSetCount() {
        return fruitSetCount;
    }

    public void setFruitSetCount(Integer fruitSetCount) {
        this.fruitSetCount = fruitSetCount;
    }

    public Double getTrunkDiameterMm() {
        return trunkDiameterMm;
    }

    public void setTrunkDiameterMm(Double trunkDiameterMm) {
        this.trunkDiameterMm = trunkDiameterMm;
    }

    public LocalDate getSamplingDate() {
        return samplingDate;
    }

    public void setSamplingDate(LocalDate samplingDate) {
        this.samplingDate = samplingDate;
    }
}
