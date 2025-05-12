-- changeset habib.machpud:create-extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- changeset habib.machpud:create-table-location_events
CREATE TABLE IF NOT EXISTS location_events (
  user_id TEXT NOT NULL,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  timestamp TIMESTAMPTZ NOT NULL,
  labels JSONB
);