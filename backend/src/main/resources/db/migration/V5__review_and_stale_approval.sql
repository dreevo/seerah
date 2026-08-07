-- ============================================================================
--  V5 — Scholarly review, and the stale-approval check (§12.10, §13.6)
--
--  "The most important rule in the platform and the least obvious." A scholar
--  approves version N of an event; an editor then changes it; publication must
--  NOT proceed carrying an approval that was never given to what is now on the
--  page. The mechanism: an approval stores a content_hash; a trigger on the
--  transition to PUBLISHED recomputes that hash from the live content and
--  refuses unless a matching approval exists.
--
--  The hash is computed in SQL, not Java, on purpose (§13.6): the value the
--  trigger compares against is then derived from the actual state at the moment
--  of publication, whoever wrote it — a direct SQL UPDATE cannot bypass it.
-- ============================================================================

-- Append-only editorial history (§12.10).
CREATE TABLE review_action (
  id             uuid PRIMARY KEY,
  target_type    entity_type NOT NULL,
  target_id      uuid NOT NULL,
  target_version integer NOT NULL,
  decision       review_decision NOT NULL,
  from_status    content_status NOT NULL,
  to_status      content_status NOT NULL,
  actor_id       uuid NOT NULL REFERENCES app_user(id),
  comment        text,
  created_at     timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_review_transition CHECK (from_status <> to_status)
);
COMMENT ON TABLE review_action IS
  'Append-only workflow log. Never updated, never deleted (§12.10).';
CREATE INDEX ix_review_action_target ON review_action (target_type, target_id);

-- A scholar's sign-off on a specific version, fingerprinted by content_hash.
CREATE TABLE approval (
  id             uuid PRIMARY KEY,
  target_type    entity_type NOT NULL,
  target_id      uuid NOT NULL,
  target_version integer NOT NULL,
  scholar_id     uuid NOT NULL REFERENCES app_user(id),
  content_hash   bytea NOT NULL,
  scope          text NOT NULL DEFAULT 'FULL',
  note           text,
  created_at     timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_approval UNIQUE (target_type, target_id, scholar_id)
);
COMMENT ON COLUMN approval.content_hash IS
  'SHA-256 over the canonical serialisation of everything the scholar saw: the '
  'substantive event columns, its translations, and its citations. Recomputing '
  'this at publication time detects drift (§13.6).';
CREATE INDEX ix_approval_target ON approval (target_type, target_id);

-- Canonical content hash. Deliberately excludes volatile columns (status,
-- version, timestamps) so an approval survives the very act of publishing, while
-- any substantive edit — a date, a caveat, a citation — changes it.
CREATE OR REPLACE FUNCTION fn_content_hash(p_type entity_type, p_id uuid)
RETURNS bytea LANGUAGE sql STABLE AS $$
  SELECT sha256(convert_to(
    coalesce((SELECT e.slug || '|' || coalesce(e.hijri_year::text, '') || '|'
                     || coalesce(e.greg_start::text, '') || '|' || e.certainty::text || '|'
                     || e.is_major::text || '|' || e.sort_key::text
              FROM event e WHERE e.id = p_id AND p_type = 'EVENT'), '')
    || coalesce((SELECT string_agg(t.field_name || '|' || t.locale || '|' || t.value,
                                   chr(10) ORDER BY t.field_name, t.locale)
                 FROM translation t
                 WHERE t.entity_type = p_type AND t.entity_id = p_id), '')
    || coalesce((SELECT string_agg(cl.citation_id::text || '|' || cl.role::text,
                                   chr(10) ORDER BY cl.citation_id)
                 FROM citation_link cl
                 WHERE cl.target_type = p_type AND cl.target_id = p_id), '')
  , 'UTF8'));
$$;

-- Freshness gate. Fires on the transition to PUBLISHED and requires an approval
-- whose stored hash still equals the live content hash. (We match on the hash
-- rather than the row version, since the publish UPDATE itself bumps the
-- optimistic-lock version; the hash is the version-independent content fingerprint.)
CREATE OR REPLACE FUNCTION trg_publish_requires_fresh_approval()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_ok boolean;
BEGIN
  IF NEW.status <> 'PUBLISHED' OR OLD.status = 'PUBLISHED' THEN
    RETURN NEW;
  END IF;
  SELECT EXISTS (
    SELECT 1 FROM approval a
    WHERE a.target_type = 'EVENT'
      AND a.target_id = NEW.id
      AND a.content_hash = fn_content_hash('EVENT', NEW.id)
  ) INTO v_ok;
  IF NOT v_ok THEN
    RAISE EXCEPTION
      'STALE_APPROVAL: event % has no approval matching its current content; it changed after review and must be re-approved',
      NEW.id USING ERRCODE = 'P0001';
  END IF;
  RETURN NEW;
END $$;

CREATE TRIGGER ct_event_fresh_approval
  AFTER UPDATE OF status ON event
  FOR EACH ROW EXECUTE FUNCTION trg_publish_requires_fresh_approval();
