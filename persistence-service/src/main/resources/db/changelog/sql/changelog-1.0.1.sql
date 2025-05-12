-- changeset habib.machpud:create-hypertable
SELECT create_hypertable('location_events', 'timestamp', if_not_exists => TRUE);