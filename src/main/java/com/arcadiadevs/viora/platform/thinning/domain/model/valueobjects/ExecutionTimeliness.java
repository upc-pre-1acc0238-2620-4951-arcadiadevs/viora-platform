package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Domain qualification representing the biological timeliness of manual thinning execution.
 *
 * <ul>
 *     <li>{@link #OPTIMAL}: Conducted prior to or at pit hardening (endocarp lignification), maximizing cell division.</li>
 *     <li>{@link #LATE}: Conducted after pit hardening window closure, offering diminished fruit sizing recovery.</li>
 * </ul>
 */
public enum ExecutionTimeliness {
    OPTIMAL,
    LATE
}
