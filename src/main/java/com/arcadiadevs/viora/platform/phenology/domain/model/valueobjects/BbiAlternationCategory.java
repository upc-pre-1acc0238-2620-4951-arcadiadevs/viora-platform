package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

/**
 * Qualitative intensity classification for Hoblyn's Biennial Bearing Index (BBI).
 *
 * <p>Agronomic ranges:
 * <ul>
 *   <li>{@code REGULAR}: BBI &lt; 0.20 (balanced multi-year production)</li>
 *   <li>{@code MODERATE_ALTERNATION}: 0.20 &le; BBI &le; 0.40 (moderate alternate bearing)</li>
 *   <li>{@code SEVERE_ALTERNATION}: BBI &gt; 0.40 (severe on/off bearing cycles)</li>
 *   <li>{@code INSUFFICIENT_DATA}: fewer than 2 distinct harvest campaigns available</li>
 * </ul>
 * </p>
 */
public enum BbiAlternationCategory {
    REGULAR,
    MODERATE_ALTERNATION,
    SEVERE_ALTERNATION,
    INSUFFICIENT_DATA
}
