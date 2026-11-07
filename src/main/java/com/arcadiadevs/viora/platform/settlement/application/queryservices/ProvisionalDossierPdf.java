package com.arcadiadevs.viora.platform.settlement.application.queryservices;

import java.time.Instant;

/**
 * A provisional dossier rendered as a PDF.
 *
 * @param content  the PDF bytes
 * @param cutOffAt the instant its data was read
 */
public record ProvisionalDossierPdf(byte[] content, Instant cutOffAt) {
}
