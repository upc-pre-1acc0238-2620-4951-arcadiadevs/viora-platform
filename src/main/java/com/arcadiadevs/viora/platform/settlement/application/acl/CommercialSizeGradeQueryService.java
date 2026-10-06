package com.arcadiadevs.viora.platform.settlement.application.acl;

import java.util.Optional;

/**
 * Application port that names the commercial size grade of a settled caliber.
 *
 * <p>It exists because the assembler that renders a settlement lives in the interfaces layer, which must not reach
 * into {@code application.internal}: the interfaces layer talks to a published port, and the port is implemented on
 * top of the outbound ACL.</p>
 */
public interface CommercialSizeGradeQueryService {

    /**
     * Resolves the grade label of a caliber.
     *
     * @param fruitsPerKg number of fruits per kilogram, or null when the caliber is unknown
     * @return the grade label, or empty when there is no caliber to grade
     */
    Optional<String> gradeOf(Double fruitsPerKg);
}
