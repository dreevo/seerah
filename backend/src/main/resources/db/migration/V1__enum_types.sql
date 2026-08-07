-- ============================================================================
--  V1 — Enumerated types (§12.2)
--  Fifteen native enum types, declared first because every table depends on
--  them. Ordering within each type is significant: PostgreSQL sorts an enum by
--  declaration order, and the review queue / citation-strength comparisons
--  rely on it.
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TYPE content_status AS ENUM (
  'DRAFT', 'IN_REVIEW', 'CHANGES_REQUESTED', 'APPROVED', 'PUBLISHED', 'RETIRED');
COMMENT ON TYPE content_status IS
  'Editorial workflow state. Order is significant: the review queue sorts by it.';

CREATE TYPE entity_type AS ENUM (
  'EVENT', 'PERSON', 'PLACE', 'ROUTE', 'PERIOD', 'RELATIONSHIP', 'LESSON',
  'THEME', 'VERSE', 'HADITH', 'LEARNING_PATH', 'TAFSIR_EXCERPT');

CREATE TYPE certainty AS ENUM (
  'MUTAWATIR',        -- mass-transmitted, no serious dispute
  'WELL_ATTESTED',    -- multiple independent sound chains
  'REPORTED',         -- single sound chain
  'WEAK',             -- transmitted but graded weak
  'SCHOLARS_DIFFER',  -- substantive disagreement; must show positions
  'DISPUTED');        -- contested authenticity

CREATE TYPE date_precision AS ENUM (
  'EXACT_DAY', 'MONTH', 'SEASON', 'YEAR', 'YEAR_RANGE', 'DECADE',
  'PERIOD_ONLY', 'UNDATED');

CREATE TYPE calendar_system AS ENUM ('HIJRI', 'GREGORIAN', 'JULIAN', 'RELATIVE');

CREATE TYPE source_tier AS ENUM (
  'PRIMARY',    -- Quran, canonical hadith collections
  'CLASSICAL',  -- Ibn Ishaq/Ibn Hisham, al-Waqidi, al-Tabari, Ibn Sa'd
  'SECONDARY',  -- recognised later scholarship
  'TERTIARY');  -- modern surveys; never sufficient alone

CREATE TYPE hadith_grade AS ENUM (
  'SAHIH', 'HASAN', 'DAIF', 'MAWDU', 'UNGRADED');

CREATE TYPE hadith_collection AS ENUM (
  'BUKHARI', 'MUSLIM', 'ABU_DAWUD', 'TIRMIDHI', 'NASAI', 'IBN_MAJAH',
  'MUWATTA', 'AHMAD', 'OTHER');

CREATE TYPE person_role AS ENUM (
  'PROPHET', 'COMPANION', 'FAMILY', 'OPPONENT', 'RULER', 'SCHOLAR',
  'NARRATOR', 'OTHER');

CREATE TYPE relationship_type AS ENUM (
  'PARTICIPATED_IN', 'LED', 'OPPOSED', 'ALLIED_WITH', 'PRECEDED', 'FOLLOWED',
  'CAUSED', 'RESULTED_IN', 'OCCURRED_AT', 'TRAVELLED_TO', 'REVEALED_DURING',
  'REVEALED_ABOUT', 'NARRATED_BY', 'MARRIED_TO', 'PARENT_OF', 'CHILD_OF',
  'SIBLING_OF', 'FREED_BY', 'COMPANION_OF', 'TEACHER_OF', 'MENTIONED_IN',
  'PART_OF', 'ILLUSTRATES', 'CONTRASTS_WITH', 'SUCCEEDED');

CREATE TYPE citation_role AS ENUM (
  'SUPPORTS', 'DETAILS', 'DISPUTES', 'CONTEXTUALISES', 'PRIMARY_FOR');

CREATE TYPE revelation_place AS ENUM ('MAKKI', 'MADANI');

CREATE TYPE media_kind AS ENUM (
  'MAP', 'MANUSCRIPT_SCAN', 'PHOTOGRAPH', 'DIAGRAM', 'AUDIO', 'CALLIGRAPHY');
-- No 'PORTRAIT' or 'ILLUSTRATION_OF_PERSON'. The absence is deliberate: the
-- type system cannot name the thing the platform must not do (§12.2, §6.5).

CREATE TYPE script_kind AS ENUM ('ARABIC', 'LATIN', 'TRANSLITERATION');

CREATE TYPE review_decision AS ENUM (
  'SUBMITTED', 'APPROVED', 'CHANGES_REQUESTED', 'PUBLISHED', 'RETIRED', 'REOPENED');
