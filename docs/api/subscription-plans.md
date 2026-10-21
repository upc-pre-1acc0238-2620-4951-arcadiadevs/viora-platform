# Individual subscription plan catalog

`GET /api/v1/subscription-plans` returns the available annual producer offer. It is public and creates neither a subscription nor a checkout.

The response preserves the Android catalog contract: `code`, `currency`, `monthlyRatePerHa`, `taxRate`, `minimumQuotaHa`, `quotaIncrementHa`, `billingMonths`, and `tariffVersion`.

## Configuration

Commercial values live in `application.properties`, with environment overrides:

| Environment variable | Default configuration | Meaning |
|---|---|---|
| `SUBSCRIPTION_MONTHLY_RATE` | `55` | Net PEN per hectare per month |
| `SUBSCRIPTION_TAX_RATE` | `0.18` | Fractional tax rate added to the net amount |
| `SUBSCRIPTION_TARIFF_VERSION` | `producer-annual-2026-1` | Tariff identifier |

The Java adapter has no embedded tariff defaults. The minimum and selection increment of 0.5 ha are individual purchase invariants; they do not redefine cooperative licenses or invitation-code capacities.

Annual pricing uses hectares multiplied by the configured monthly rate and twelve months. Base and tax are rounded to cents with HALF_UP; total is their sum. Exact monetary values must fit the persistence precision. Provider amounts with fractional cents are rejected instead of silently rounded.

For 0.5 ha at the default configured tariff: base 330.00 PEN, tax 59.40 PEN, total 389.40 PEN. This is an annual purchase, not recurring monthly collection.

## Scope and compatibility

This feature adds only the catalog endpoint and its pricing/query dependencies. It does not implement cooperative redemption, invitation-code issuance, subscription creation, checkout or webhooks. `IndividualHectaresQuota` is deliberately separate from the cooperative branch's `HectaresQuota` to avoid imposing purchase increments on that model.

Existing branches containing individual subscription controllers must not be merged wholesale after this feature: remove the duplicate catalog mapping and reuse these components during endpoint-specific reconstruction.

## Validation

Run `mvn test` with Java 21. `IndividualPricingTest` covers annual amounts, rounding, invalid increments, monetary precision and overflow. `ConfiguredSubscriptionCatalogTest` verifies that nondefault rates affect both advertised conditions and computed amounts. `SubscriptionPlanIntegrationTest` verifies the public response with the security filter chain and confirms request parameters cannot replace configured prices.
