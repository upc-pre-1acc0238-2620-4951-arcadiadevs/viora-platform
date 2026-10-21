package com.arcadiadevs.viora.platform.subscription.application.queryservices;

import java.math.BigDecimal;
import java.util.List;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
/** Read contract for configured server-owned tariff. */
public interface SubscriptionCatalog {
    List<PlanOffer> plans();
    SubscriptionPrice quote(String planCode, HectaresQuota quota);
    record PlanOffer(String code, String currency, BigDecimal monthlyRatePerHa, BigDecimal taxRate,
            BigDecimal minimumQuotaHa, BigDecimal quotaIncrementHa, int billingMonths, String tariffVersion) { }
}
