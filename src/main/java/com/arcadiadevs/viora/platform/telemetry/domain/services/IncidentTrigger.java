package com.arcadiadevs.viora.platform.telemetry.domain.services;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.ThresholdBreachInfo;

import java.util.List;

/**
 * Result record produced when an agroclimatic threshold evaluation triggers an alert condition.
 *
 * @param type                      the incident type
 * @param severity                  the severity classification
 * @param breachInfo                the metric details that caused the breach
 * @param mitigationInstructionKeys the recommended action keys for mitigation
 */
public record IncidentTrigger(
        IncidentType type,
        IncidentSeverity severity,
        ThresholdBreachInfo breachInfo,
        List<String> mitigationInstructionKeys
) {
    public IncidentTrigger {
        mitigationInstructionKeys = (mitigationInstructionKeys != null) ? List.copyOf(mitigationInstructionKeys) : List.of();
    }
}
