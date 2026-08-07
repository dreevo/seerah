-- ============================================================================
--  V6 — Routes (§12.4). The journeys of the corpus (the Hijrah's indirect path,
--  campaign marches, the Exodus) as an ordered list of lat/lng points in a child
--  table — no PostGIS linestring. distance_km is computed in Java (haversine) at
--  insert time. Defaults to conjectural, because almost every route is a modern
--  reconstruction and claiming precision we do not have is a scholarly fault.
-- ============================================================================
CREATE TABLE route (
  id             uuid PRIMARY KEY,
  slug           citext NOT NULL UNIQUE,
  event_id       uuid REFERENCES event(id) ON DELETE SET NULL,
  distance_km    numeric(8,2),
  is_conjectural boolean NOT NULL DEFAULT true,
  status         content_status NOT NULL DEFAULT 'PUBLISHED',
  created_at     timestamptz NOT NULL DEFAULT now(),
  updated_at     timestamptz NOT NULL DEFAULT now()
);
COMMENT ON COLUMN route.is_conjectural IS
  'Defaults TRUE. Almost every route is a modern reconstruction; the map renders '
  'conjectural routes as a dashed line (§12.4).';

CREATE TABLE route_point (
  route_id uuid NOT NULL REFERENCES route(id) ON DELETE CASCADE,
  ordinal  int  NOT NULL,
  lat      double precision NOT NULL,
  lng      double precision NOT NULL,
  PRIMARY KEY (route_id, ordinal)
);

CREATE INDEX route_event_idx ON route (event_id);
