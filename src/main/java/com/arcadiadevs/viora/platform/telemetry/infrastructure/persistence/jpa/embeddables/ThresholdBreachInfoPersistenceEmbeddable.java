package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA embeddable mapping the threshold breach attributes in the database.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdBreachInfoPersistenceEmbeddable {

    @Column(name = "breach_metric_name", nullable = false, length = 50)
    private String metricName;

    @Column(name = "breach_current_value", nullable = false)
    private Double currentValue;

    @Column(name = "breach_threshold_value", nullable = false)
    private Double thresholdValue;

    @Column(name = "breach_unit", nullable = false, length = 20)
    private String unit;
}
