-- =============================================================================================
-- P62 · The kilos removed and the crew size of a thinning become optional
-- File:    scripts/sql/2026-10-16-thinning-optional-execution-details.sql
-- Target:  PostgreSQL (production). H2 needs nothing: tests create the schema from the entities.
--
-- WHEN TO RUN
--   Before or right after deploying release 0.39.2. Until it runs, confirming a thinning without
--   kilos or crew fails in production with a NOT NULL violation, because the project manages its
--   schema with `ddl-auto=update`, which adds columns but never relaxes an existing NOT NULL.
--   It is idempotent: dropping a NOT NULL that is already gone does nothing.
--
-- WHY
--   The app records a thinning with the date, the share of fruit removed and an optional note
--   (Figma P62). The producer rarely weighs the fruit removed or counts the crew, so both values
--   are stored when given and left empty otherwise. No calculation uses them.
-- =============================================================================================

ALTER TABLE execution_confirmations ALTER COLUMN removed_kg DROP NOT NULL;
ALTER TABLE execution_confirmations ALTER COLUMN labor_crew_size DROP NOT NULL;
