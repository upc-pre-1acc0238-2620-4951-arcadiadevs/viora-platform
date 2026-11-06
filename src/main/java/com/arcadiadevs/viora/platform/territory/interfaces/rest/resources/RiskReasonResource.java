package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * One reason of a plot's color.
 *
 * @param type         {@code OVERLOAD}, {@code CRITICAL_INCIDENT}, {@code FROST_FORECAST} or {@code WARNING_INCIDENT}
 * @param incidentType the incident, such as {@code FROST_WARNING}; null for other reasons
 * @param value        the overload percentage or the forecast minimum in Celsius; null for incidents
 * @param date         the day of the forecast minimum; null for other reasons
 */
@Schema(name = "RiskReasonResource", description = "Why a member plot has its color")
public record RiskReasonResource(
        @Schema(example = "OVERLOAD") String type,
        @Schema(example = "FROST_WARNING") String incidentType,
        @Schema(example = "43.0") Double value,
        LocalDate date
) {
}
