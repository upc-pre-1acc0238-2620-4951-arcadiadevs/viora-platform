package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Immutable settlement voucher of one campaign: weights, thinning balance and stabilization curve frozen at
 * settlement time. Only the status may change later (certification).
 *
 * <p>{@code producer_id}, {@code receipt_number} and {@code weighed_on} are denormalized onto the settlement so a
 * settlement can be reached by producer, without walking the report, and so the receipt number can be numbered and
 * looked up per producer. They are declared nullable because {@code ddl-auto=update} cannot add a NOT NULL column
 * to a table that already holds rows: every settlement created after this change always fills them, but the rows
 * written before it keep them null until the one-off backfill script runs. The backfill is expected to copy
 * {@code producer_id} from the parent report and to give the older rows a derived receipt number and weighing date;
 * once it has run everywhere, these three columns can be tightened to NOT NULL; {@code mill_ticket_number} and
 * {@code idempotency_key} stay nullable, because both are optional by contract.</p>
 *
 * <p>Both unique constraints are deliberately plain ones. A unique constraint admits many rows whose constrained
 * column is NULL, because NULLs compare as distinct in Postgres and in H2, so {@code uq_settlement_producer_idempotency}
 * means exactly "one settlement per producer and idempotency key, and any number of settlements without a key",
 * which is the required "unique where not null" semantics without a partial index.</p>
 *
 * <p>That same NULL rule means the two new constraints only protect the rows that carry a producer: until the
 * backfill has run, the rows written before this change have a NULL {@code producer_id} and escape both of them.
 * They are frozen settlements that cannot be settled twice anyway, so they need no protection.</p>
 */
@Entity
@Table(name = "harvest_settlements",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_settlement_report_campaign",
                        columnNames = {"report_id", "campaign_year"}),
                @UniqueConstraint(name = "uq_settlement_producer_receipt",
                        columnNames = {"producer_id", "receipt_number"}),
                @UniqueConstraint(name = "uq_settlement_producer_idempotency",
                        columnNames = {"producer_id", "idempotency_key"})
        })
@Getter
@Setter
public class HarvestSettlementPersistenceEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, updatable = false)
    private AgronomicReportPersistenceEntity report;

    /** Denormalized owner of the settlement; see the class Javadoc for the pending backfill. */
    @Column(name = "producer_id", updatable = false)
    private UUID producerId;

    @Column(name = "campaign_year", nullable = false, updatable = false)
    private Integer campaignYear;
    @Column(nullable = false, updatable = false)
    private Double greenOlivesKg;
    @Column(nullable = false, updatable = false)
    private Double blackOlivesKg;
    @Column(nullable = false, updatable = false)
    private Double totalYieldKg;
    @Column(updatable = false)
    private Double commercialFruitsPerKg;
    @Column(updatable = false, length = 1000)
    private String notes;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false, updatable = false)
    private Instant settledAt;

    // Receipt and weighing data frozen at settlement
    @Column(name = "receipt_number", length = 12, updatable = false)
    private String receiptNumber;
    @Column(name = "weighed_on", updatable = false)
    private LocalDate weighedOn;
    @Column(name = "mill_ticket_number", length = 30, updatable = false)
    private String millTicketNumber;
    @Column(name = "idempotency_key", length = 64, updatable = false)
    private String idempotencyKey;

    // Thinning balance frozen at settlement
    @Column(nullable = false, updatable = false, length = 20)
    private String thinningStatus;
    @Column(updatable = false)
    private LocalDate thinningExecutedDate;
    @Column(updatable = false)
    private Double prescribedRemovalPercentage;
    @Column(updatable = false)
    private Double actualRemovalPercentage;
    @Column(updatable = false)
    private Double removalDeviationPoints;

    // Stabilization curve frozen at settlement
    @Column(nullable = false, updatable = false, length = 30)
    private String curveStatus;
    @Column(nullable = false, updatable = false)
    private Integer baselineCampaigns;
    @Column(nullable = false, updatable = false)
    private Integer settledCampaigns;
    @Column(updatable = false)
    private Double baselineYieldKg;
    @Column(updatable = false)
    private Double baselineAlternationIndex;
    @Column(updatable = false)
    private Double managedAlternationIndex;
    @Column(updatable = false)
    private Double amplitudeReductionRate;
    @Column(updatable = false)
    private Boolean stabilizationTargetAchieved;
    @Column(updatable = false)
    private Double interannualVarianceKg2;
    @Column(updatable = false)
    private Double coefficientOfVariation;
}
