package com.arcadiadevs.viora.platform.settlement.application.internal.acl;

import com.arcadiadevs.viora.platform.settlement.application.acl.CommercialSizeGradeQueryService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalThinningService;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementation of the commercial size grade port, delegating to the outbound ACL that talks to Thinning, the
 * context that owns the scale.
 */
@Service
public class CommercialSizeGradeQueryServiceImpl implements CommercialSizeGradeQueryService {

    private final ExternalThinningService externalThinningService;

    /**
     * Constructs the query service injecting the outbound Thinning ACL service.
     *
     * @param externalThinningService the outbound ACL service
     */
    public CommercialSizeGradeQueryServiceImpl(ExternalThinningService externalThinningService) {
        this.externalThinningService = externalThinningService;
    }

    @Override
    public Optional<String> gradeOf(Double fruitsPerKg) {
        return externalThinningService.findCommercialSizeGrade(fruitsPerKg);
    }
}
