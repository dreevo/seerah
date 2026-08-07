-- ============================================================================
--  V3 — People, Scripture, and the connective tissue: Relationships (§12.4-12.7)
--  This is the migration that turns a list of events into a connected graph:
--  people who took part, verses revealed around them, and typed edges between
--  any two entities.
-- ============================================================================

-- --- people (§12.4) -------------------------------------------------------
CREATE TABLE person (
  id            uuid PRIMARY KEY,
  slug          citext NOT NULL UNIQUE,
  role_type     person_role NOT NULL,
  kunya         text,
  nasab         text,
  birth_year_ce integer,
  death_year_ce integer,
  birth_year_ah integer,
  death_year_ah integer,
  honorific_key text,
  status        content_status NOT NULL DEFAULT 'DRAFT',
  version       integer NOT NULL DEFAULT 0,
  published_at  timestamptz,
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now(),
  created_by    uuid REFERENCES app_user(id),
  updated_by    uuid REFERENCES app_user(id),
  CONSTRAINT ck_person_lifespan
    CHECK (death_year_ce IS NULL OR birth_year_ce IS NULL
           OR death_year_ce >= birth_year_ce)
);
COMMENT ON TABLE person IS
  'A person in the corpus. Display names live in person_alias / translation, '
  'never on this row (§11.2).';

CREATE TABLE person_alias (
  id         uuid PRIMARY KEY,
  person_id  uuid NOT NULL REFERENCES person(id) ON DELETE CASCADE,
  alias      text NOT NULL,
  script     script_kind NOT NULL,
  locale     text,
  is_primary boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_person_alias UNIQUE (person_id, alias, script)
);
COMMENT ON TABLE person_alias IS
  'Alternate names and transliterations. Feeds search recall: a user typing '
  'Uthman, Othman, or Usman must reach the same person.';

-- --- scripture reference data (§12.7). NOT editorial content: no status, no
--     version, no review workflow. Seeded once from a vetted upstream set. ----
CREATE TABLE surah (
  number           smallint PRIMARY KEY CHECK (number BETWEEN 1 AND 114),
  name_ar          text NOT NULL,
  name_translit    text NOT NULL,
  name_en          text NOT NULL,
  revelation_place revelation_place NOT NULL,
  revelation_order smallint NOT NULL UNIQUE CHECK (revelation_order BETWEEN 1 AND 114),
  ayah_count       smallint NOT NULL CHECK (ayah_count > 0),
  has_bismillah    boolean NOT NULL DEFAULT true
);
COMMENT ON TABLE surah IS
  'Reference data. 114 rows, immutable, seeded once. Not editorial content.';

CREATE TABLE verse (
  id            uuid PRIMARY KEY,
  surah_number  smallint NOT NULL REFERENCES surah(number),
  ayah_number   smallint NOT NULL CHECK (ayah_number > 0),
  text_uthmani  text COLLATE "C" NOT NULL,
  text_simple   text COLLATE "C" NOT NULL,
  juz           smallint CHECK (juz BETWEEN 1 AND 30),
  sajdah        boolean NOT NULL DEFAULT false,
  CONSTRAINT ux_verse UNIQUE (surah_number, ayah_number)
);
COMMENT ON COLUMN verse.text_uthmani IS
  'Verbatim Uthmani script. COLLATE "C" guarantees byte-exact comparison. '
  'NEVER transform this column.';

CREATE TABLE verse_translation (
  id          uuid PRIMARY KEY,
  verse_id    uuid NOT NULL REFERENCES verse(id) ON DELETE RESTRICT,
  locale      text NOT NULL,
  translator  text NOT NULL,
  source_id   uuid REFERENCES source(id),
  text        text NOT NULL,
  footnotes   text,
  licence     text NOT NULL DEFAULT 'UNKNOWN',
  CONSTRAINT ux_verse_translation UNIQUE (verse_id, locale, translator)
);
COMMENT ON TABLE verse_translation IS
  'A rendering of meaning, attributed to a named translator. The platform '
  'never presents a translation as the Quran itself.';

-- --- relationships: the polymorphic edge table (§12.5) --------------------
CREATE TABLE relationship (
  id            uuid PRIMARY KEY,
  subject_type  entity_type NOT NULL,
  subject_id    uuid NOT NULL,
  rel_type      relationship_type NOT NULL,
  object_type   entity_type NOT NULL,
  object_id     uuid NOT NULL,
  qualifier     text,
  certainty     certainty NOT NULL DEFAULT 'REPORTED',
  is_interpretive boolean NOT NULL DEFAULT false,
  valid_from    date,
  valid_to      date,
  weight        numeric(4,3) NOT NULL DEFAULT 1.000 CHECK (weight > 0 AND weight <= 1),
  status        content_status NOT NULL DEFAULT 'DRAFT',
  version       integer NOT NULL DEFAULT 0,
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now(),
  created_by    uuid REFERENCES app_user(id),
  updated_by    uuid REFERENCES app_user(id),
  CONSTRAINT ux_relationship UNIQUE
    (subject_type, subject_id, rel_type, object_type, object_id),
  CONSTRAINT ck_rel_not_self
    CHECK (NOT (subject_type = object_type AND subject_id = object_id)),
  CONSTRAINT ck_rel_validity
    CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to >= valid_from)
);
COMMENT ON COLUMN relationship.is_interpretive IS
  'TRUE when the edge is an inference rather than a transmitted fact (CAUSED, '
  'CONTRASTS_WITH). Interpretive edges carry a stricter citation rule (§13.3).';
CREATE INDEX ix_rel_subject ON relationship (subject_type, subject_id, status);
CREATE INDEX ix_rel_object  ON relationship (object_type, object_id, status);

-- Polymorphic referential integrity by trigger (§12.5). The enum value lower-cased
-- equals the target table name — a deliberate, documented coupling.
CREATE OR REPLACE FUNCTION fn_check_entity_ref(p_type entity_type, p_id uuid)
RETURNS boolean LANGUAGE plpgsql STABLE AS $$
DECLARE ok boolean;
BEGIN
  EXECUTE format('SELECT EXISTS (SELECT 1 FROM %I WHERE id = $1)', lower(p_type::text))
    INTO ok USING p_id;
  RETURN ok;
END $$;

CREATE OR REPLACE FUNCTION trg_relationship_refs() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF NOT fn_check_entity_ref(NEW.subject_type, NEW.subject_id) THEN
    RAISE EXCEPTION 'relationship subject % % does not exist',
      NEW.subject_type, NEW.subject_id USING ERRCODE = '23503';
  END IF;
  IF NOT fn_check_entity_ref(NEW.object_type, NEW.object_id) THEN
    RAISE EXCEPTION 'relationship object % % does not exist',
      NEW.object_type, NEW.object_id USING ERRCODE = '23503';
  END IF;
  RETURN NEW;
END $$;

CREATE CONSTRAINT TRIGGER ct_relationship_refs
  AFTER INSERT OR UPDATE OF subject_id, object_id ON relationship
  DEFERRABLE INITIALLY DEFERRED
  FOR EACH ROW EXECUTE FUNCTION trg_relationship_refs();
