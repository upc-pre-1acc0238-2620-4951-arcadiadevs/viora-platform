package com.arcadiadevs.viora.platform.territory.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.application.internal.security.LicenseAccessPolicy;
import com.arcadiadevs.viora.platform.territory.application.queryservices.CaliberRangeSetQueryService;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetCaliberRangesByManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberRange;
import com.arcadiadevs.viora.platform.territory.domain.repositories.CaliberRangeSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reads the caliber ranges of the license a manager is in charge of.
 */
@Service
@Transactional(readOnly = true)
public class CaliberRangeSetQueryServiceImpl implements CaliberRangeSetQueryService {

    private final CaliberRangeSetRepository caliberRangeSetRepository;
    private final LicenseAccessPolicy licenseAccessPolicy;

    /**
     * Creates the service.
     *
     * @param caliberRangeSetRepository the ranges of each license
     * @param licenseAccessPolicy       resolves the license of the manager
     * @throws IllegalArgumentException if any dependency is null
     */
    public CaliberRangeSetQueryServiceImpl(CaliberRangeSetRepository caliberRangeSetRepository,
                                           LicenseAccessPolicy licenseAccessPolicy) {
        if (caliberRangeSetRepository == null || licenseAccessPolicy == null) {
            throw new IllegalArgumentException("territory.caliber_range_set_service.dependency.null");
        }
        this.caliberRangeSetRepository = caliberRangeSetRepository;
        this.licenseAccessPolicy = licenseAccessPolicy;
    }

    @Override
    public Result<List<CaliberRange>, ApplicationError> handle(GetCaliberRangesByManagerIdQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "territory.caliber_range_set.query.null"));
        }
        var scope = licenseAccessPolicy.resolveManager(query.managerId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        return Result.success(caliberRangeSetRepository.findByLicenseId(scope.success().orElseThrow().licenseId())
                .map(set -> set.snapshot().ranges())
                .orElse(List.of()));
    }
}
