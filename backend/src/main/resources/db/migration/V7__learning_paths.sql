-- ============================================================================
--  V7 — Learning paths (§12.8). Curated, ordered journeys through the corpus —
--  the "Guided Journeys" of the product blueprint. A path is a sequence of steps,
--  each pointing at an entity (an event, in Phase 1); its title and blurb live in
--  translation, like all human-readable text (§11.2).
-- ============================================================================
CREATE TABLE learning_path (
  id           uuid PRIMARY KEY,
  slug         citext NOT NULL UNIQUE,
  audience     text NOT NULL DEFAULT 'GENERAL',
  est_minutes  smallint,
  status       content_status NOT NULL DEFAULT 'DRAFT',
  version      integer NOT NULL DEFAULT 0,
  published_at timestamptz,
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE learning_path_step (
  id          uuid PRIMARY KEY,
  path_id     uuid NOT NULL REFERENCES learning_path(id) ON DELETE CASCADE,
  ordinal     integer NOT NULL,
  target_type entity_type NOT NULL,
  target_id   uuid NOT NULL,
  prompt      text,
  CONSTRAINT ux_lps_ordinal UNIQUE (path_id, ordinal) DEFERRABLE INITIALLY DEFERRED
);
CREATE INDEX ix_lps_path ON learning_path_step (path_id, ordinal);
