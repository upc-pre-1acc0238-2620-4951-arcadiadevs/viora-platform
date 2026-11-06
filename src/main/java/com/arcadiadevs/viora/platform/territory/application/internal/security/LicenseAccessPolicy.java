package com.arcadiadevs.viora.platform.territory.application.internal.security;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalIamService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Who may use the sectors of a license, and as whom: a technical manager the license they are in charge of while it is
 * in force, and a producer the license that sponsors their plan. Anyone else reaches none.
 */
@Component
public class LicenseAccessPolicy {

    /** Reason when a manager has no license in force. */
    public static final String MANAGER_WITHOUT_LICENSE = "territory.manager.no_license";
    /** Reason when a producer has no sponsored plan in force. */
    public static final String PRODUCER_NOT_SPONSORED = "territory.producer.not_sponsored";

    private static final String RESOURCE = "sector-zone";

    private final ExternalIamService externalIamService;
    private final ExternalSubscriptionService externalSubscriptionService;

    /**
     * Creates the resolver.
     *
     * @param externalIamService          the roles
     * @param externalSubscriptionService the licenses
     * @throws IllegalArgumentException if any dependency is null
     */
    public LicenseAccessPolicy(ExternalIamService externalIamService,
                                ExternalSubscriptionService externalSubscriptionService) {
        if (externalIamService == null || externalSubscriptionService == null) {
            throw new IllegalArgumentException("territory.access_policy.dependency.null");
        }
        this.externalIamService = externalIamService;
        this.externalSubscriptionService = externalSubscriptionService;
    }

    /**
     * Resolves the license of a user.
     *
     * @param userId the signed-in user
     * @return the scope, or a 403 with the reason the user reaches no license
     */
    public Result<LicenseScope, ApplicationError> resolve(UserId userId) {
        var role = externalIamService.findRole(userId).orElse("");
        if ("ROLE_TECHNICAL_MANAGER".equals(role)) {
            var managerId = new ManagerId(userId.userId());
            return externalSubscriptionService.findLicenseInChargeOf(managerId)
                    .<Result<LicenseScope, ApplicationError>>map(license ->
                            Result.success(new LicenseScope(license, managerId, null)))
                    .orElseGet(() -> forbidden(MANAGER_WITHOUT_LICENSE));
        }
        if ("ROLE_PRODUCER".equals(role)) {
            var producerId = new ProducerId(userId.userId());
            return externalSubscriptionService.findSponsoringLicense(producerId)
                    .<Result<LicenseScope, ApplicationError>>map(license ->
                            Result.success(new LicenseScope(license, null, producerId)))
                    .orElseGet(() -> forbidden(PRODUCER_NOT_SPONSORED));
        }
        return Result.failure(ApplicationError.forbidden(RESOURCE, "territory.user.not_allowed"));
    }

    /**
     * Resolves the license of a user who must be its manager.
     *
     * @param managerId the signed-in manager
     * @return the scope, or a 403 when the user is not a manager in charge of a license
     */
    public Result<LicenseScope, ApplicationError> resolveManager(ManagerId managerId) {
        var scope = resolve(new UserId(managerId.managerId()));
        if (scope.isSuccess() && !scope.success().orElseThrow().isManager()) {
            return Result.failure(ApplicationError.forbidden(RESOURCE, "territory.user.not_allowed"));
        }
        return scope;
    }

    /**
     * Resolves the license of a user who must be one of its members.
     *
     * @param producerId the signed-in producer
     * @return the scope, or a 403 when the producer is not sponsored now
     */
    public Result<LicenseScope, ApplicationError> resolveMember(ProducerId producerId) {
        var scope = resolve(new UserId(producerId.producerId()));
        if (scope.isSuccess() && scope.success().orElseThrow().isManager()) {
            return Result.failure(ApplicationError.forbidden(RESOURCE, "territory.user.not_allowed"));
        }
        return scope;
    }

    private static Result<LicenseScope, ApplicationError> forbidden(String reason) {
        var error = ApplicationError.forbidden(RESOURCE, reason);
        return Result.failure(new ApplicationError(error.code(), error.message(), error.details(),
                Map.of("reason", reason)));
    }
}
