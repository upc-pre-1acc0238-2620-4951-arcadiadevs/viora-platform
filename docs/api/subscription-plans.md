# Individual subscription plan catalog

`GET /api/v1/subscription-plans` returns the available annual producer offer. It is public and creates neither a subscription nor a checkout.

The response preserves the Android catalog contract: `code`, `currency`, `monthlyRatePerHa`, `taxRate`, `minimumQuotaHa`, `quotaIncrementHa`, `billingMonths`, and `tariffVersion`.

## Configuration

Commercial values live in `application.properties`, with environment overrides:

| Environment variable | Default configuration | Meaning |
|---|---|---|
| `SUBSCRIPTION_MONTHLY_RATE` | `55` | PEN per hectare per month, IGV included |
| `SUBSCRIPTION_TAX_RATE` | `0.18` | IGV rate, used to split the IGV out of the total |
| `SUBSCRIPTION_TARIFF_VERSION` | `producer-annual-2026-2-igv-included` | Tariff identifier |

The Java adapter has no embedded tariff defaults. The minimum and selection increment of 0.5 ha are individual purchase invariants; they do not redefine cooperative licenses or invitation-code capacities.

Annual pricing uses hectares multiplied by the configured monthly rate and twelve months, rounded to cents with HALF_UP: that is the total, IGV included. The base is the total divided by one plus the IGV rate, rounded to cents, and the IGV is the difference. Exact monetary values must fit the persistence precision. Provider amounts with fractional cents are rejected instead of silently rounded.

For 0.5 ha at the default configured tariff: total 330.00 PEN, base 279.66 PEN, IGV 50.34 PEN. This is an annual purchase, not recurring monthly collection.

Adding hectares to a plan in force is prorated by the days it has left, a started day counting as a whole one: the price of one hectare is the yearly rate times the days left over the days of the plan, with six decimals (`extensionRatePerHa` in `GET /subscriptions/{id}/extension-terms`), and the total is the hectares times that rate, rounded to cents. For 3 ha with 306 days left of a 366-day plan: 660 × 306 / 366 = 551.803279 PEN per hectare, total 1655.41 PEN.

## Scope and compatibility

This feature adds only the catalog endpoint and its pricing/query dependencies. It does not implement cooperative redemption, invitation-code issuance, subscription creation, checkout or webhooks. `IndividualHectaresQuota` is deliberately separate from the cooperative branch's `HectaresQuota` to avoid imposing purchase increments on that model.

Existing branches containing individual subscription controllers must not be merged wholesale after this feature: remove the duplicate catalog mapping and reuse these components during endpoint-specific reconstruction.

## Validation

Run `mvn test` with Java 21. `IndividualPricingTest` covers annual amounts, rounding, invalid increments, monetary precision and overflow. `ConfiguredSubscriptionCatalogTest` verifies that nondefault rates affect both advertised conditions and computed amounts. `SubscriptionPlanIntegrationTest` verifies the public response with the security filter chain and confirms request parameters cannot replace configured prices.
