package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * What a sector alert is about, and how urgent it is for the manager.
 */
public enum SectorAlertType {
    /** The sector has plots in red for overload or a critical incident: «La Yarada Baja pasó a Crítico». */
    SECTOR_CRITICAL(SectorAlertSeverity.CRITICAL),
    /** Frost is forecast or open in the sector: «Helada radiativa en La Yarada Alta». */
    FROST(SectorAlertSeverity.ATTENTION);

    private final SectorAlertSeverity severity;

    SectorAlertType(SectorAlertSeverity severity) {
        this.severity = severity;
    }

    /**
     * How urgent this kind of alert is.
     *
     * @return the severity
     */
    public SectorAlertSeverity severity() {
        return severity;
    }
}
