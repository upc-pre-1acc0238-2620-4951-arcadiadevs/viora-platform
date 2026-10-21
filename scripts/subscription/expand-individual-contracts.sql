-- PostgreSQL expansion of the existing shared subscriptions table.
-- Back up the database and review the target schema before applying.
-- Does not consume codes, change sponsors, or alter existing subscription terms.
BEGIN;
ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS plan_code varchar(255),
    ADD COLUMN IF NOT EXISTS base_amount numeric(19,2),
    ADD COLUMN IF NOT EXISTS tax_amount numeric(19,2),
    ADD COLUMN IF NOT EXISTS total_amount numeric(19,2),
    ADD COLUMN IF NOT EXISTS payment_id varchar(255),
    ADD COLUMN IF NOT EXISTS requested_at timestamp with time zone;

-- Individual contracts have no entitlement dates before payment approval.
ALTER TABLE subscriptions ALTER COLUMN starts_at DROP NOT NULL;
ALTER TABLE subscriptions ALTER COLUMN ends_at DROP NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_subscriptions_payment_id ON subscriptions(payment_id);
COMMIT;
