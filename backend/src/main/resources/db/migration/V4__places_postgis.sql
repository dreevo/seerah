-- ============================================================================
--  V4 — Geography. Bring PostGIS online and give `place` a real point geometry
--  (§12.4). The geometry is DERIVED from latitude/longitude by the database, so
--  the JPA layer stays free of spatial types — it reads and writes plain doubles.
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE place
  ADD COLUMN modern_name    text,
  ADD COLUMN is_approximate boolean NOT NULL DEFAULT false,
  ADD COLUMN geom geography(Point, 4326)
    GENERATED ALWAYS AS (
      CASE
        WHEN latitude IS NOT NULL AND longitude IS NOT NULL
        THEN ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
      END
    ) STORED;

COMMENT ON COLUMN place.is_approximate IS
  'True when the location is scholarly conjecture. The map renders a shaded '
  'radius instead of a pin, and the API returns a confidence flag (§12.4).';

CREATE INDEX place_geom_gix ON place USING GIST (geom);
