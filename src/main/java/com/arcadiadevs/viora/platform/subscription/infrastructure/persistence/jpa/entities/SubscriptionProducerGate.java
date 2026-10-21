package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.*;
/** Infrastructure-only serialization row. It contains no external account entity. */
@Entity @Table(name = "subscription_producer_gates") @Getter @Setter @NoArgsConstructor
public class SubscriptionProducerGate {
    @Id @Column(name = "producer_id", length = 128) private String producerId;
}
