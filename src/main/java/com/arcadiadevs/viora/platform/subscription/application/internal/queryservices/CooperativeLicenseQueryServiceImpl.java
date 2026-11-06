package com.arcadiadevs.viora.platform.subscription.application.internal.queryservices;

import com.arcadiadevs.viora.platform.subscription.application.queryservices.CooperativeLicenseQueryService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetAllCooperativeLicensesByManagerIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeLicenseByIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeLicenseByIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Creates the query service.
     *
     * @param licenses the cooperative licenses
     * @throws IllegalArgumentException if the repository is null
     */
    public CooperativeLicenseQueryServiceImpl(CooperativeLicenseRepository licenses) {
        if (licenses == null) {
            throw new IllegalArgumentException("subscription.license_repository.null");
        }
        this.licenses = licenses;
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
}
