package com.arcadiadevs.viora.platform.settlement.domain.model.queries;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.VerificationHash;

import java.util.Locale;

/**
 * Validated question asking which sealed dossier a PDF is, by its SHA-256.
 *
 * @param verificationHash 64 hexadecimal characters; surrounding spaces and upper case are accepted, as people copy it
 */
public record GetSealedDossierByVerificationHashQuery(String verificationHash) {

    public GetSealedDossierByVerificationHashQuery {
        verificationHash = new VerificationHash(verificationHash == null ? null
                : verificationHash.trim().toLowerCase(Locale.ROOT)).value();
    }
}
