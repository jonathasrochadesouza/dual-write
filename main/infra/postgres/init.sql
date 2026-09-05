CREATE TABLE IF NOT EXISTS orders (
    id              UUID PRIMARY KEY,
    experiment_id   UUID NOT NULL,
    customer_id     VARCHAR(64) NOT NULL,
    total           NUMERIC(12, 2) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_experiment_id ON orders (experiment_id);

CREATE TABLE IF NOT EXISTS outbox_events (
    id              UUID PRIMARY KEY,
    experiment_id   UUID NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(128) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(32) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_experiment_id ON outbox_events (experiment_id);
CREATE INDEX IF NOT EXISTS idx_outbox_events_status ON outbox_events (status);

CREATE TABLE IF NOT EXISTS experiments (
    id              UUID PRIMARY KEY,
    scenario        VARCHAR(64) NOT NULL,
    executed_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    verdict         VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_experiments_executed_at ON experiments (executed_at DESC);
