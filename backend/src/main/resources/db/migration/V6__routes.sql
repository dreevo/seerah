-- ============================================================================
--  V6 — Routes (§12.4). The journeys of the Seerah as PostGIS linestrings: the
--  Hijrah's indirect path, campaign marches, and so on. Defaults to conjectural,
--  because almost every route in the corpus is a modern reconstruction and
--  claiming precision we do not have is a scholarly fault.
--
--  distance_km is set at insert time from ST_Length rather than as a generated
--  column, to avoid depending on the volatility class of the geography variant.
-- ============================================================================
CREATE TABLE route (
  id             uuid PRIMARY KEY,
  slug           citext NOT NULL UNIQUE,
  event_id       uuid REFERENCES event(id) ON DELETE SET NULL,
  path           geography(LineString, 4326) NOT NULL,
  distance_km    numeric(8,2),
  is_conjectural boolean NOT NULL DEFAULT true,
  status         content_status NOT NULL DEFAULT 'PUBLISHED',
  created_at     timestamptz NOT NULL DEFAULT now(),
  updated_at     timestamptz NOT NULL DEFAULT now()
);
COMMENT ON COLUMN route.is_conjectural IS
  'Defaults TRUE. Almost every route is a modern reconstruction; the map renders '
  'conjectural routes as a dashed line (§12.4).';

CREATE INDEX route_geom_gix ON route USING GIST (path);
CREATE INDEX route_event_idx ON route (event_id);
