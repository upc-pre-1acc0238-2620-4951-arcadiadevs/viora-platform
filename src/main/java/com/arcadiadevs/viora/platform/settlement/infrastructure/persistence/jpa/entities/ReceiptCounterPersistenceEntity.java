package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * JPA entity mapping the {@code harvest_receipt_counters} relational database table: one row per producer and
 * campaign year holding the last receipt sequence handed out to that producer.
 *
 * <p>The unique constraint on {@code (producer_id, campaign_year)} is what makes the row safe to create: two
 * settlements that race to open the counter of the same producer and campaign cannot both insert it.</p>
 */
@Entity
@Table(name = "harvest_receipt_counters",
        uniqueConstraints = @UniqueConstraint(name = "uq_receipt_counter_producer_campaign",
                columnNames = {"producer_id", "campaign_year"}))
@Getter
@Setter
@NoArgsConstructor
public class ReceiptCounterPersistenceEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "producer_id", nullable = false, updatable = false)
    private UUID producerId;

    @Column(name = "campaign_year", nullable = false, updatable = false)
    private Integer campaignYear;

    /** The only mutable column: every settlement of the same producer and campaign moves it forward. */
    @Column(name = "last_sequence", nullable = false)
    private Integer lastSequence;
}
