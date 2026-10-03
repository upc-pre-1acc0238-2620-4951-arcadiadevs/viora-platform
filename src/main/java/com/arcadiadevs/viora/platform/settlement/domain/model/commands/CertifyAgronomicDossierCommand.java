package com.arcadiadevs.viora.platform.settlement.domain.model.commands;

/**
 * Instruction to certify the dossier of one settled campaign of a plot.
 *
 * <p>Only presence of the mandatory fields is checked here (a missing field is rejected, blank text is not
 * judged); trimming, blank and length rules live in the value objects ({@code AuditorSignature},
 * {@code CertifierIdentity}, {@code CertificationNotes}) built before the aggregate is called, so there is a
 * single source for each rule.</p>
 *
 * @param plotId           plot whose campaign is certified
 * @param campaignYear     campaign to certify
 * @param auditorSignature declared collegiate signature
 * @param certifiedBy      name of the certifying professional
 * @param cipNumber        collegiate registration number
 * @param notes            optional certification notes
 */
public record CertifyAgronomicDossierCommand(String plotId, Integer campaignYear, String auditorSignature,
        String certifiedBy, String cipNumber, String notes) {

    public CertifyAgronomicDossierCommand {
        if (plotId == null) {
            throw new IllegalArgumentException("settlement.plot.id.null_or_empty");
        }
        if (campaignYear == null) {
            throw new IllegalArgumentException("settlement.campaign_year.null");
        }
        if (auditorSignature == null) {
            throw new IllegalArgumentException("settlement.certification.signature.invalid");
        }
        if (certifiedBy == null) {
            throw new IllegalArgumentException("settlement.certification.certifier.invalid");
        }
        if (cipNumber == null) {
            throw new IllegalArgumentException("settlement.certification.cip.invalid");
        }
    }
}
