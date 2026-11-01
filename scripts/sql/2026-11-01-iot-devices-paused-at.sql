-- =============================================================================================
-- Telemetry · nodes the producer pauses from the app keep the moment of the pause
-- File:    scripts/sql/2026-11-01-iot-devices-paused-at.sql
-- Target:  PostgreSQL (production). H2 needs nothing: tests create the schema from the entities.
--
-- WHEN TO RUN
--   Optional. spring.jpa.hibernate.ddl-auto=update adds a new nullable column on its own when the
--   release starts, so production does not need it. Run it by hand only where ddl-auto is off, before
--   deploying the release that lets the app pause a node ("Transmitir lecturas" off). It is
--   idempotent.
--
-- WHAT IT DOES
--   Adds iot_devices.paused_at: when the node was paused; NULL for every node that is not paused,
--   which is every existing node (no node could be paused before this release).
-- =============================================================================================

ALTER TABLE iot_devices ADD COLUMN IF NOT EXISTS paused_at TIMESTAMP WITH TIME ZONE;
