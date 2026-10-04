package com.arcadiadevs.viora.platform.telemetry.domain.model.commands;

import java.util.List;

/**
 * Domain command expressing intent to raise an agroclimatic incident for an orchard plot.
 *
 * @param plotId                      the target plot identifier
 * @param type                        the incident type name
 * @param severity                    the severity classification name
 * @param metricName                  the metric that breached limits
 * @param currentValue                the recorded reading value
 * @param thresholdValue              the threshold limit
 * @param unit                        the measurement unit
 * @param mitigationStepInstructionKeys recommended action step keys
 */
public record RaiseAgroclimaticIncidentCommand(
        String plotId,
        String type,
        String severity,
        String metricName,
        Double currentValue,
        Double thresholdValue,
        String unit,
        List<String> mitigationStepInstructionKeys
) {
    public RaiseAgroclimaticIncidentCommand {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("incident.plot_id.null");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("incident.type.null");
        }
        if (severity == null || severity.isBlank()) {
            throw new IllegalArgumentException("incident.severity.null");
        }
        if (metricName == null || metricName.isBlank()) {
            throw new IllegalArgumentException("incident.breach_info.metric_name.null");
        }
        if (currentValue == null) {
            throw new IllegalArgumentException("incident.breach_info.current_value.null");
        }
        if (thresholdValue == null) {
            throw new IllegalArgumentException("incident.breach_info.threshold_value.null");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("incident.breach_info.unit.null");
        }
        mitigationStepInstructionKeys = (mitigationStepInstructionKeys != null)
                ? List.copyOf(mitigationStepInstructionKeys)
                : List.of();
    }
}
