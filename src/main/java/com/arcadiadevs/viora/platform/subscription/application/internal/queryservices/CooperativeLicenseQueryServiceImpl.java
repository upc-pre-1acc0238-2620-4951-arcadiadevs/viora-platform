package com.arcadiadevs.viora.platform.subscription.application.internal.queryservices;

import com.arcadiadevs.viora.platform.subscription.application.queryservices.CooperativeLicenseQueryService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetAllCooperativeLicensesByManagerIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeLicenseCapacityQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeLicenseByIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeLicenseByIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.LicenseCapacity;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link CooperativeLicenseQueryService}: the license a technical manager is in charge of, or the
 * one sponsoring a plan.
 */
@Service
@Transactional(readOnly = true)
public class CooperativeLicenseQueryServiceImpl implements CooperativeLicenseQueryService {

    private final CooperativeLicenseRepository licenses;
    private final InvitationCodeRepository codes;
    private final Clock clock;

    /**
     * Creates the query service.
     *
     * @param licenses the cooperative licenses
     * @param codes    the invitation codes, to count what the license has given
     * @param clock    the application clock
     * @throws IllegalArgumentException if a dependency is null
     */
    public CooperativeLicenseQueryServiceImpl(CooperativeLicenseRepository licenses, InvitationCodeRepository codes,
                                              Clock clock) {
        if (licenses == null || codes == null || clock == null) {
            throw new IllegalArgumentException("subscription.license_queries.dependency.null");
        }
        this.licenses = licenses;
        this.codes = codes;
        this.clock = clock;
    }

    @Override
    public List<CooperativeLicense> handle(GetAllCooperativeLicensesByManagerIdQuery query) {
        return licenses.findByManagerId(query.managerId()).stream().toList();
    }

    @Override
    public Optional<CooperativeLicense> handle(GetCooperativeLicenseByIdAndManagerIdQuery query) {
        return licenses.findById(query.licenseId())
                .filter(license -> query.managerId().equals(license.snapshot().managerId()));
    }

    @Override
    public Optional<CooperativeLicense> handle(GetCooperativeLicenseByIdQuery query) {
        return licenses.findById(query.licenseId());
    }

    @Override
    public Optional<LicenseCapacity> handle(GetCooperativeLicenseCapacityQuery query) {
        var now = clock.instant();
        return licenses.findById(query.licenseId()).map(license -> {
            var overdueAreaHa = codes.findOverdueByLicenseId(query.licenseId(), now).stream()
                    .map(code -> code.snapshot().quota().hectares())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return license.capacity(codes.countSeatsInUse(query.licenseId(), now), overdueAreaHa);
        });
    }
}
