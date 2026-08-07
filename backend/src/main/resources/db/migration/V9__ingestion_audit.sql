-- ============================================================================
--  V9 — Ingestion audit (§12.10). Every batch import records a run; every row
--  the importer refuses is captured with its raw payload and the reason. "An
--  import that silently drops data is worse than one that fails."
-- ============================================================================
CREATE TABLE ingestion_run (
  id              uuid PRIMARY KEY,
  source_name     text NOT NULL,
  source_checksum bytea,
  started_at      timestamptz NOT NULL DEFAULT now(),
  finished_at     timestamptz,
  rows_read       integer NOT NULL DEFAULT 0,
  rows_ok         integer NOT NULL DEFAULT 0,
  rows_skipped    integer NOT NULL DEFAULT 0,
  outcome         text NOT NULL DEFAULT 'RUNNING',
  triggered_by    uuid REFERENCES app_user(id)
);

CREATE TABLE skip_audit (
  id         bigserial PRIMARY KEY,
  run_id     uuid NOT NULL REFERENCES ingestion_run(id) ON DELETE CASCADE,
  row_number integer,
  reason     text NOT NULL,
  payload    jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE skip_audit IS
  'Every row the importer refused, with the raw payload and the reason.';
