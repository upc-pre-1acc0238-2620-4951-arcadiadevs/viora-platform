package com.arcadiadevs.viora.platform.territory.application.queryservices;

import java.time.Instant;

/**
 * The provisional dossier of a member plot as a PDF to download.
 *
 * @param content  the PDF bytes
 * @param fileName a file name with the plot and the cut-off day
 * @param cutOffAt when its data was read
 */
public record ProvisionalDossierFile(byte[] content, String fileName, Instant cutOffAt) {
}
