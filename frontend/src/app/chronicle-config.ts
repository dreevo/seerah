import { TimelineItem } from './models';

// Per-chronicle "era" definitions. The Seerah divides by Gregorian year; a
// prophet's story with no attested dates (Yūsuf) divides by narrative phase
// (event position). Each era knows how to test whether an event belongs to it,
// so the timeline can draw period bands and offer period filters for any
// chronicle from the same code.
export interface Era {
  name: string;
  cls: string; // e0 | e1 | e2 — reuses the existing band palette
  test: (item: TimelineItem, index: number, total: number) => boolean;
}

const CONFIGS: Record<string, Era[]> = {
  seerah: [
    { name: 'Before Prophethood', cls: 'e0', test: (i) => (i.gregYear ?? 0) < 610 },
    { name: 'The Meccan Period', cls: 'e1', test: (i) => (i.gregYear ?? 0) >= 610 && (i.gregYear ?? 0) < 622 },
    { name: 'The Medinan Period', cls: 'e2', test: (i) => (i.gregYear ?? 0) >= 622 },
  ],
  yusuf: [
    { name: 'In Canaan', cls: 'e0', test: (_i, idx) => idx < 3 },
    { name: 'Trials in Egypt', cls: 'e1', test: (_i, idx) => idx >= 3 && idx < 9 },
    { name: 'Power & Reunion', cls: 'e2', test: (_i, idx) => idx >= 9 },
  ],
  musa: [
    { name: 'In Egypt & Midian', cls: 'e0', test: (_i, idx) => idx < 7 },
    { name: 'Confronting Pharaoh', cls: 'e1', test: (_i, idx) => idx >= 7 && idx < 12 },
    { name: 'Sinai & the Wilderness', cls: 'e2', test: (_i, idx) => idx >= 12 },
  ],
  ibrahim: [
    { name: 'In Babylon', cls: 'e0', test: (_i, idx) => idx < 5 },
    { name: 'The Migration', cls: 'e1', test: (_i, idx) => idx >= 5 && idx < 7 },
    { name: 'Makkah & the Kaaba', cls: 'e2', test: (_i, idx) => idx >= 7 },
  ],
  nuh: [
    { name: 'The Long Call', cls: 'e0', test: (_i, idx) => idx < 4 },
    { name: 'The Ark & the Flood', cls: 'e1', test: (_i, idx) => idx >= 4 && idx < 8 },
    { name: 'After the Flood', cls: 'e2', test: (_i, idx) => idx >= 8 },
  ],
  isa: [
    { name: 'Birth & the Cradle', cls: 'e0', test: (_i, idx) => idx < 3 },
    { name: 'The Message & Signs', cls: 'e1', test: (_i, idx) => idx >= 3 && idx < 8 },
    { name: 'Raised & the Return', cls: 'e2', test: (_i, idx) => idx >= 8 },
  ],
  adam: [
    { name: 'Creation & the Garden', cls: 'e0', test: (_i, idx) => idx < 4 },
    { name: 'The Fall', cls: 'e1', test: (_i, idx) => idx >= 4 && idx < 7 },
    { name: 'On Earth', cls: 'e2', test: (_i, idx) => idx >= 7 },
  ],
  dawud: [
    { name: 'Kingship', cls: 'e0', test: (_i, idx) => idx < 2 },
    { name: 'Gifts & Scripture', cls: 'e1', test: (_i, idx) => idx >= 2 && idx < 4 },
    { name: 'The Test & Justice', cls: 'e2', test: (_i, idx) => idx >= 4 },
  ],
  sulayman: [
    { name: 'The Inheritance', cls: 'e0', test: (_i, idx) => idx < 3 },
    { name: 'Sheba', cls: 'e1', test: (_i, idx) => idx >= 3 && idx < 6 },
    { name: 'The Death', cls: 'e2', test: (_i, idx) => idx >= 6 },
  ],
  yunus: [
    { name: 'The Flight', cls: 'e0', test: (_i, idx) => idx < 2 },
    { name: 'The Fish', cls: 'e1', test: (_i, idx) => idx >= 2 && idx < 4 },
    { name: 'The Return', cls: 'e2', test: (_i, idx) => idx >= 4 },
  ],
  hud: [
    { name: 'The Call', cls: 'e0', test: (_i, idx) => idx < 2 },
    { name: 'The Defiance', cls: 'e1', test: (_i, idx) => idx >= 2 && idx < 4 },
    { name: 'The Wind', cls: 'e2', test: (_i, idx) => idx >= 4 },
  ],
  salih: [
    { name: 'The Call & the Sign', cls: 'e0', test: (_i, idx) => idx < 2 },
    { name: 'The Betrayal', cls: 'e1', test: (_i, idx) => idx >= 2 && idx < 4 },
    { name: 'The Blast', cls: 'e2', test: (_i, idx) => idx >= 4 },
  ],
  lut: [
    { name: 'The Call', cls: 'e0', test: (_i, idx) => idx < 2 },
    { name: 'The Guests', cls: 'e1', test: (_i, idx) => idx >= 2 && idx < 4 },
    { name: 'The Overturning', cls: 'e2', test: (_i, idx) => idx >= 4 },
  ],
  shuayb: [
    { name: 'The Call', cls: 'e0', test: (_i, idx) => idx < 2 },
    { name: 'The Defiance', cls: 'e1', test: (_i, idx) => idx >= 2 && idx < 4 },
    { name: 'The Earthquake', cls: 'e2', test: (_i, idx) => idx >= 4 },
  ],
  ayyub: [
    { name: 'The Affliction', cls: 'e0', test: (_i, idx) => idx < 2 },
    { name: 'The Healing', cls: 'e1', test: (_i, idx) => idx >= 2 && idx < 3 },
    { name: 'Restoration', cls: 'e2', test: (_i, idx) => idx >= 3 },
  ],
  zakariyya: [
    { name: 'Maryam & the Prayer', cls: 'e0', test: (_i, idx) => idx < 2 },
    { name: 'Glad Tidings', cls: 'e1', test: (_i, idx) => idx >= 2 && idx < 3 },
    { name: 'The Sign', cls: 'e2', test: (_i, idx) => idx >= 3 },
  ],
  yahya: [
    { name: 'The Gift', cls: 'e0', test: (_i, idx) => idx < 2 },
    { name: 'The Character', cls: 'e1', test: (_i, idx) => idx >= 2 && idx < 3 },
    { name: 'The Peace', cls: 'e2', test: (_i, idx) => idx >= 3 },
  ],
  idris: [
    { name: 'Truth & Prophethood', cls: 'e0', test: (_i, idx) => idx < 1 },
    { name: 'The High Station', cls: 'e1', test: (_i, idx) => idx >= 1 && idx < 2 },
    { name: 'Among the Patient', cls: 'e2', test: (_i, idx) => idx >= 2 },
  ],
  ilyas: [
    { name: 'The Message', cls: 'e0', test: (_i, idx) => idx < 2 },
    { name: 'The Denial', cls: 'e1', test: (_i, idx) => idx >= 2 && idx < 3 },
    { name: 'The Peace', cls: 'e2', test: (_i, idx) => idx >= 3 },
  ],
  alyasa: [
    { name: 'Preferred', cls: 'e0', test: (_i, idx) => idx < 1 },
    { name: 'Outstanding', cls: 'e2', test: (_i, idx) => idx >= 1 },
  ],
  dhulkifl: [
    { name: 'The Patient', cls: 'e0', test: (_i, idx) => idx < 1 },
    { name: 'The Outstanding', cls: 'e2', test: (_i, idx) => idx >= 1 },
  ],
};

export function erasFor(slug: string | null | undefined): Era[] {
  return CONFIGS[slug ?? ''] ?? [];
}
