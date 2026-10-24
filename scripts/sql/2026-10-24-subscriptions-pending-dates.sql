-- =============================================================================================
-- Plan Productor · A purchase waiting for its payment has no dates yet
-- File:    scripts/sql/2026-10-24-subscriptions-pending-dates.sql
-- Target:  PostgreSQL (production). H2 needs nothing: tests create the schema from the entities.
--
-- WHEN TO RUN
--   As soon as possible, with any release. Until it runs, every new individual purchase fails in
--   production with 409 RESOURCE_CONFLICT ("Este elemento entra en conflicto con otro existente"
--   in the app), because the INSERT of the PENDING_PAYMENT row breaks a NOT NULL.
--   It is idempotent: dropping a NOT NULL that is already gone does nothing.
--
-- WHY
--   0.44.0 created `subscriptions` for cooperative plans only, with starts_at and ends_at NOT NULL.
--   0.46.0 added the paid individual plan, whose contract is stored as PENDING_PAYMENT without
--   dates: its year starts when Mercado Pago approves the payment. The entity made both columns
--   nullable, but the project manages its schema with `ddl-auto=update`, which adds columns and
--   never relaxes an existing NOT NULL.
--   trunk_diameter_mm went through the same change (optional trunk diameter in a sampled tree);
--   it is included in case its table was created before it became optional.
-- =============================================================================================

ALTER TABLE subscriptions ALTER COLUMN starts_at DROP NOT NULL;
ALTER TABLE subscriptions ALTER COLUMN ends_at DROP NOT NULL;
ALTER TABLE tree_sampling_records ALTER COLUMN trunk_diameter_mm DROP NOT NULL;
