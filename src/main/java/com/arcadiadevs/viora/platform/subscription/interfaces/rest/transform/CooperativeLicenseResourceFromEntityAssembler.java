package com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.subscription.application.queryservices.CooperativeLicenseQueryService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeLicenseCapacityQuery;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.CooperativeLicenseResource;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Maps a cooperative license to its public response, with the seats and hectares it still has to give.
 *
 * <p>The capacity is read through the license query service, so the controllers only deal with application
 * services.</p>
 */
@Component
public class CooperativeLicenseResourceFromEntityAssembler {

    private final CooperativeLicenseQueryService cooperativeLicenseQueryService;
    private final Clock clock;

    /**
     * Constructs the assembler.
     *
     * @param cooperativeLicenseQueryService the license reads
     * @param clock                          the application clock
     * @throws IllegalArgumentException if a dependency is null
     */
    public CooperativeLicenseResourceFromEntityAssembler(CooperativeLicenseQueryService cooperativeLicenseQueryService,
                                                         Clock clock) {
        if (cooperativeLicenseQueryService == null || clock == null) {
            throw new IllegalArgumentException("subscription.license_assembler.dependency.null");
        }
        this.cooperativeLicenseQueryService = cooperativeLicenseQueryService;
        this.clock = clock;
    }

    /**
     * Maps a license to its public response.
     *
     * @param license the license
     * @return the public resource, with its capacity
     */
    public CooperativeLicenseResource toResource(CooperativeLicense license) {
        var capacity = cooperativeLicenseQueryService
                .handle(new GetCooperativeLicenseCapacityQuery(license.snapshot().id()))
                .orElseThrow();
        return CooperativeLicenseResourceAssembler.toResource(license, capacity, clock.instant());
    }
}
