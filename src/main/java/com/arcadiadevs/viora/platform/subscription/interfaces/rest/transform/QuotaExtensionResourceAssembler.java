package com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.subscription.application.queryservices.QuotaExtensionQueryService.ExtensionTerms;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtension;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.ExtensionTermsResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.QuotaExtensionResource;

/**
 * Assembler from plan extensions and their terms to public resources.
 */
public final class QuotaExtensionResourceAssembler {

    private QuotaExtensionResourceAssembler() {
    }

    /**
     * Builds the public resource of an extension.
     *
     * @param extension     the extension
     * @param plan          the plan it belongs to, read after the extension was stored
     * @param paymentStatus the latest payment attempt state, or null
     * @return the resource
     */
    public static QuotaExtensionResource toResource(QuotaExtension extension, Subscription plan, String paymentStatus) {
        var e = extension.snapshot();
        var p = plan.snapshot();
        var price = e.price();
        return new QuotaExtensionResource(
                e.id().quotaExtensionId(),
                e.subscriptionId().subscriptionId(),
                e.source().name(),
                e.status().name(),
                e.addedArea().hectares(),
                p.quota().hectares(),
                p.period() == null ? null : p.period().endsAt(),
                e.billedMonths(),
                price == null ? null : "PEN",
                price == null ? null : price.base().amount(),
                price == null ? null : price.tax().amount(),
                price == null ? null : price.total().amount(),
                e.createdAt(),
                e.appliedAt(),
                paymentStatus);
    }

    /**
     * Builds the public resource of extension terms.
     *
     * @param terms the terms
     * @return the resource
     */
    public static ExtensionTermsResource toResource(ExtensionTerms terms) {
        return new ExtensionTermsResource(terms.subscriptionId().toString(), terms.mode(), terms.extendable(),
                terms.reason(), terms.action(), terms.quotaHa(), terms.endsAt(), terms.billableMonths(),
                terms.monthlyRatePerHa(), terms.currency(), terms.minimumHa(), terms.incrementHa(),
                terms.billableDays(), terms.extensionRatePerHa());
    }
}
