// ----------------------------------------------------------------------------
// Hand-crafted, self-contained cartography for the Seerah map.
//
// Everything here is in real (lat, lng) degrees so it can be projected with the
// SAME projection as an event's places and routes — the coastline, landmasses,
// mountains, and reference cities therefore sit in true geographic relation to
// wherever an event happened. Deliberately simplified ("stylised, not to scale")
// and strictly non-figural: coastlines, terrain, and the written word only.
// ----------------------------------------------------------------------------

export interface Geo { lat: number; lng: number; }

// Land masses as closed rings. Vertices along the real coast are accurate-ish;
// the far inland vertices are pushed well beyond any event frame so that, once
// projected and clipped to the viewBox, only the true coastline shows and the
// land always fills its side of the map whatever region we zoom to.

// The Arabian Peninsula — its western edge is the Red Sea (Hejaz/Tihama) coast.
export const ARABIA: Geo[] = [
  { lat: 29.6, lng: 34.9 }, // head of the Gulf of Aqaba
  { lat: 28.2, lng: 34.7 },
  { lat: 27.0, lng: 35.5 },
  { lat: 26.1, lng: 36.2 },
  { lat: 25.0, lng: 36.9 },
  { lat: 24.1, lng: 37.4 }, // Yanbu
  { lat: 23.0, lng: 38.2 },
  { lat: 22.2, lng: 38.8 },
  { lat: 21.5, lng: 39.15 }, // Jeddah
  { lat: 20.3, lng: 40.0 },
  { lat: 19.0, lng: 40.9 },
  { lat: 17.8, lng: 41.7 },
  { lat: 16.9, lng: 42.6 }, // Jizan
  { lat: 15.3, lng: 42.9 },
  { lat: 13.6, lng: 43.3 }, // approach to Bab-el-Mandeb
  { lat: 12.8, lng: 43.9 }, // south-west Yemen corner
  { lat: 12.6, lng: 47.0 }, // south coast (off-frame)
  { lat: 15.0, lng: 52.0 }, // far south-east (off-frame)
  { lat: 25.0, lng: 57.0 }, // far east (off-frame)
  { lat: 31.0, lng: 48.0 }, // far north-east (off-frame)
  { lat: 32.3, lng: 39.0 }, // northern desert (off-frame)
  { lat: 31.4, lng: 36.6 },
  { lat: 30.4, lng: 35.4 },
  { lat: 29.6, lng: 34.9 },
];

// The Horn of Africa / Red Sea African shore (Sudan, Eritrea, Djibouti, Egypt).
export const AFRICA: Geo[] = [
  { lat: 29.2, lng: 32.6 }, // Suez (off-frame north)
  { lat: 27.8, lng: 33.6 },
  { lat: 26.0, lng: 34.3 },
  { lat: 24.0, lng: 35.4 },
  { lat: 22.0, lng: 36.6 },
  { lat: 19.6, lng: 37.2 }, // Port Sudan
  { lat: 18.0, lng: 38.4 },
  { lat: 15.6, lng: 39.6 }, // Massawa
  { lat: 13.8, lng: 41.8 },
  { lat: 12.6, lng: 43.3 }, // Bab-el-Mandeb (Djibouti)
  { lat: 11.5, lng: 42.5 },
  { lat: 8.0, lng: 40.0 }, // inland (off-frame south)
  { lat: 5.0, lng: 33.0 }, // far south-west (off-frame)
  { lat: 20.0, lng: 24.0 }, // far west (off-frame)
  { lat: 31.0, lng: 27.0 }, // far north-west (off-frame, Egypt)
  { lat: 30.2, lng: 32.3 },
  { lat: 29.2, lng: 32.6 },
];

// The Levant / Sinai (so the Isra event's Jerusalem sits on land, not sea).
export const LEVANT: Geo[] = [
  { lat: 33.6, lng: 35.2 }, // Lebanese coast (off-frame north)
  { lat: 32.6, lng: 34.9 },
  { lat: 31.6, lng: 34.5 }, // Gaza
  { lat: 30.6, lng: 34.3 }, // north Sinai coast
  { lat: 29.6, lng: 34.9 }, // Gulf of Aqaba (shared with Arabia)
  { lat: 30.6, lng: 35.6 },
  { lat: 32.0, lng: 36.6 },
  { lat: 34.0, lng: 38.0 }, // inland Syria (off-frame)
  { lat: 34.6, lng: 36.0 },
  { lat: 33.6, lng: 35.2 },
];

// The Sinai peninsula — the wedge of land between the Gulf of Suez (west) and
// the Gulf of Aqaba (east). Without it, Mount Sinai / the Exodus route would
// float in open water.
export const SINAI: Geo[] = [
  { lat: 31.2, lng: 32.3 }, // NW, Mediterranean coast near Suez (off-frame)
  { lat: 31.1, lng: 34.3 }, // NE, toward Gaza / the Negev
  { lat: 29.5, lng: 34.9 }, // head of the Gulf of Aqaba
  { lat: 28.0, lng: 34.3 }, // Ras Muhammad (south tip)
  { lat: 29.4, lng: 32.7 }, // Gulf of Suez side
  { lat: 30.6, lng: 32.35 }, // toward Suez
  { lat: 31.2, lng: 32.3 },
];

// The Fertile Crescent — Mesopotamia (Iraq) and inland Syria up to the
// mountains of the north — so the chronicles of Ibrāhīm (Ur, Babylon) and Nūḥ
// (Mount Jūdī) sit on land rather than in open water.
export const FERTILE_CRESCENT: Geo[] = [
  { lat: 37.2, lng: 42.3 }, // Mount Jūdī / the northern mountains
  { lat: 37.0, lng: 44.8 },
  { lat: 35.0, lng: 46.6 }, // Zagros foothills (east)
  { lat: 32.0, lng: 47.6 },
  { lat: 30.1, lng: 48.3 }, // head of the Gulf (Basra)
  { lat: 29.9, lng: 47.6 },
  { lat: 30.8, lng: 44.0 }, // inland desert (overlaps Arabia's north — both land)
  { lat: 32.5, lng: 40.5 },
  { lat: 33.6, lng: 38.5 }, // Syrian desert
  { lat: 35.0, lng: 37.5 },
  { lat: 36.5, lng: 37.0 }, // north Syria
  { lat: 37.2, lng: 42.3 },
];

export const LANDMASSES: Geo[][] = [ARABIA, AFRICA, LEVANT, SINAI, FERTILE_CRESCENT];

// Faint ridge lines standing in for the Hejaz (Sarawat) mountains that run
// parallel to the Red Sea coast — drawn as soft chevrons for a sense of relief.
export const RIDGES: Geo[][] = [
  [
    { lat: 25.2, lng: 38.6 }, { lat: 24.3, lng: 39.4 }, { lat: 23.3, lng: 39.8 },
    { lat: 22.2, lng: 40.1 }, { lat: 21.3, lng: 40.3 }, { lat: 20.2, lng: 40.7 },
    { lat: 19.0, lng: 41.4 }, { lat: 17.8, lng: 42.2 },
  ],
];

// Reference cities drawn faintly for orientation whenever they fall in frame,
// so even a single-place event sits inside a recognisable region.
export interface RefCity extends Geo { name: string; minor?: boolean; }
// `minor` towns only surface their label once the reader has zoomed in, so the
// map stays uncluttered at a glance but rewards exploration ("see what's around").
export const REF_CITIES: RefCity[] = [
  { name: 'Makkah', lat: 21.4225, lng: 39.8262 },
  { name: 'Madinah', lat: 24.4686, lng: 39.6142 },
  { name: "Ta'if", lat: 21.2703, lng: 40.4158 },
  { name: 'Yanbu', lat: 24.09, lng: 38.06 },
  { name: 'Khaybar', lat: 25.7, lng: 39.29 },
  { name: 'Tabuk', lat: 28.38, lng: 36.57 },
  { name: 'Najran', lat: 17.49, lng: 44.13 },
  { name: 'Jeddah', lat: 21.49, lng: 39.19, minor: true },
  { name: 'Quba', lat: 24.44, lng: 39.62, minor: true },
  { name: 'Rabigh', lat: 22.8, lng: 39.03, minor: true },
  { name: 'Dhul-Hulayfa', lat: 24.41, lng: 39.53, minor: true },
  { name: 'Al-Jar', lat: 24.66, lng: 38.36, minor: true },
  { name: "Dumat al-Jandal", lat: 29.81, lng: 39.86, minor: true },
  { name: 'Sanaa', lat: 15.37, lng: 44.19, minor: true },
  // Levant & Egypt — orientation for the prophets' chronicles (Yūsuf, Mūsā).
  { name: 'Jerusalem', lat: 31.78, lng: 35.23 },
  { name: 'Memphis', lat: 29.84, lng: 31.25 },
  { name: 'Gaza', lat: 31.5, lng: 34.47, minor: true },
  { name: 'Petra', lat: 30.33, lng: 35.44, minor: true },
  { name: 'Thebes', lat: 25.7, lng: 32.6, minor: true },
  // Mesopotamia & Syria — orientation for Ibrāhīm and Nūḥ.
  { name: 'Babylon', lat: 32.54, lng: 44.42 },
  { name: 'Ur', lat: 30.96, lng: 46.1 },
  { name: 'Harran', lat: 36.86, lng: 39.03, minor: true },
  { name: 'Damascus', lat: 33.51, lng: 36.29, minor: true },
  { name: 'Basra', lat: 30.5, lng: 47.8, minor: true },
];

export type PlaceKind =
  | 'sanctuary' | 'holy' | 'city' | 'fortress'
  | 'cave' | 'mountain' | 'battle' | 'waypoint';

// Infer an icon kind from the place's slug/name (the corpus has no kind field).
export function placeKind(slug: string | null | undefined, name: string): PlaceKind {
  const s = (slug ?? '').toLowerCase();
  const n = (name ?? '').toLowerCase();
  if (s.includes('cave') || n.includes('cave')) return 'cave';
  if (s === 'makkah' || n.includes('makkah') || n.includes('mecca')) return 'sanctuary';
  if (s === 'jerusalem' || n.includes('jerusalem')) return 'holy';
  if (s.includes('mount') || s === 'arafat' || n.includes('mount') || n.includes('arafat')) return 'mountain';
  if (s.includes('khaybar')) return 'fortress';
  if (['badr', 'hunayn', 'mutah', 'trench', 'khandaq'].some((k) => s.includes(k))) return 'battle';
  if (['madinah', 'taif', "ta'if", 'tabuk', 'najran', 'yanbu', 'jeddah'].some((k) => s.includes(k))) return 'city';
  return 'waypoint';
}
