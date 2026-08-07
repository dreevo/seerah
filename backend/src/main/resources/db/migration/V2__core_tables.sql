-- ============================================================================
--  V2 — Core tables for the Phase 1 vertical slice (§12.3 – §12.10)
--  Ported faithfully from the Technical Design Record. This migration carries
--  the tables the `content` and `provenance` contexts need end-to-end, plus
--  the reference tables they depend on and the transactional outbox.
-- ============================================================================

-- --- identity (minimal; owned by the identity module) --------------------
CREATE TABLE app_user (
  id            uuid PRIMARY KEY,
  email         citext NOT NULL UNIQUE,
  display_name  text NOT NULL,
  created_at    timestamptz NOT NULL DEFAULT now()
);

-- --- chronology reference (§12.3) ----------------------------------------
CREATE TABLE period (
  id          uuid PRIMARY KEY,
  slug        citext NOT NULL UNIQUE,
  start_year_ce integer,
  end_year_ce   integer,
  ordinal     integer NOT NULL DEFAULT 0,
  status      content_status NOT NULL DEFAULT 'DRAFT',
  created_at  timestamptz NOT NULL DEFAULT now()
);

-- --- place reference (§12.4; PostGIS geometry deferred, see §12 note) -----
CREATE TABLE place (
  id          uuid PRIMARY KEY,
  slug        citext NOT NULL UNIQUE,
  place_type  text NOT NULL DEFAULT 'SETTLEMENT',
  latitude    double precision,
  longitude   double precision,
  status      content_status NOT NULL DEFAULT 'DRAFT',
  created_at  timestamptz NOT NULL DEFAULT now()
);

-- --- provenance (§12.6) ---------------------------------------------------
CREATE TABLE source (
  id           uuid PRIMARY KEY,
  slug         citext NOT NULL UNIQUE,
  work_title   text NOT NULL,
  work_title_ar text,
  author       text,
  author_ar    text,
  death_year_ah integer,
  tier         source_tier NOT NULL,
  edition      text,
  publisher    text,
  publication_year integer,
  isbn         text,
  licence      text NOT NULL DEFAULT 'UNKNOWN',
  licence_url  text,
  is_quotable  boolean NOT NULL DEFAULT false,
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz NOT NULL DEFAULT now()
);
COMMENT ON COLUMN source.is_quotable IS
  'Whether verbatim excerpts may be displayed. FALSE for in-copyright '
  'translations we may cite but not reproduce.';

CREATE TABLE citation (
  id          uuid PRIMARY KEY,
  source_id   uuid NOT NULL REFERENCES source(id) ON DELETE RESTRICT,
  locator     text NOT NULL,
  locator_kind text NOT NULL DEFAULT 'PAGE',
  quote       text,
  quote_ar    text,
  grade       hadith_grade,
  note        text,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now(),
  created_by  uuid REFERENCES app_user(id)
);
-- A UNIQUE constraint cannot hold an expression; coalesce() forces a unique index.
CREATE UNIQUE INDEX ux_citation ON citation (source_id, locator, coalesce(quote, ''));

CREATE TABLE citation_link (
  id          uuid PRIMARY KEY,
  citation_id uuid NOT NULL REFERENCES citation(id) ON DELETE CASCADE,
  target_type entity_type NOT NULL,
  target_id   uuid NOT NULL,
  role        citation_role NOT NULL DEFAULT 'SUPPORTS',
  field_name  text,
  ordinal     integer NOT NULL DEFAULT 0,
  created_at  timestamptz NOT NULL DEFAULT now(),
  created_by  uuid REFERENCES app_user(id)
);
COMMENT ON TABLE citation_link IS
  'Many-to-many between a citation and anything it supports. field_name allows '
  'a citation to support one specific claim within an entity.';
CREATE UNIQUE INDEX ux_citation_link
  ON citation_link (citation_id, target_type, target_id, coalesce(field_name, ''));
CREATE INDEX ix_citation_link_target ON citation_link (target_type, target_id);

CREATE TABLE scholarly_position (
  id            uuid PRIMARY KEY,
  target_type   entity_type NOT NULL,
  target_id     uuid NOT NULL,
  position_key  text NOT NULL,
  held_by       text NOT NULL,
  summary       text NOT NULL,
  citation_id   uuid REFERENCES citation(id) ON DELETE RESTRICT,
  ordinal       integer NOT NULL DEFAULT 0,
  status        content_status NOT NULL DEFAULT 'DRAFT',
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_scholarly_position
    UNIQUE (target_type, target_id, position_key, held_by)
);
COMMENT ON TABLE scholarly_position IS
  'One row per distinct scholarly view on a disputed point. Required in pairs '
  'or more when certainty = SCHOLARS_DIFFER (§13.4).';
CREATE INDEX ix_scholarly_position_target ON scholarly_position (target_type, target_id);

-- --- content: the central Event aggregate (§12.3) ------------------------
CREATE TABLE event (
  id              uuid PRIMARY KEY,
  slug            citext NOT NULL UNIQUE,
  period_id       uuid REFERENCES period(id) ON DELETE RESTRICT,
  place_id        uuid REFERENCES place(id) ON DELETE RESTRICT,
  -- Chronology. Both calendars stored; never converted on read.
  calendar        calendar_system NOT NULL DEFAULT 'HIJRI',
  hijri_year      integer,
  hijri_month     smallint CHECK (hijri_month BETWEEN 1 AND 12),
  hijri_day       smallint CHECK (hijri_day BETWEEN 1 AND 30),
  greg_start      date,
  greg_end        date,
  date_precision  date_precision NOT NULL DEFAULT 'YEAR',
  date_note       text,
  -- Editorial
  certainty       certainty NOT NULL DEFAULT 'REPORTED',
  status          content_status NOT NULL DEFAULT 'DRAFT',
  is_major        boolean NOT NULL DEFAULT false,
  sort_key        integer NOT NULL DEFAULT 0,
  version         integer NOT NULL DEFAULT 0,
  published_at    timestamptz,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid REFERENCES app_user(id),
  updated_by      uuid REFERENCES app_user(id),
  CONSTRAINT ck_event_greg_range
    CHECK (greg_end IS NULL OR greg_start IS NULL OR greg_end >= greg_start),
  CONSTRAINT ck_event_undated_has_period
    CHECK (date_precision <> 'UNDATED' OR period_id IS NOT NULL),
  CONSTRAINT ck_event_dated_has_date
    CHECK (date_precision = 'UNDATED' OR hijri_year IS NOT NULL
           OR greg_start IS NOT NULL),
  CONSTRAINT ck_event_published_has_date
    CHECK (status <> 'PUBLISHED' OR published_at IS NOT NULL)
);
COMMENT ON TABLE event IS
  'Central content aggregate. One row per historical event. Titles and '
  'summaries live in translation, never on this table (§11.2).';
COMMENT ON COLUMN event.sort_key IS
  'Manual tie-breaker for events sharing a date. The timeline orders by '
  '(greg_start, sort_key, id) so ordering is total.';

-- Composition: a campaign decomposes into ordered sub-events (§12.3).
CREATE TABLE narrative_sequence (
  id               uuid PRIMARY KEY,
  parent_event_id  uuid NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  child_event_id   uuid NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  ordinal          integer NOT NULL,
  created_at       timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_narrseq UNIQUE (parent_event_id, child_event_id),
  CONSTRAINT ux_narrseq_ord UNIQUE (parent_event_id, ordinal)
    DEFERRABLE INITIALLY DEFERRED,
  CONSTRAINT ck_narrseq_no_self CHECK (parent_event_id <> child_event_id)
);

-- --- localisation: all human-readable strings live here (§11.2, §12.9) ---
CREATE TABLE translation (
  id           uuid PRIMARY KEY,
  entity_type  entity_type NOT NULL,
  entity_id    uuid NOT NULL,
  field_name   text NOT NULL,
  locale       text NOT NULL CHECK (locale ~ '^[a-z]{2}(-[A-Z]{2})?$'),
  value        text NOT NULL,
  is_machine   boolean NOT NULL DEFAULT false,
  status       content_status NOT NULL DEFAULT 'DRAFT',
  translator_id uuid REFERENCES app_user(id),
  reviewed_by  uuid REFERENCES app_user(id),
  reviewed_at  timestamptz,
  version      integer NOT NULL DEFAULT 0,
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_translation UNIQUE (entity_type, entity_id, field_name, locale),
  CONSTRAINT ck_translation_machine_not_published
    CHECK (NOT (is_machine AND status = 'PUBLISHED' AND reviewed_by IS NULL))
);
COMMENT ON TABLE translation IS
  'All human-readable strings for all content entities (§11.2).';
CREATE INDEX ix_translation_entity ON translation (entity_type, entity_id, locale);

CREATE TABLE slug_alias (
  id          uuid PRIMARY KEY,
  entity_type entity_type NOT NULL,
  entity_id   uuid NOT NULL,
  old_slug    citext NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_slug_alias UNIQUE (entity_type, old_slug)
);

-- --- transactional outbox (§25.5) ----------------------------------------
CREATE TABLE outbox_event (
  id             bigserial PRIMARY KEY,
  aggregate_type text NOT NULL,
  aggregate_id   uuid NOT NULL,
  event_type     text NOT NULL,
  payload        jsonb NOT NULL,
  trace_id       text,
  occurred_at    timestamptz NOT NULL DEFAULT now(),
  published_at   timestamptz
);
COMMENT ON TABLE outbox_event IS
  'Transactional outbox. In Phase 1 the relay is a housekeeping job; from the '
  'search phase Debezium tails this table via logical decoding (§25.5).';
CREATE INDEX ix_outbox_occurred_at ON outbox_event (occurred_at);
