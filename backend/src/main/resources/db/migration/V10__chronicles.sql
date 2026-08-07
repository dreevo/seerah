-- ============================================================================
--  V10 — Chronicles. Generalises the platform from a single Seerah into a
--  library of connected chronologies (the Seerah is chronicle #1; further
--  prophetic narratives sit alongside it). Every event now belongs to exactly
--  one chronicle. The three governance invariants (§13.2/§13.4/§13.6) are
--  untouched — a chronicle only groups already-governed events.
-- ============================================================================

CREATE TABLE chronicle (
  id          uuid PRIMARY KEY,
  slug        citext NOT NULL UNIQUE,
  title       text NOT NULL,
  title_ar    text,
  subtitle    text,
  blurb       text,
  glyph       text,
  kind        text NOT NULL DEFAULT 'PROPHET',   -- SEERAH | PROPHET
  ordinal     integer NOT NULL DEFAULT 0,
  status      content_status NOT NULL DEFAULT 'PUBLISHED',
  created_at  timestamptz NOT NULL DEFAULT now()
);

-- The two seeded chronicles carry fixed ids so the seeder and any re-run agree.
INSERT INTO chronicle (id, slug, title, title_ar, subtitle, blurb, glyph, kind, ordinal) VALUES
  ('a0000000-0000-0000-0000-0000000000c1', 'seerah',
   'The Life of the Prophet Muhammad ﷺ', 'السيرة النبوية',
   'The Prophetic Biography',
   'The connected chronology of the final Messenger ﷺ — from the Year of the Elephant to the Farewell Pilgrimage — every event carried by its verses, companions, geography, and sources.',
   '۞', 'SEERAH', 0),
  ('a0000000-0000-0000-0000-0000000000c2', 'yusuf',
   'The Story of Prophet Yūsuf', 'قصة يوسف عليه السلام',
   'Aḥsan al-Qaṣaṣ — the Most Beautiful of Stories',
   'The life of Prophet Yūsuf (Joseph) عليه السلام, narrated by Allah as ‘‘the most beautiful of stories’’ — told here strictly from Sūrah Yūsuf, verse by verse.',
   '☾', 'PROPHET', 1);

-- Every event belongs to a chronicle.
ALTER TABLE event ADD COLUMN chronicle_id uuid REFERENCES chronicle(id) ON DELETE RESTRICT;
CREATE INDEX ix_event_chronicle ON event(chronicle_id);

-- Any events already present pre-date the multi-chronicle model: they are Seerah.
UPDATE event SET chronicle_id = 'a0000000-0000-0000-0000-0000000000c1' WHERE chronicle_id IS NULL;
