package com.arcadiadevs.viora.platform.territory.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.application.commandservices.SectorAlertCommandService;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.EvaluateSectorAlertsCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Evaluates the sector alerts of one license at a time.
 * <p>The scheduler and the plot events can ask for the same license at once. Each evaluation reads the open alerts
 * and opens the missing ones, and nothing in the database prevents two open alerts of the same type in a sector, so
 * two evaluations of the same license wait for each other. The lock is held across the whole transaction of
 * {@link SectorAlertCommandService#handle(EvaluateSectorAlertsCommand)}: call it outside any transaction, or the
 * alerts would commit after the lock is released.</p>
 * <p>The lock lives in memory: it serializes a single instance of the platform, as deployed on Render. Several
 * instances would need a database lock instead.</p>
 */
@Service
public class SectorAlertEvaluator {

    private final SectorAlertCommandService sectorAlertCommandService;
    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Creates the evaluator.
     *
     * @param sectorAlertCommandService the evaluation of a license's alerts
     * @throws IllegalArgumentException if the service is null
     */
    public SectorAlertEvaluator(SectorAlertCommandService sectorAlertCommandService) {
        if (sectorAlertCommandService == null) {
            throw new IllegalArgumentException("territory.sector_alert_evaluator.dependency.null");
        }
        this.sectorAlertCommandService = sectorAlertCommandService;
    }

    /**
     * Brings the alerts of a license up to date, after any other evaluation of the same license has finished.
     *
     * @param licenseId the license
     * @return the number of alerts opened, refreshed or normalized, or why the license could not be evaluated
     */
    public Result<Integer, ApplicationError> evaluate(LicenseId licenseId) {
        if (licenseId == null) {
            return Result.failure(ApplicationError.validationError("licenseId", "territory.sector_alert.license.null"));
        }
        var lock = locks.computeIfAbsent(licenseId.licenseId(), id -> new ReentrantLock());
        lock.lock();
        try {
            return sectorAlertCommandService.handle(new EvaluateSectorAlertsCommand(licenseId.licenseId()));
        } finally {
            lock.unlock();
        }
    }
}
