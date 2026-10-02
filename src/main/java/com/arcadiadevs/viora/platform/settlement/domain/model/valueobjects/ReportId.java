package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of the agronomic report (one per plot).
 *
 * @param reportId UUID string
 */
public record ReportId(String reportId) {

    public ReportId {
        if (reportId == null || reportId.isBlank()) {
            throw new IllegalArgumentException("settlement.report.id.null_or_empty");
        }
        try {
            reportId = UUID.fromString(reportId.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("settlement.report.id.invalid_uuid", e);
        }
    }

    public ReportId() {
        this(UUID.randomUUID().toString());
    }
}
