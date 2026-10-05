package com.arcadiadevs.viora.platform.thinning.domain.model.commands;

/**
 * Asks to issue the prescription of a plot and campaign if everything it needs is already known, for
 * example because a technical profile was approved after the sampling.
 *
 * @param plotId       plot UUID
 * @param campaignYear campaign to evaluate
 */
public record EvaluateThinningPrescriptionCommand(String plotId, int campaignYear) {
}
