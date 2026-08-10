import {
  Component, computed, DestroyRef, effect, ElementRef, HostListener,
  inject, input, signal, viewChild,
} from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { iconSvg } from './icon.component';
import { placeKind } from './geo/hejaz-geo';

interface RelatedVerse {
  reference: string; surahNameEn: string; surahNameAr: string;
  textUthmani: string; translation: string | null; translator: string | null; relation: string;
}
interface StoryPlace {
  slug: string; name: string; modernName: string | null;
  latitude: number | null; longitude: number | null; approximate: boolean;
}
interface StorySource {
  workTitle: string; tier: string; locator: string;
  quote: string | null; quoteAr: string | null; chain: string[] | null; grade: string | null;
}
interface StoryBeat {
  slug: string; title: string; summary: string | null; why: string | null;
  hijriYear: number | null; gregYear: number | null; major: boolean; undated: boolean;
  verse: RelatedVerse | null; place: StoryPlace | null; source: StorySource | null;
}
interface Story {
  chronicle: string; prophet: string; glyph: string; titleAr: string | null;
  blurb: string | null; beats: StoryBeat[];
}

type LL = [number, number];

// Story Mode: a whole chronicle told scene by scene. Each event fades in with its
// summary and the āyah revealed around it, while a small map flies along the
// prophet's geography — every beat cited to its source. It auto-plays, but the
// reader can scrub, pause, and step. Purely a re-presentation of the timeline;
// no content is invented here (§ sources-only).
@Component({
  selector: 'app-story',
  imports: [RouterLink],
  template: `
    <section class="st" [class.playing]="playing()">
      @if (data.isLoading()) { <p class="state">Opening the story…</p> }
      @else if (beats().length === 0) { <p class="state">This chronicle has no scenes yet.</p> }
      @else {
        <!-- the map flies the journey beneath the scene -->
        <div #host class="st-map" [class.dim]="!beat().place"></div>
        <div class="st-scrim"></div>

        <header class="st-top">
          <a class="st-back" [routerLink]="['/c', chronicle()]">✕ Exit story</a>
          <div class="st-who"><span class="st-glyph">{{ data.value()?.glyph }}</span>
            <span>{{ data.value()?.prophet }}</span></div>
          <div class="st-count">{{ i() + 1 }} / {{ beats().length }}</div>
        </header>

        <div class="st-stage">
          @for (b of scene(); track i()) {
            <article class="st-scene">
              @if (b.major || era(b)) {
                <div class="st-when">
                  @if (b.major) { <span class="st-major">Turning point</span> }
                  @if (era(b); as e) { <span>{{ e }}</span> }
                </div>
              }
              <h1 class="st-title">{{ b.title }}</h1>
              @if (b.summary) { <p class="st-sum">{{ b.summary }}</p> }

              @if (b.verse; as v) {
                <figure class="st-ayah">
                  <p class="st-ar" dir="rtl" lang="ar">{{ v.textUthmani }}</p>
                  @if (v.translation) { <p class="st-tr">“{{ v.translation }}”</p> }
                  <figcaption>— {{ v.surahNameEn }} · {{ v.reference }}
                    @if (v.translator) { <span class="st-by">tr. {{ v.translator }}</span> }
                  </figcaption>
                </figure>
              }

              <footer class="st-foot">
                @if (b.place) {
                  <span class="st-chip st-where">◎ {{ b.place.name }}@if (b.place.modernName) { <em> · {{ b.place.modernName }}</em> }</span>
                }
                @if (b.source; as s) {
                  <span class="st-chip st-src">{{ srcMark(s) }} {{ s.workTitle }}<em> · {{ s.locator }}</em>@if (s.grade) { <span class="st-grade">{{ s.grade }}</span> }</span>
                }
                <a class="st-chip st-more" [routerLink]="['/event', b.slug]">Open this event →</a>
              </footer>
            </article>
          }
        </div>

        <div class="st-ctl">
          <button class="st-nav" (click)="prev()" [disabled]="i() === 0" aria-label="Previous scene">◀</button>
          <button class="st-play" (click)="toggle()" [attr.aria-label]="playing() ? 'Pause' : 'Play'">
            {{ playing() ? '❚❚' : (atEnd() ? '↺' : '▶') }}
          </button>
          <button class="st-nav" (click)="next()" [disabled]="atEnd()" aria-label="Next scene">▶</button>
          <div class="st-scrub">
            @for (b of beats(); track b.slug; let n = $index) {
              <button class="st-dot" [class.on]="n === i()" [class.seen]="n < i()" [class.maj]="b.major"
                      (click)="go(n)" [attr.aria-label]="'Scene ' + (n + 1) + ': ' + b.title"
                      [attr.title]="b.title"></button>
            }
          </div>
        </div>
      }
    </section>
  `,
})
export class StoryComponent {
  chronicle = input.required<string>();
  data = httpResource<Story>(() => `/api/public/chronicles/${this.chronicle()}/story`);

  i = signal(0);
  playing = signal(false);

  beats = computed(() => this.data.value()?.beats ?? []);
  beat = computed(() => this.beats()[this.i()] ?? null);
  /** The current beat as a 0/1-element list, so `@for … track i()` replays the scene animation. */
  scene = computed(() => { const b = this.beat(); return b ? [b] : []; });
  atEnd = computed(() => this.i() >= this.beats().length - 1);

  private host = viewChild<ElementRef<HTMLElement>>('host');
  private map?: L.Map;
  private path = L.layerGroup();     // faint dots + line through the whole journey
  private hereLayer = L.layerGroup(); // the pulsing pin at the current beat
  private timer?: ReturnType<typeof setTimeout>;
  private reduce = typeof matchMedia !== 'undefined' && matchMedia('(prefers-reduced-motion: reduce)').matches;

  constructor() {
    // The map host only exists once data has loaded (it's inside the @else). Build the
    // map the moment that element appears — afterNextRender alone would fire too early.
    effect(() => {
      const el = this.host()?.nativeElement;
      if (el && !this.map) requestAnimationFrame(() => this.initMap());
    });
    // Reset to the first scene whenever a new chronicle loads, then auto-play.
    effect(() => { this.beats(); this.i.set(0); if (this.beats().length) this.playing.set(true); });
    effect(() => { this.drawPath(); });          // draw the whole journey once data arrives
    effect(() => { this.flyTo(this.beat()); });   // move the map to the current beat
    // Drive auto-advance: each time the beat or play-state changes, (re)arm the timer.
    effect(() => {
      clearTimeout(this.timer);
      if (!this.playing()) return;
      this.i();                                    // depend on current beat
      if (this.atEnd()) { this.playing.set(false); return; }
      this.timer = setTimeout(() => this.i.update((n) => n + 1), this.dwell());
    });
    inject(DestroyRef).onDestroy(() => { clearTimeout(this.timer); this.map?.remove(); });
  }

  /** How long to hold a scene — longer when there's more to read. */
  private dwell(): number {
    const b = this.beat();
    if (!b) return 6000;
    const words = (b.summary ?? '').split(/\s+/).length + (b.verse?.translation ?? '').split(/\s+/).length;
    return Math.min(13000, 4200 + words * 260);
  }

  toggle() {
    if (this.atEnd() && !this.playing()) { this.i.set(0); this.playing.set(true); return; }
    this.playing.update((p) => !p);
  }
  next() { this.playing.set(false); if (!this.atEnd()) this.i.update((n) => n + 1); }
  prev() { this.playing.set(false); if (this.i() > 0) this.i.update((n) => n - 1); }
  go(n: number) { this.playing.set(false); this.i.set(n); }

  @HostListener('window:keydown', ['$event'])
  onKey(e: KeyboardEvent) {
    if (e.key === 'ArrowRight') { e.preventDefault(); this.next(); }
    else if (e.key === 'ArrowLeft') { e.preventDefault(); this.prev(); }
    else if (e.key === ' ') { e.preventDefault(); this.toggle(); }
  }

  /** A dated label only when a source truly fixes the timing — the prophets before Islam
   *  predate any calendar, so we show nothing rather than a spurious "0 AH". */
  era(b: StoryBeat): string {
    if (b.undated) return 'Undated';
    if (b.gregYear != null && b.gregYear !== 0) return b.gregYear < 0 ? `${-b.gregYear} BCE` : `${b.gregYear} CE`;
    if (b.hijriYear != null && b.hijriYear > 0) return `${b.hijriYear} AH`;
    return '';
  }
  srcMark(s: StorySource): string { return /qur/i.test(s.workTitle) ? '۝' : '⚑'; }

  // --- map -----------------------------------------------------------------

  private initMap() {
    const el = this.host()?.nativeElement;
    if (!el) return;
    const map = L.map(el, {
      zoomControl: false, attributionControl: false, dragging: false, scrollWheelZoom: false,
      doubleClickZoom: false, boxZoom: false, keyboard: false, minZoom: 2, maxZoom: 9,
    });
    L.tileLayer(
      'https://{s}.basemaps.cartocdn.com/rastertiles/voyager_nolabels/{z}/{x}/{y}{r}.png',
      { subdomains: 'abcd', maxZoom: 9, maxNativeZoom: 9, keepBuffer: 4 },
    ).addTo(map);
    this.path.addTo(map);
    this.hereLayer.addTo(map);
    map.setView([26, 39], 4);
    this.map = map;
    map.invalidateSize();
    this.drawPath();
    this.flyTo(this.beat());
  }

  private located = computed(() => this.beats()
    .filter((b) => b.place?.latitude != null && b.place?.longitude != null));

  /** Draw the faint dotted path through every located scene, in story order. */
  private drawPath() {
    const map = this.map;
    if (!map) return;
    this.path.clearLayers();
    const pts = this.located().map((b) => [b.place!.latitude!, b.place!.longitude!] as LL);
    if (pts.length >= 2) {
      L.polyline(pts, { className: 'st-route', interactive: false }).addTo(this.path);
    }
    for (const b of this.located()) {
      L.marker([b.place!.latitude!, b.place!.longitude!], {
        interactive: false, icon: L.divIcon({
          className: 'st-node-wrap', iconSize: [10, 10], iconAnchor: [5, 5], html: `<span class="st-node"></span>`,
        }),
      }).addTo(this.path);
    }
  }

  private flyTo(b: StoryBeat | null) {
    const map = this.map;
    if (!map) return;
    this.hereLayer.clearLayers();
    if (!b?.place || b.place.latitude == null || b.place.longitude == null) return;
    const ll: LL = [b.place.latitude, b.place.longitude];
    const kind = placeKind(b.place.slug, b.place.name);
    L.marker(ll, { interactive: false, zIndexOffset: 1000, icon: L.divIcon({
      className: 'st-here-wrap', iconSize: [34, 34], iconAnchor: [17, 17],
      html: `<span class="st-here"><span class="st-pulse"></span>${iconSvg(kind, 16, '#0A1D2E', 2)}</span>`,
    }) }).addTo(this.hereLayer);
    if (this.reduce) map.setView(ll, 6, { animate: false });
    else map.flyTo(ll, 6, { duration: 1.3 });
  }
}
