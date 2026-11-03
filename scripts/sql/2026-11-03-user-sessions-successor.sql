-- =============================================================================================
-- IAM · a rotated session remembers the session that replaced it
-- File:    scripts/sql/2026-11-03-user-sessions-successor.sql
-- Target:  PostgreSQL (production). H2 needs nothing: tests create the schema from the entities.
--
-- WHEN TO RUN
--   Optional. spring.jpa.hibernate.ddl-auto=update adds a new nullable column on its own when the
--   release starts, so production does not need it. Run it by hand only where ddl-auto is off, before
--   deploying the release that recovers lost refresh answers. It is idempotent.
--
-- WHAT IT DOES
--   Adds user_sessions.successor_session_id: the session that took the place of this one in a
--   rotation; NULL for every existing session, so a token rotated before this release that comes
--   back is still treated as reused.
-- =============================================================================================

ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS successor_session_id UUID;
