package com.arcadiadevs.viora.platform.settlement.domain.model.commands;

/**
 * Command asking to seal the agronomic dossier of a settled campaign (US30). Values are validated by the
 * application service, which builds the value objects.
 *
 * @param plotId       plot UUID
 * @param campaignYear settled campaign
 * @param actorId      producer asking for it; only the owner of the plot can seal its dossier
 */
public record SealAgronomicDossierCommand(String plotId, Integer campaignYear, String actorId) {
}
