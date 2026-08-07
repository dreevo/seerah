import { Component, computed, inject, input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

// A single, reusable inline-SVG icon set for the whole app. Strictly non-figural:
// people are represented by symbolic motifs (lantern, star, book…), never a face
// or a human silhouette. Everything strokes/fills in currentColor so callers set
// the colour via CSS. Educational signposting (certainty, source tiers, sections)
// draws from the same set so the visual language stays consistent everywhere.
@Component({
  selector: 'app-icon',
  // The full <svg> is injected as sanitized HTML on a span: setting innerHTML on
  // a real <svg> element parses children in the HTML namespace (they never
  // render), whereas a complete "<svg>…</svg>" string parses correctly.
  template: `<span class="app-icon" aria-hidden="true" [innerHTML]="html()"></span>`,
  styles: [`:host { display: inline-flex; line-height: 0; color: inherit; } .app-icon { display: inline-flex; }`],
})
export class IconComponent {
  private sanitizer = inject(DomSanitizer);
  name = input.required<string>();
  size = input(18);

  // Map a person's role/relation to a symbolic (faceless) icon name.
  static personIcon(role?: string | null, relation?: string | null): string {
    const s = `${role ?? ''} ${relation ?? ''}`.toLowerCase();
    if (s.includes('prophet') || s.includes('messenger')) return 'prophet';
    if (s.includes('opponent') || s.includes('enemy') || s.includes('adversary')) return 'opponent';
    if (s.includes('ruler') || s.includes('king') || s.includes('negus') || s.includes('emperor')) return 'ruler';
    if (s.includes('scholar') || s.includes('scribe') || s.includes('poet') || s.includes('narrator')) return 'scholar';
    if (s.includes('commander') || s.includes('warrior') || s.includes('general')) return 'warrior';
    if (s.includes('family') || s.includes('wife') || s.includes('daughter') || s.includes('son') || s.includes('uncle') || s.includes('kin')) return 'family';
    return 'companion';
  }
  static certaintyIcon(c?: string | null): string {
    switch ((c ?? '').toUpperCase()) {
      case 'MUTAWATIR': return 'mutawatir';
      case 'WELL_ATTESTED': return 'well-attested';
      case 'SCHOLARS_DIFFER': return 'scholars-differ';
      case 'DISPUTED': return 'disputed';
      default: return 'reported';
    }
  }
  static tierIcon(t?: string | null): string {
    switch ((t ?? '').toUpperCase()) {
      case 'PRIMARY': return 'tier-primary';
      case 'CLASSICAL': return 'tier-classical';
      default: return 'tier-secondary';
    }
  }

  html = computed<SafeHtml>(() => this.sanitizer.bypassSecurityTrustHtml(iconSvg(this.name(), this.size())));
}

/** Build a standalone inline-SVG string for an icon (used where an Angular
 *  component can't be rendered — e.g. Leaflet marker HTML). */
export function iconSvg(name: string, size = 18, stroke = 'currentColor', strokeWidth = 1.6): string {
  const body = ICONS[name] ?? ICONS['dot'];
  return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${stroke}" `
    + `stroke-width="${strokeWidth}" stroke-linecap="round" stroke-linejoin="round">${body}</svg>`;
}

// 24×24 path fragments. Keep them simple, legible at ~16–22px.
const ICONS: Record<string, string> = {
  // ---- navigation --------------------------------------------------------
  timeline: `<path d="M3 12h18"/><circle cx="7" cy="12" r="2"/><circle cx="17" cy="12" r="2"/><path d="M7 10V6M17 14v4"/>`,
  explore: `<circle cx="12" cy="12" r="9"/><path d="M15.5 8.5 10.5 10.5 8.5 15.5 13.5 13.5 Z"/>`,
  companions: `<path d="M8 8.5a2.4 2.4 0 1 0 0-.01"/><path d="M16 8.5a2.4 2.4 0 1 0 0-.01"/><path d="M3.5 18c.4-2.6 2.2-4 4.5-4s4.1 1.4 4.5 4"/><path d="M15.5 14.2c2 .2 3.6 1.6 4 3.8"/>`,
  search: `<circle cx="11" cy="11" r="6"/><path d="m20 20-3.5-3.5"/>`,
  ask: `<path d="M4 5h16v10H12l-5 4v-4H4Z"/><path d="M9.2 8.6a2.8 2.8 0 0 1 5.4 1c0 1.8-2.6 2-2.6 3.4"/><path d="M12 15.4h.01"/>`,
  // ---- people (symbolic, no faces) --------------------------------------
  prophet: `<path d="M12 3v2M12 19v2M5 12H3M21 12h-2"/><path d="M12 7a5 5 0 0 1 3 9H9a5 5 0 0 1 3-9Z"/><path d="M10.5 16h3"/>`,
  companion: `<path d="M12 2.5 14 9l6.5 0-5.2 3.9 2 6.6L12 15.6 6.7 19.5l2-6.6L3.5 9 10 9Z"/>`,
  family: `<path d="M4 11 12 4l8 7"/><path d="M6 10v9h12v-9"/><path d="M10.5 19v-4h3v4"/>`,
  opponent: `<path d="M12 3 5 5.5V11c0 4.5 3 7.5 7 9 4-1.5 7-4.5 7-9V5.5Z"/><path d="m8.5 8 7 7"/>`,
  ruler: `<circle cx="12" cy="12" r="7"/><path d="M12 6.5 13.4 10l3.6.2-2.8 2.3.9 3.5L12 14.2 8.9 16l.9-3.5L7 10.2 10.6 10Z"/>`,
  scholar: `<path d="M4 5.5c2.5-1 5.5-1 8 .5 2.5-1.5 5.5-1.5 8-.5V18c-2.5-1-5.5-1-8 .5-2.5-1.5-5.5-1.5-8-.5Z"/><path d="M12 6v13"/>`,
  warrior: `<path d="m5 5 8 8M14 6l4-1-1 4-3 3M13 13l-3 3-2 3-1-1 3-2 3-3"/>`,
  // ---- certainty ---------------------------------------------------------
  mutawatir: `<circle cx="12" cy="12" r="8.5"/><path d="M4 8h16M4 12h16M4 16h16"/>`,
  'well-attested': `<path d="M12 3 5 5.5V11c0 4.5 3 7.5 7 9 4-1.5 7-4.5 7-9V5.5Z"/><path d="m8.8 12 2.2 2.2 4.2-4.4"/>`,
  'scholars-differ': `<path d="M3 8h13l-3-3M21 16H8l3 3"/>`,
  reported: `<circle cx="12" cy="12" r="8.5"/><path d="M12 8v5M12 16h.01"/>`,
  disputed: `<circle cx="12" cy="12" r="8.5"/><path d="M9 9l6 6M15 9l-6 6"/>`,
  // ---- source tiers ------------------------------------------------------
  'tier-primary': `<path d="M6 3h9l3 3v15H6Z"/><path d="M15 3v3h3"/><path d="M9 12h6M9 15.5h6"/>`,
  'tier-classical': `<path d="M4 6c2.5-1 5.5-1 8 .5 2.5-1.5 5.5-1.5 8-.5V18c-2.5-1-5.5-1-8 .5-2.5-1.5-5.5-1.5-8-.5Z"/>`,
  'tier-secondary': `<path d="M6 4h12v16l-6-3-6 3Z"/>`,
  // ---- misc / signposting -----------------------------------------------
  verse: `<path d="M6 4h9a3 3 0 0 1 3 3v13H8a2 2 0 0 0-2 2Z"/><path d="M9.5 9h5M9.5 12.5h5"/>`,
  source: `<path d="M6 3h9l3 3v15H6Z"/><path d="M15 3v3h3"/>`,
  info: `<circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/>`,
  reviewed: `<path d="M12 3 5 5.5V11c0 4.5 3 7.5 7 9 4-1.5 7-4.5 7-9V5.5Z"/><path d="m8.8 12 2.2 2.2 4.2-4.4"/>`,
  pivotal: `<path d="M12 2.5 14 9l6.5 0-5.2 3.9 2 6.6L12 15.6 6.7 19.5l2-6.6L3.5 9 10 9Z"/>`,
  chronicle: `<path d="M5 4h11l3 3v13H5Z"/><path d="M9 4v16"/><path d="M12 9h4M12 12.5h4"/>`,
  // ---- places (mirror the map's kinds) ----------------------------------
  sanctuary: `<rect x="7" y="7" width="10" height="10" rx="1"/><path d="M7 11h10"/>`,
  holy: `<path d="M6 15a6 6 0 0 1 12 0Z"/><path d="M12 9V6M12 6h.01"/><path d="M5 19h14"/>`,
  city: `<path d="M5 19V10l7-4 7 4v9"/><path d="M9 10.5a3 3 0 0 1 6 0"/>`,
  fortress: `<path d="M5 19V9h14v10"/><path d="M5 9V7h2v2M11 9V7h2v2M17 9V7h2v2"/>`,
  cave: `<path d="M4 19a8 8 0 0 1 16 0Z"/><path d="M9.5 19a2.5 3 0 0 1 5 0"/>`,
  mountain: `<path d="m3 19 6-11 3 5 2-3 7 9Z"/><path d="m7.5 12 1.5-3 1.4 2.4"/>`,
  battle: `<path d="m5 5 8 8M14 6l4-1-1 4-3 3M13 13l-3 3-2 3-1-1 3-2 3-3"/>`,
  waypoint: `<circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="2.4"/>`,
  dot: `<circle cx="12" cy="12" r="3.5"/>`,
};
