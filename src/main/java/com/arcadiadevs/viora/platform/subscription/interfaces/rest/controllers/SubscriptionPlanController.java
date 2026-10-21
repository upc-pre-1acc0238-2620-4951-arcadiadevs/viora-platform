package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionCatalog;

/** Public read boundary for the configured individual producer offer. */
@RestController
@RequestMapping("/api/v1/subscription-plans")
@Tag(name = "Subscriptions")
public class SubscriptionPlanController {
    private final SubscriptionCatalog catalog;

    /**
     * Creates the catalog HTTP boundary.
     * @param catalog server-owned plan query contract
     */
    public SubscriptionPlanController(SubscriptionCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Lists the commercial conditions without creating a contract or payment.
     * @return available individual producer offers
     */
    @GetMapping
    @Operation(summary = "List individual producer subscription plans")
    public List<SubscriptionCatalog.PlanOffer> plans() {
        return catalog.plans();
    }
}
