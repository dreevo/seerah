-- ============================================================================
--  V19 — Four more chronicles from the Qur'an:
--    • Idrīs (Enoch)     — on the trunk between Ādam and Nūḥ; the man of truth
--                          raised to a high station.
--    • Ilyās (Elijah)    — of the house of Hārūn; the caller against Baʿl.
--    • al-Yasaʿ (Elisha) — his successor; named among those preferred.
--    • Dhū al-Kifl       — among the patient admitted into Allah's mercy.
--  For al-Yasaʿ and Dhū al-Kifl the Qur'an gives the honour of a name and a
--  praise, but no narrative; the chronicles say only what the text says.
-- ============================================================================

INSERT INTO chronicle (id, slug, title, title_ar, subtitle, blurb, glyph, kind, ordinal) VALUES
  ('a0000000-0000-0000-0000-0000000000d2', 'idris',
   'The Story of Prophet Idrīs', 'قصة إدريس عليه السلام',
   'Raised to a High Station',
   'The life of Prophet Idrīs (Enoch) عليه السلام — a man of truth and a prophet, patient and righteous, whom Allah raised to a high station; between Ādam and Nūḥ — from the Qur''an.',
   '✷', 'PROPHET', 17),
  ('a0000000-0000-0000-0000-0000000000d3', 'ilyas',
   'The Story of Prophet Ilyās', 'قصة إلياس عليه السلام',
   'The Caller Against Baʿl',
   'The life of Prophet Ilyās (Elijah) عليه السلام — of the house of Hārūn, sent to a people who worshipped the idol Baʿl and forsook the best of creators; a believing servant over whom peace is left among later generations — from the Qur''an.',
   '❃', 'PROPHET', 18),
  ('a0000000-0000-0000-0000-0000000000d4', 'alyasa',
   'The Story of Prophet al-Yasaʿ', 'قصة اليسع عليه السلام',
   'Among Those Preferred',
   'Prophet al-Yasaʿ (Elisha) عليه السلام — the successor of Ilyās; the Qur''an names him among the prophets preferred above the worlds and among the outstanding, though it relates no story of him — from the Qur''an.',
   '✶', 'PROPHET', 19),
  ('a0000000-0000-0000-0000-0000000000d5', 'dhulkifl',
   'The Story of Prophet Dhū al-Kifl', 'قصة ذي الكفل عليه السلام',
   'Among the Patient',
   'Prophet Dhū al-Kifl عليه السلام — named in the Qur''an with Ismāʿīl and Idrīs among the patient, admitted into Allah''s mercy, and counted among the outstanding and the righteous; the Qur''an relates no story of him — from the Qur''an.',
   '❋', 'PROPHET', 20);
