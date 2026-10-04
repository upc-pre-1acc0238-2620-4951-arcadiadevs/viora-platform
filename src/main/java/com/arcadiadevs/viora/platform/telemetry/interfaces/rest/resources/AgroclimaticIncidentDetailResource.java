package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * REST presentation resource representing detailed incident context including steps and trend data.
 */
@Schema(name = "AgroclimaticIncidentDetailResource", description = "Detailed representation of an agroclimatic incident")
@NullMarked
public record AgroclimaticIncidentDetailResource(
        @Schema(description = "Incident unique identifier", example = "7c9e6679-5e2a-4a6f-871d-5a9e3a6c1234")
        String id,

        @Schema(description = "Plot unique identifier", example = "b1a2c3d4-1234-5678-9abc-def012345678")
        String plotId,

        @Schema(description = "Plot human-readable name", example = "Lote Norte")
        String plotName,

        @Schema(description = "Plot botanical olive variety", example = "Criolla")
        String plotVariety,

        @Schema(description = "Incident type", example = "HEAT_WAVE")
        String type,

        @Schema(description = "Severity level", example = "CRITICAL")
        String severity,

        @Schema(description = "Lifecycle status", example = "ACTIVE")
        String status,

        @Schema(description = "i18n translation key for the incident headline", example = "heat_wave.headline")
        String headline,

        @Schema(description = "Metric name that breached threshold", example = "AMBIENT_TEMPERATURE")
        String metricName,

        @Schema(description = "Current recorded reading value", example = "36.5")
        Double currentValue,

        @Schema(description = "Threshold boundary value", example = "36.0")
        Double thresholdValue,

        @Schema(description = "Measurement unit", example = "°C")
        String unit,

        @Schema(description = "Trigger timestamp", example = "2026-02-18T08:00:00Z")
        Instant triggeredAt,

        @Schema(description = "Trigger date string", example = "2026-02-18")
        String triggerDate,

        @Schema(description = "Time window of highest risk", example = "11:00 - 16:00")
        String timeWindow,

        @Schema(description = "Cumulative stress duration in minutes", example = "120")
        Long stressDurationMinutes,

        @Schema(description = "Snoozed until instant, if snoozed", example = "2026-02-18T14:00:00Z")
        @Nullable Instant snoozedUntil,

        @Schema(description = "Actionable mitigation checklist steps")
        List<MitigationStepResource> mitigationSteps,

        @Schema(description = "Weekly observation trend data points")
        List<WeeklyTrendPointResource> weeklyTrend
) {
    public AgroclimaticIncidentDetailResource {
        mitigationSteps = (mitigationSteps != null) ? List.copyOf(mitigationSteps) : List.of();
        weeklyTrend = (weeklyTrend != null) ? List.copyOf(weeklyTrend) : List.of();
    }
}
