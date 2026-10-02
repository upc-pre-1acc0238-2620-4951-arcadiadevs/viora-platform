package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One agronomic report per plot; the unique plot reference turns a duplicate-creation race into a conflict. */
@Entity
@Table(name = "agronomic_reports",
        uniqueConstraints = @UniqueConstraint(name = "uq_agronomic_report_plot", columnNames = "plot_id"))
@Getter
@Setter
public class AgronomicReportPersistenceEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "plot_id", nullable = false, updatable = false)
    private UUID plotId;

    @Column(name = "producer_id", nullable = false, updatable = false)
    private UUID producerId;

    @Version
    @Column(name = "revision")
    private Long revision;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("campaignYear ASC")
    private List<HarvestSettlementPersistenceEntity> settlements = new ArrayList<>();
}
