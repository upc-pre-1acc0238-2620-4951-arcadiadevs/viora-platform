-- =============================================================================================
-- US29 · Backfill of the harvest settlement receipt contract
-- File:    scripts/sql/2026-10-05-us29-settlement-receipt-backfill.sql
-- Target:  PostgreSQL. Do NOT run against H2 (the test/dev database): this script uses
--          DO blocks, split_part() and the ~ regex operator, none of which H2 understands.
--
-- WHEN TO RUN
--   Deploy `feature/settlement-settle-contract-us29` FIRST: the new columns, the two unique
--   constraints and the `harvest_receipt_counters` table only exist after the application has
--   started once with `ddl-auto=update` (section 0 fails loudly if they do not). Then run this
--   script once. It is idempotent: it can be run again, and it is safe if the application already
--   settled something in between, because every statement only touches rows that are still
--   missing the value and the counters are never lowered. Even so, keep it short and avoid
--   running it while settlements are being written.
--
-- WHY IT IS NEEDED
--   The project manages its schema with Hibernate `spring.jpa.hibernate.ddl-auto=update` (there is
--   no Flyway and no Liquibase). That ADDS the new columns and the two new unique constraints to
--   `harvest_settlements`, but it never fills the rows that already exist. After the update, every
--   settlement stored before this change has:
--       producer_id     NULL   (the owner only lived on the parent report)
--       receipt_number  NULL   (receipt numbers did not exist)
--       weighed_on      NULL   (the weighing date was not recorded)
--   This script reconstructs those three columns for those rows.
--
-- WHAT IT DOES NOT TOUCH
--   mill_ticket_number and idempotency_key stay NULL. They did not exist before this change and
--   there is nothing to reconstruct: no mill ticket can be invented, and a legacy settlement has no
--   idempotency key, so it cannot be replayed. That is why `producer_id`, `receipt_number` and
--   `weighed_on` are declared nullable in the entity and NOT NULL is only safe once this has run.
--
-- SAFETY
--   The whole script is one transaction: if any statement fails, nothing is applied. It takes no
--   lock other than the row locks of the rows it updates, so keep it short and run it while the
--   application is not writing settlements.
--
-- AFTER IT HAS RUN EVERYWHERE
--   `producer_id`, `receipt_number` and `weighed_on` can be tightened to NOT NULL, and that is a
--   separate, explicit migration. Do not do it here.
-- =============================================================================================

BEGIN;

-- ---------------------------------------------------------------------------------------------
-- 0. Preflight. Fail loudly and early instead of half-applying the backfill.
-- ---------------------------------------------------------------------------------------------
DO $$
BEGIN
    IF to_regclass('harvest_settlements') IS NULL THEN
        RAISE EXCEPTION
            'harvest_settlements does not exist. Deploy the application once with ddl-auto=update so the '
            'new columns exist, then run this script again.';
    END IF;
    IF to_regclass('harvest_receipt_counters') IS NULL THEN
        RAISE EXCEPTION
            'harvest_receipt_counters does not exist. Deploy the application once with ddl-auto=update so the '
            'table exists, then run this script again.';
    END IF;
    IF to_regclass('agronomic_reports') IS NULL THEN
        RAISE EXCEPTION 'agronomic_reports does not exist; this script cannot find the owner of a settlement.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'harvest_settlements' AND column_name = 'producer_id') THEN
        RAISE EXCEPTION
            'harvest_settlements.producer_id is missing. Run the application once with ddl-auto=update '
            'so the column exists, then run this script again.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'harvest_settlements' AND column_name = 'receipt_number') THEN
        RAISE EXCEPTION 'harvest_settlements.receipt_number is missing; run the deploy first.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'harvest_settlements' AND column_name = 'weighed_on') THEN
        RAISE EXCEPTION 'harvest_settlements.weighed_on is missing; run the deploy first.';
    END IF;
END
$$;

-- ---------------------------------------------------------------------------------------------
-- 1. Before: what is still missing. These are the numbers to compare with the ones in section 5.
-- ---------------------------------------------------------------------------------------------
SELECT count(*)                                        AS settlements_total,
       count(*) FILTER (WHERE producer_id IS NULL)      AS missing_producer_id,
       count(*) FILTER (WHERE receipt_number IS NULL)   AS missing_receipt_number,
       count(*) FILTER (WHERE weighed_on IS NULL)       AS missing_weighed_on,
       count(*) FILTER (WHERE mill_ticket_number IS NULL) AS without_mill_ticket,
       count(*) FILTER (WHERE idempotency_key IS NULL)   AS without_idempotency_key
FROM harvest_settlements;

-- ---------------------------------------------------------------------------------------------
-- 2. producer_id, copied from the parent report.
--
--    Every settlement has a NOT NULL report_id with a foreign key to agronomic_reports, and
--    agronomic_reports.producer_id is NOT NULL, so the join always finds an owner. The
--    `r.producer_id IS NOT NULL` guard is belt and braces, not a case that happens in practice.
--    The UPDATE also does not touch rows that already carry a producer.
-- ---------------------------------------------------------------------------------------------
UPDATE harvest_settlements s
SET producer_id = r.producer_id
FROM agronomic_reports r
WHERE s.report_id = r.id
  AND s.producer_id IS NULL
  AND r.producer_id IS NOT NULL;

-- ---------------------------------------------------------------------------------------------
-- 3. weighed_on, the UTC calendar date of settled_at.
--
--    `settled_at` is an Instant, stored by Hibernate as `timestamp(6) with time zone`. Casting it
--    straight to a date would use the time zone of the database session, so a settlement at
--    2026-11-04T00:30Z would be filed as 2026-11-03 in America/Argentina. `AT TIME ZONE 'UTC'`
--    converts the instant to a UTC wall-clock timestamp first, which is what the application
--    means: the weighing date is judged against LocalDate.now() on the injected UTC clock.
--
--    This is a reconstruction, not the real weighing date: nobody recorded it before this change.
--    The date the campaign was settled is the closest truthful value, and it is what the server
--    would have stored had the field existed.
-- ---------------------------------------------------------------------------------------------
UPDATE harvest_settlements
SET weighed_on = (settled_at AT TIME ZONE 'UTC')::date
WHERE weighed_on IS NULL
  AND settled_at IS NOT NULL;

-- ---------------------------------------------------------------------------------------------
-- 4. receipt_number, reconstructed as the server numbers it.
--
--    The server guarantees VR-{yy}-{nnnn}: yy is the last two digits of the campaign year and
--    nnnn is a 1-based sequence per PRODUCER AND CAMPAIGN YEAR, allocated from a locked counter.
--    The order inside a campaign is not recorded anywhere, so this reconstruction orders by
--    settled_at (with id as the tiebreaker, so two settlements stamped in the same microsecond
--    still get distinct numbers and the script is deterministic).
--
--    This is a one-time reconstruction of an invariant, not new information: no document, no
--    receipt and no mill ever printed these numbers, so nothing downstream can contradict them.
--    `lpad(..., 4, '0')` pads to four digits and lets longer numbers grow, exactly like
--    ReceiptNumber.of, which formats "%04d" and caps the sequence at 999999 so the longest value
--    is "VR-26-999999", the twelve characters the receipt_number column holds. Sequences are
--    counted per producer and campaign, so a producer would have to settle a million plots in one
--    campaign to reach the cap.
--
--    The sequence continues after the highest number already present in the same producer and
--    campaign. That matters because the application is already running when this script is
--    started (it creates the columns): any number it handed out in the meantime is the real one,
--    and the reconstruction must not repeat it, even though it cannot know its order relative to
--    the older settlements. When nothing was settled since the deploy the offset is 0, so the
--    numbering is exactly chronological.
-- ---------------------------------------------------------------------------------------------
DO $$
DECLARE
    overflowing_rows bigint;
BEGIN
    WITH ranked AS (
        SELECT s.producer_id,
               s.campaign_year,
               row_number() OVER (PARTITION BY s.producer_id, s.campaign_year
                                  ORDER BY s.settled_at, s.id) AS position_in_campaign,
               coalesce((SELECT max(split_part(existing.receipt_number, '-', 3)::int)
                         FROM harvest_settlements existing
                         WHERE existing.producer_id = s.producer_id
                           AND existing.campaign_year = s.campaign_year
                           AND existing.receipt_number ~ '^VR-[0-9]{2}-[0-9]{4,6}$'), 0) AS already_used
        FROM harvest_settlements s
        WHERE s.producer_id IS NOT NULL
          AND s.receipt_number IS NULL
    )
    SELECT count(*) INTO overflowing_rows
    FROM ranked
    WHERE already_used + position_in_campaign > 999999;

    IF overflowing_rows > 0 THEN
        -- One string literal on purpose: PL/pgSQL does not concatenate adjacent literals the way SQL does.
        RAISE EXCEPTION '% settlement(s) of one producer and campaign would need a receipt sequence above 999999, which does not fit the receipt_number column and is rejected by ReceiptNumber. Backfill them by hand or split the campaign before retrying.', overflowing_rows;
    END IF;
END
$$;

WITH ranked AS (
    SELECT s.id,
           s.campaign_year,
           row_number() OVER (PARTITION BY s.producer_id, s.campaign_year
                              ORDER BY s.settled_at, s.id) AS position_in_campaign,
           coalesce((SELECT max(split_part(existing.receipt_number, '-', 3)::int)
                     FROM harvest_settlements existing
                     WHERE existing.producer_id = s.producer_id
                       AND existing.campaign_year = s.campaign_year
                       AND existing.receipt_number ~ '^VR-[0-9]{2}-[0-9]{4,6}$'), 0) AS already_used
    FROM harvest_settlements s
    WHERE s.producer_id IS NOT NULL
      AND s.receipt_number IS NULL
)
UPDATE harvest_settlements s
SET receipt_number = 'VR-'
                    || lpad(right(ranked.campaign_year::text, 2), 2, '0')
                    || '-'
                    || lpad((ranked.already_used + ranked.position_in_campaign)::text, 4, '0')
FROM ranked
WHERE s.id = ranked.id
  AND s.receipt_number IS NULL;

-- ---------------------------------------------------------------------------------------------
-- 4b. harvest_receipt_counters, seeded from the receipt numbers that now exist.
--
--    The application hands out the next number from the counter of the producer and campaign year,
--    and opens a missing counter at 0. Without this step the first new settlement of a producer
--    whose legacy rows were numbered above would be offered VR-yy-0001 again, which the unique
--    constraint uq_settlement_producer_receipt rejects. The application also catches the counter up
--    with the stored numbers before it allocates one, so this seeding is the primary fix and that
--    catch-up the safety net.
--
--    One counter per (producer, campaign year), holding the highest sequence of that pair's receipt
--    numbers. An existing counter is never lowered: GREATEST keeps whatever the application already
--    handed out, so running this again (or after the application settled something) changes nothing
--    it should not. It runs in the same transaction as the numbering above.
--    gen_random_uuid() is built into PostgreSQL 13 and later.
-- ---------------------------------------------------------------------------------------------
INSERT INTO harvest_receipt_counters (id, producer_id, campaign_year, last_sequence)
SELECT gen_random_uuid(),
       s.producer_id,
       s.campaign_year,
       max(split_part(s.receipt_number, '-', 3)::int)
FROM harvest_settlements s
WHERE s.producer_id IS NOT NULL
  AND s.receipt_number ~ '^VR-[0-9]{2}-[0-9]{4,6}$'
GROUP BY s.producer_id, s.campaign_year
ON CONFLICT (producer_id, campaign_year) DO UPDATE
SET last_sequence = GREATEST(harvest_receipt_counters.last_sequence, EXCLUDED.last_sequence);

-- ---------------------------------------------------------------------------------------------
-- 5. After: the same counts as in section 1. The operator checks these, nothing else.
--    Expect missing_producer_id, missing_receipt_number and missing_weighed_on to be 0, and
--    without_mill_ticket / without_idempotency_key to be unchanged: they were never in scope.
-- ---------------------------------------------------------------------------------------------
SELECT count(*)                                        AS settlements_total,
       count(*) FILTER (WHERE producer_id IS NULL)      AS missing_producer_id,
       count(*) FILTER (WHERE receipt_number IS NULL)   AS missing_receipt_number,
       count(*) FILTER (WHERE weighed_on IS NULL)       AS missing_weighed_on,
       count(*) FILTER (WHERE mill_ticket_number IS NULL) AS without_mill_ticket,
       count(*) FILTER (WHERE idempotency_key IS NULL)   AS without_idempotency_key
FROM harvest_settlements;

-- The seeded counters: last_sequence must equal the highest receipt sequence of each producer and campaign.
SELECT c.producer_id,
       c.campaign_year,
       c.last_sequence,
       max(split_part(s.receipt_number, '-', 3)::int) AS highest_receipt_sequence
FROM harvest_receipt_counters c
LEFT JOIN harvest_settlements s
       ON s.producer_id = c.producer_id
      AND s.campaign_year = c.campaign_year
      AND s.receipt_number ~ '^VR-[0-9]{2}-[0-9]{4,6}$'
GROUP BY c.producer_id, c.campaign_year, c.last_sequence
ORDER BY c.producer_id, c.campaign_year;

-- The reconstructed numbers, read the way a producer would: per producer and campaign, in order.
SELECT s.producer_id,
       s.campaign_year,
       s.receipt_number,
       s.weighed_on,
       s.settled_at
FROM harvest_settlements s
ORDER BY s.producer_id, s.campaign_year, s.settled_at, s.id;

COMMIT;
