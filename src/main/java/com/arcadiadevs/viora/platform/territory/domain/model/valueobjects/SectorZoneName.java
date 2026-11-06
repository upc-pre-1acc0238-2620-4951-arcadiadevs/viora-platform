package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Name of a sector zone as people write it, such as «La Yarada Baja».
 * <p>Two names are the same sector when their {@link #normalized()} forms match: accents, case and repeated spaces do
 * not count, so «cerro blanco» and «Cerro  Blanco» are one sector.</p>
 *
 * @param value the name, trimmed and with single spaces
 */
public record SectorZoneName(String value) {

    /** The longest name accepted. */
    public static final int MAX_LENGTH = 60;

    /**
     * Validates the name, trimming it and collapsing its spaces.
     *
     * @throws IllegalArgumentException if the name is blank, too short or too long
     */
    public SectorZoneName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("territory.sector_zone.name.blank");
        }
        value = value.trim().replaceAll("\\s+", " ");
        if (value.length() < 2 || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("territory.sector_zone.name.length");
        }
    }

    /**
     * Returns the form used to compare names: lowercase, without accents and with single spaces.
     *
     * @return the normalized name
     */
    public String normalized() {
        var decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Tells whether two names are the same sector.
     *
     * @param other the other name
     * @return true when both normalized forms match
     */
    public boolean sameSectorAs(SectorZoneName other) {
        return other != null && normalized().equals(other.normalized());
    }
}
