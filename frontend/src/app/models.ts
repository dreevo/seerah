// Response shapes served by the backend BFF (see PublicViews.java).

export interface ChronicleItem {
  slug: string;
  title: string;
  titleAr: string | null;
  subtitle: string | null;
  blurb: string | null;
  glyph: string | null;
  kind: string;
  ordinal: number;
  eventCount: number;
}

export interface TimelineItem {
  id: string;
  slug: string;
  title: string;
  hijriYear: number | null;
  gregYear: number | null;
  certainty: string;
  major: boolean;
  /** True when no source fixes this event's timing — shown off the dated spine. */
  undated: boolean;
}

export interface RelatedPerson {
  id: string;
  slug: string;
  name: string;
  nameArabic: string | null;
  role: string;
  relation: string;
}

export interface RelatedVerse {
  reference: string;
  surahNameEn: string;
  surahNameAr: string;
  textUthmani: string;
  translation: string | null;
  translator: string | null;
  relation: string;
}

export interface RelatedEventItem {
  id: string;
  slug: string;
  title: string;
  relation: string;
}

export interface RelatedPlace {
  id: string;
  slug: string;
  name: string;
  modernName: string | null;
  latitude: number | null;
  longitude: number | null;
  approximate: boolean;
  relation: string;
}

export interface MapPoint {
  lat: number;
  lng: number;
}

export interface RouteLine {
  slug: string;
  conjectural: boolean;
  distanceKm: number | null;
  points: MapPoint[];
}

export interface SourceItem {
  workTitle: string;
  tier: string;
  locator: string;
  quote: string | null;
  quoteAr: string | null;
  /** Isnād narrators, collector-ward first → Companion last; null when not a bundled ḥadīth or unverified. */
  chain: string[] | null;
  grade: string | null;
}

export interface EventDetail {
  id: string;
  slug: string;
  title: string;
  summary: string | null;
  why: string | null;
  certainty: string;
  hijriYear: number | null;
  gregYear: number | null;
  major: boolean;
  chronicleSlug: string | null;
  chronicleTitle: string | null;
  people: RelatedPerson[];
  verses: RelatedVerse[];
  places: RelatedPlace[];
  routes: RouteLine[];
  relatedEvents: RelatedEventItem[];
  sources: SourceItem[];
}

export interface PersonListItem {
  id: string;
  slug: string;
  name: string;
  nameArabic: string | null;
  role: string;
  deathYearAh: number | null;
}

export interface PersonEventItem {
  slug: string;
  title: string;
  relation: string;
}

export interface PersonDetail {
  id: string;
  slug: string;
  name: string;
  nameArabic: string | null;
  role: string;
  deathYearAh: number | null;
  events: PersonEventItem[];
}

export interface PathSummary {
  slug: string;
  title: string;
  blurb: string | null;
  audience: string;
  estMinutes: number | null;
  stepCount: number;
}

export interface PathStep {
  ordinal: number;
  eventSlug: string;
  eventTitle: string;
  prompt: string | null;
}

export interface PathDetail {
  slug: string;
  title: string;
  blurb: string | null;
  audience: string;
  estMinutes: number | null;
  steps: PathStep[];
}
