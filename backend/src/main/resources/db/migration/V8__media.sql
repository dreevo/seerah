-- ============================================================================
--  V8 — Media assets (§12.9). Maps, manuscript scans, diagrams, calligraphy,
--  audio. The visual language of the platform is geography, architecture, and
--  the written word — never a person. The media_kind enum (V1) has no value for
--  a depiction of a person: the type system cannot name the prohibited thing
--  (§6.5, §12.2). It is not sufficient alone — a MAP could still contain a
--  forbidden image — so review remains the backstop.
-- ============================================================================
CREATE TABLE media_asset (
  id              uuid PRIMARY KEY,
  s3_key          text NOT NULL UNIQUE,
  kind            media_kind NOT NULL,
  mime_type       text NOT NULL,
  byte_size       bigint NOT NULL CHECK (byte_size > 0),
  checksum_sha256 bytea NOT NULL,
  width_px        integer,
  height_px       integer,
  metadata        jsonb NOT NULL DEFAULT '{}'::jsonb,
  licence         text NOT NULL,
  attribution     text NOT NULL,
  source_url      text,
  status          content_status NOT NULL DEFAULT 'DRAFT',
  created_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid REFERENCES app_user(id),
  CONSTRAINT ux_media_checksum UNIQUE (checksum_sha256)
);
COMMENT ON COLUMN media_asset.attribution IS
  'NOT NULL with no default. An asset without attribution cannot be inserted, '
  'which is the cheapest possible enforcement of the rule (§12.9).';

-- Placement of an asset against a content entity (an event, in Phase 1).
CREATE TABLE media_link (
  id          uuid PRIMARY KEY,
  media_id    uuid NOT NULL REFERENCES media_asset(id) ON DELETE CASCADE,
  target_type entity_type NOT NULL,
  target_id   uuid NOT NULL,
  ordinal     integer NOT NULL DEFAULT 0,
  caption     text,
  created_at  timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_media_link UNIQUE (media_id, target_type, target_id)
);
CREATE INDEX ix_media_link_target ON media_link (target_type, target_id);
