package com.arcadiadevs.viora.platform.subscription.application.internal.queryservices;

import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalProfileService;
import com.arcadiadevs.viora.platform.subscription.application.ports.InvitationCodeCipher;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.InvitationCodeQueryService;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.InvitationCodeView;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetInvitationCodesByManagerIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Lists the invitation codes of the license a technical manager is in charge of. A code whose time is over shows as
 * expired even before it is marked so; only a code still to redeem is shown in full.
 */
@Service
@Transactional(readOnly = true)
public class InvitationCodeQueryServiceImpl implements InvitationCodeQueryService {

    private static final String PREFIX = "COOP-";
    private static final String HIDDEN = "••";

    private final CooperativeLicenseRepository licenses;
    private final InvitationCodeRepository codes;
    private final InvitationCodeCipher cipher;
    private final ExternalProfileService externalProfileService;
    private final Clock clock;

    /**
     * Creates the query service.
     *
     * @param licenses               the cooperative licenses
     * @param codes                  the invitation codes
     * @param cipher                 the encryption of codes still to redeem
     * @param externalProfileService the names of the producers
     * @param clock                  the application clock
     * @throws IllegalArgumentException if any dependency is null
     */
    public InvitationCodeQueryServiceImpl(CooperativeLicenseRepository licenses, InvitationCodeRepository codes,
                                          InvitationCodeCipher cipher, ExternalProfileService externalProfileService,
                                          Clock clock) {
        if (licenses == null || codes == null || cipher == null || externalProfileService == null || clock == null) {
            throw new IllegalArgumentException("subscription.invitation_code_queries.dependency.null");
        }
        this.licenses = licenses;
        this.codes = codes;
        this.cipher = cipher;
        this.externalProfileService = externalProfileService;
        this.clock = clock;
    }

    @Override
    public List<InvitationCodeView> handle(GetInvitationCodesByManagerIdQuery query) {
        var now = clock.instant();
        return licenses.findByManagerId(query.managerId())
                .map(license -> codes.findAllByLicenseId(license.snapshot().id()).stream()
                        .filter(code -> query.filter().includes(code.statusAt(now)))
                        .map(code -> toView(code, now))
                        .toList())
                .orElse(List.of());
    }

    private InvitationCodeView toView(InvitationCode code, Instant now) {
        var snapshot = code.snapshot();
        var status = code.statusAt(now);
        var inFull = status == InvitationCodeStatus.AVAILABLE && snapshot.sealedCode() != null && cipher.isAvailable()
                ? cipher.decrypt(snapshot.sealedCode()) : null;
        var redeemedBy = snapshot.redeemedBy() == null ? null
                : externalProfileService.findFullName(snapshot.redeemedBy()).orElse(null);
        return new InvitationCodeView(
                snapshot.id().invitationCodeId(),
                snapshot.batchId(),
                snapshot.issuedAt(),
                snapshot.expiresAt(),
                snapshot.quota().hectares(),
                status.name(),
                inFull,
                PREFIX + HIDDEN + (snapshot.hint() == null ? HIDDEN : snapshot.hint()),
                redeemedBy,
                snapshot.redeemedAt());
    }
}
