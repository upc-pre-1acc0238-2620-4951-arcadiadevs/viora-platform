package com.arcadiadevs.viora.platform.territory.application.queryservices;

import java.time.LocalDate;

/**
 * Read model of one reason of a plot's color.
 *
 * @param type         {@code OVERLOAD}, {@code CRITICAL_INCIDENT}, {@code FROST_FORECAST} or {@code WARNING_INCIDENT}
 * @param incidentType the incident, such as {@code FROST_WARNING}; null for other reasons
 * @param value        the overload percentage or the forecast minimum in Celsius; null for incidents
 * @param date         the day of the forecast minimum; null for other reasons
 */
public record RiskReasonView(String type, String incidentType, Double value, LocalDate date) {
}
