package com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Outbound ACL service used by the Subscription context to know how many hectares a producer already registered.
 */
@Service("subscriptionExternalOrchardService")
public class ExternalOrchardService {

    private final OrchardContextFacade orchardContextFacade;

    /**
     * Creates the service.
     *
     * @param orchardContextFacade the Orchard context facade
     * @throws IllegalArgumentException if the facade is null
     */
    public ExternalOrchardService(OrchardContextFacade orchardContextFacade) {
        if (orchardContextFacade == null) {
            throw new IllegalArgumentException("subscription.orchard_facade.null");
        }
        this.orchardContextFacade = orchardContextFacade;
    }

    /**
     * Adds up the area of the active plots of a producer. Archived plots do not count.
     *
     * @param producerId the producer
     * @return the hectares, with two decimals; zero for a producer without plots
     */
    public BigDecimal registeredAreaHa(ProducerId producerId) {
        if (producerId == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return orchardContextFacade.findActivePlotIdsByProducerId(producerId.producerId()).stream()
                .map(orchardContextFacade::findPlotAreaHectares)
                .flatMap(area -> area.stream())
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
