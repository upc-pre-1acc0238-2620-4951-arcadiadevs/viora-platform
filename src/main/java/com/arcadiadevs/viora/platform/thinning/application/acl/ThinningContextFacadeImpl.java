package com.arcadiadevs.viora.platform.thinning.application.acl;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CommercialSizeScale;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of the Thinning ACL facade, publishing the commercial size scale of the context to the contexts
 * that settle a harvest and have to name the caliber of the olives they delivered.
 *
 * <p>The label comes from {@link CommercialSizeScale}, the single owner of the scale, so no consumer has to know
 * how a caliber maps onto a grade.</p>
 */
@Service
@Transactional(readOnly = true)
public class ThinningContextFacadeImpl implements ThinningContextFacade {

    @Override
    public Optional<String> findCommercialSizeGrade(Double fruitsPerKg) {
        if (fruitsPerKg == null) {
            return Optional.empty();
        }
        return Optional.of(CommercialSizeScale.gradeOf(fruitsPerKg));
    }
}
