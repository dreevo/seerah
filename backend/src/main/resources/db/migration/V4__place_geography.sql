-- ============================================================================
--  V4 — Geography for `place`. Plain latitude/longitude (added in V2) plus a
--  modern-name label and an "approximate" flag. No PostGIS: the corpus is a
--  small, fixed set of points and the map only needs lat/lng, so a spatial
--  extension, a generated geometry column, and a GiST index would be weight
--  without a job.
-- ============================================================================
ALTER TABLE place
  ADD COLUMN modern_name    text,
  ADD COLUMN is_approximate boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN place.is_approximate IS
  'True when the location is scholarly conjecture. The map renders a shaded '
  'radius instead of a pin, and the API returns a confidence flag (§12.4).';
