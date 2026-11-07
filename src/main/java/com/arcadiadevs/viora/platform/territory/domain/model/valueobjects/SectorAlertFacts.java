package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * What an open sector alert says, kept up to date while it lasts: «Teodoro Mamani registró una sobrecarga de 43 %.
 * Ya son 5 parcelas en rojo en el sector» or «Mínima prevista de 1,2 °C».
 *
 * @param affectedPlots  the plots of the sector the alert is about
 * @param sectorPlots    the member plots of the sector
 * @param leadPlotId     the plot that leads the alert; null if none stands out
 * @param leadProducerId its owner; null with no lead plot
 * @param leadReason     why that plot leads it
 * @param leadValue      the overload percentage or the forecast minimum; null for an incident
 * @param leadDate       the day of the forecast minimum; null otherwise
 */
public record SectorAlertFacts(int affectedPlots, int sectorPlots, PlotId leadPlotId, ProducerId leadProducerId,
                               RiskReasonType leadReason, Double leadValue, LocalDate leadDate) {

    /**
     * Validates the counts.
     *
     * @throws IllegalArgumentException if a count is negative or the affected plots exceed the sector's
     */
    public SectorAlertFacts {
        if (affectedPlots < 0 || sectorPlots < affectedPlots) {
            throw new IllegalArgumentException("territory.sector_alert.facts.invalid");
        }
    }
}
