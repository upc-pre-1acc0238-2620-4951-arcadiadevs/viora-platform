-- =============================================================================================
-- Asistente Viora · table of the daily summaries of the home screen
-- File:    scripts/sql/2026-10-31-assistant-daily-summaries.sql
-- Target:  PostgreSQL (production). H2 needs nothing: tests create the schema from the entities.
--
-- WHEN TO RUN
--   Optional, before deploying the release that adds GET /api/v1/assistant/daily-summary. The
--   project manages its schema with `ddl-auto=update`, which creates a new table on start, so the
--   release works without it. Run it to create the table with its indexes under review instead of
--   at start. It is idempotent: every statement uses IF NOT EXISTS.
--
-- WHY
--   The daily summary of Inicio has three fact cards and an `assistant_answers` row has exactly
--   two, so the summaries live in their own table (handoff 3: "necesita su propio agregado y
--   recurso"). A job writes the summary of each producer's whole field at 06:30 in Lima and the
--   endpoint writes it on demand for a plot, in English, or when the figures changed. Only the
--   summary of the day is served; rows older than viora.assistant.history-days (10) are deleted
--   once a day, and every row of a producer is deleted with its account. No existing table,
--   column or index changes.
-- =============================================================================================

CREATE TABLE IF NOT EXISTS assistant_daily_summaries (
    id            UUID          NOT NULL,
    producer_id   VARCHAR(36)   NOT NULL,
    plot_id       VARCHAR(36),
    language      VARCHAR(2)    NOT NULL,
    data_version  VARCHAR(80)   NOT NULL,
    text          VARCHAR(1000) NOT NULL,
    facts_json    VARCHAR(2000) NOT NULL,
    sources_json  VARCHAR(1000) NOT NULL,
    provider      VARCHAR(20)   NOT NULL,
    model_name    VARCHAR(80),
    fallback      BOOLEAN       NOT NULL,
    input_tokens  INTEGER,
    output_tokens INTEGER,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT assistant_daily_summaries_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_assistant_daily_summary_current
    ON assistant_daily_summaries (producer_id, plot_id, created_at);

CREATE INDEX IF NOT EXISTS ix_assistant_daily_summary_created
    ON assistant_daily_summaries (created_at);
