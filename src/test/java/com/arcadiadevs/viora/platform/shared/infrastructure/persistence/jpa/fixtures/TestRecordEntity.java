package com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.fixtures;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "test_records",
        indexes = {
                @Index(name = "idx_test_record_parent_id", columnList = "parent_id")
        }
)
public class TestRecordEntity extends AuditableAbstractPersistenceEntity {

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal measurementValue;

    @Column(precision = 8, scale = 2)
    private BigDecimal percentageScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private TestParentEntity parent;

    public TestRecordEntity(BigDecimal measurementValue, BigDecimal percentageScore, TestParentEntity parent) {
        this.setId(java.util.UUID.randomUUID());
        this.measurementValue = measurementValue;
        this.percentageScore = percentageScore;
        this.parent = parent;
    }
}
