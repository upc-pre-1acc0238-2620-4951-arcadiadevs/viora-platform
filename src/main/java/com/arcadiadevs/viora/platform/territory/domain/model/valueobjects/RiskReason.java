package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * One reason of the color of a member plot, such as «Sobrecarga 43 %» or «Helada 1,2 °C el jueves».
 *
 * @param type         what kind of reason
 * @param incidentType the agroclimatic incident, such as {@code FROST_WARNING}; null for other reasons
 * @param value        the overload percentage or the forecast minimum in Celsius; null for incidents
 * @param date         the day of the forecast minimum; null for other reasons
 */
public record RiskReason(RiskReasonType type, String incidentType, Double value, LocalDate date) {

    /**
     * Validates that the reason has a type.
     *
     * @throws IllegalArgumentException if the type is null
     */
    public RiskReason {
        if (type == null) {
            throw new IllegalArgumentException("territory.risk.reason.type.null");
        }
    }
}
