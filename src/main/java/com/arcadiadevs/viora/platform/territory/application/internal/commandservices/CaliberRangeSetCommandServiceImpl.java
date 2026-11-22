package com.arcadiadevs.viora.platform.territory.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.application.commandservices.CaliberRangeSetCommandService;
import com.arcadiadevs.viora.platform.territory.application.internal.security.LicenseAccessPolicy;
import com.arcadiadevs.viora.platform.territory.domain.exceptions.CaliberRangeConflictException;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.CaliberRangeSet;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.ReplaceCaliberRangesCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberRange;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.territory.domain.repositories.CaliberRangeSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;

/**
 * Replaces the caliber ranges of the license a manager is in charge of. The ranges in force stay when the new ones do
 * not fit together, and the refusal names the range in conflict.
 */
@Service
public class CaliberRangeSetCommandServiceImpl implements CaliberRangeSetCommandService {

    private final CaliberRangeSetRepository caliberRangeSetRepository;
    private final LicenseAccessPolicy licenseAccessPolicy;

    /**
     * Creates the service.
     *
     * @param caliberRangeSetRepository the ranges of each license
     * @param licenseAccessPolicy       resolves the license of the manager
     * @throws IllegalArgumentException if any dependency is null
     */
    public CaliberRangeSetCommandServiceImpl(CaliberRangeSetRepository caliberRangeSetRepository,
                                             LicenseAccessPolicy licenseAccessPolicy) {
        if (caliberRangeSetRepository == null || licenseAccessPolicy == null) {
            throw new IllegalArgumentException("territory.caliber_range_set_service.dependency.null");
        }
        this.caliberRangeSetRepository = caliberRangeSetRepository;
        this.licenseAccessPolicy = licenseAccessPolicy;
    }

    @Override
    @Transactional
    public Result<CaliberRangeSet, ApplicationError> handle(ReplaceCaliberRangesCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command",
                    "territory.caliber_range_set.command.null"));
        }
        var ranges = new ArrayList<CaliberRange>();
        for (int i = 0; i < command.ranges().size(); i++) {
            var item = command.ranges().get(i);
            try {
                ranges.add(new CaliberRange(item.minFruitsPerKg(), item.maxFruitsPerKg()));
            } catch (IllegalArgumentException ex) {
                return conflict(ex.getMessage(), i);
            }
        }
        try {
            var scope = licenseAccessPolicy.resolveManager(new ManagerId(command.managerId()));
            if (scope.isFailure()) {
                return Result.failure(scope.failure().orElseThrow());
            }
            var licenseId = scope.success().orElseThrow().licenseId();
            var set = caliberRangeSetRepository.findByLicenseId(licenseId)
                    .orElseGet(() -> CaliberRangeSet.startFor(licenseId));
            set.replace(ranges);
            return Result.success(caliberRangeSetRepository.save(set));
        } catch (CaliberRangeConflictException ex) {
            return conflict(ex.getMessage(), ex.rangeIndex());
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("ranges", ex.getMessage(),
                    Map.of("reason", ex.getMessage())));
        }
    }

    private static Result<CaliberRangeSet, ApplicationError> conflict(String reason, int rangeIndex) {
        return Result.failure(ApplicationError.validationError("ranges", reason,
                Map.of("reason", reason, "rangeIndex", rangeIndex)));
    }
}
