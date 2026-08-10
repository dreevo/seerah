import {
  afterNextRender, Component, computed, DestroyRef, effect, ElementRef, inject, input, signal, viewChild,
} from '@angular/core';
import { httpResource } from '@angular/common/http';
import * as L from 'leaflet';
import { MapPlace, RelatedPlace, RouteLine } from './models';
import { placeKind, PlaceKind } from './geo/hejaz-geo';
import { iconSvg } from './icon.component';

interface Placed { p: RelatedPlace; kind: PlaceKind; }
type LL = [number, number];
/** A route prepared for playback: its points plus cumulative km at each point. */
interface Journey { pts: LL[]; cum: number[]; total: number; }

/** Great-circle distance in km between two [lat,lng] points. */
function haversineKm(a: LL, b: LL): number {
  const R = 6371, toR = Math.PI / 180;
  const dLat = (b[0] - a[0]) * toR, dLng = (b[1] - a[1]) * toR;
  const s = Math.sin(dLat / 2) ** 2 + Math.cos(a[0] * toR) * Math.cos(b[0] * toR) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(s)));
}

// A real map, rendered with Leaflet over a label-free physical-relief basemap
// (Esri World Physical Map — terrain and seas, no anachronistic modern roads or
// city names), tinted to an antique-parchment look and framed to match the app.
// Markers reuse the app's own non-figural icon language; routes flow and arrow
// toward their destination. Zoom/pan/tooltips are Leaflet-native.
@Component({
  selector: 'app-event-map',
  template: `
    <div class="seemap">
      <div class="seemap-stage">
        <div #host class="leaf"></div>
        <div class="mc compass" aria-hidden="true"><span class="n">N</span><span class="arrow">▲</span></div>
        @if (hasJourney()) {
          <div class="jr-ctl">
            <button class="jr-btn" (click)="playing() ? stop() : play()">
              {{ playing() ? '❚❚ Stop' : (progress() >= 1 ? '↺ Replay the journey' : '▶ Play the journey') }}
            </button>
            @if (playing() || progress() > 0) {
              <div class="jr-hud">
                <span class="jr-route">{{ journeyLabel() }}</span>
                <span class="jr-km">{{ liveKm() }} km</span>
              </div>
            }
          </div>
        }
      </div>
      <div class="seemap-foot">
        <div class="legend">
          @for (l of legend(); track l.k) { <span class="lg"><i class="lg-{{ l.k }}"></i>{{ l.t }}</span> }
        </div>
        <span class="disclaimer">This event highlighted in gold · the other dots are other prophets' places (zoom in for their names) · journeys stylised, not to scale</span>
      </div>
    </div>
  `,
})
export class EventMapComponent {
  places = input<RelatedPlace[]>([]);
  routes = input<RouteLine[]>([]);

  /** Every place in the corpus — drawn faintly so a single event keeps the whole geography for context. */
  private allPlaces = httpResource<MapPlace[]>(() => '/api/public/places', { defaultValue: [] });

  private host = viewChild.required<ElementRef<HTMLElement>>('host');
  private map?: L.Map;
  private overlays = L.layerGroup();
  private context = L.layerGroup();
  private playLayer = L.layerGroup();
  private bounds: LL[] = [];
  private contextBounds: LL[] = [];

  // --- journey playback ---
  playing = signal(false);
  progress = signal(0);
  private raf = 0;

  constructor() {
    // Build the map only after the browser has laid the container out.
    afterNextRender(() => requestAnimationFrame(() => this.initMap()));
    effect(() => { this.places(); this.routes(); this.draw(); });
    effect(() => { this.allPlaces.value(); this.drawContext(); });
    inject(DestroyRef).onDestroy(() => { cancelAnimationFrame(this.raf); this.map?.remove(); });
  }

  private initMap() {
    const map = L.map(this.host().nativeElement, {
      zoomControl: false, attributionControl: true, minZoom: 2, maxZoom: 9,
    });
    L.tileLayer(
      'https://{s}.basemaps.cartocdn.com/rastertiles/voyager_nolabels/{z}/{x}/{y}{r}.png',
      { subdomains: 'abcd', attribution: '© OpenStreetMap · © CARTO', maxZoom: 9, maxNativeZoom: 9, keepBuffer: 4 },
    ).addTo(map);
    L.control.zoom({ position: 'topleft' }).addTo(map);
    L.control.scale({ position: 'bottomleft', imperial: false, maxWidth: 140 }).addTo(map);
    this.context.addTo(map);   // context layer beneath the event's own markers
    this.overlays.addTo(map);
    this.playLayer.addTo(map); // journey playback (traveller + trail) on top
    this.map = map;
    // Context names would clutter the wide overview, so reveal them only once zoomed in;
    // the event's own labels always show. Labels de-overlap as you zoom, so this reads clean.
    const toggleCtxLabels = () => this.host().nativeElement.classList.toggle('show-ctx-labels', map.getZoom() >= 6);
    map.on('zoomend', toggleCtxLabels);
    map.setView([26, 39], 5);
    toggleCtxLabels();
    map.invalidateSize();
    this.draw();
    this.drawContext();
  }

  /** Every other place across the corpus, drawn as small unlabelled dots for orientation. */
  private drawContext() {
    const map = this.map;
    if (!map) return;
    this.context.clearLayers();
    this.contextBounds = [];
    const here = new Set(this.places().map((p) => p.slug));
    for (const p of this.allPlaces.value()) {
      if (p.latitude == null || p.longitude == null || here.has(p.slug)) continue;
      const kind = placeKind(p.slug, p.name);
      this.contextBounds.push([p.latitude, p.longitude]);
      const m = L.marker([p.latitude, p.longitude], { icon: L.divIcon({
        className: 'mpin-ctx-wrap', iconSize: [18, 18], iconAnchor: [9, 9],
        html: `<span class="mpin-ctx pk-${kind}">${iconSvg(kind, 10, '#F3ECD9', 2)}</span>`,
      }), zIndexOffset: -500 });
      m.bindTooltip(p.name, { permanent: true, direction: 'top', offset: [0, -10], className: 'map-lbl ctx' });
      m.addTo(this.context);
    }
    // Refit so the default view shows the whole geography with this event highlighted.
    this.fit();
  }

  placed = computed<Placed[]>(() =>
    this.places().filter((p) => p.latitude != null && p.longitude != null)
      .map((p) => ({ p, kind: placeKind(p.slug, p.name) })));

  private draw() {
    const map = this.map;
    if (!map) return;
    this.resetPlay();          // a new event cancels any journey in progress
    this.overlays.clearLayers();
    this.bounds = [];

    // routes first, so pins sit above them
    for (const r of this.routes()) {
      const pts = r.points.map((pt) => [pt.lat, pt.lng] as [number, number]);
      if (pts.length < 2) continue;
      pts.forEach((pt) => this.bounds.push(pt));
      L.polyline(pts, { className: 'rt-halo', interactive: false }).addTo(this.overlays);
      const flow = L.polyline(pts, { className: 'rt-flow' + (r.conjectural ? ' conj' : '') });
      flow.bindTooltip(this.pretty(r.slug) + (r.distanceKm ? ` · ~${Math.round(r.distanceKm)} km` : '')
        + (r.conjectural ? ' · reconstructed' : ''), { sticky: true, className: 'map-rl' });
      flow.addTo(this.overlays);
      // origin dot, so a journey shows where it began, not only where it ends
      L.marker(pts[0], { interactive: false, icon: L.divIcon({
        className: 'rt-start-wrap', iconSize: [14, 14], iconAnchor: [7, 7], html: `<span class="rt-start"></span>`,
      }) }).addTo(this.overlays);
      const a = pts[pts.length - 2], b = pts[pts.length - 1];
      const brg = Math.atan2(b[1] - a[1], b[0] - a[0]) * 180 / Math.PI;
      L.marker(b, { interactive: false, icon: L.divIcon({
        className: 'rt-arrow-wrap', iconSize: [16, 16], iconAnchor: [8, 8],
        html: `<span class="rt-arrow" style="transform:rotate(${90 - brg}deg)">➤</span>`,
      }) }).addTo(this.overlays);
    }

    for (const pl of this.placed()) {
      const ll: [number, number] = [pl.p.latitude!, pl.p.longitude!];
      this.bounds.push(ll);
      if (pl.p.approximate) {
        L.circle(ll, { radius: 24000, className: 'approx', interactive: false }).addTo(this.overlays);
      }
      const m = L.marker(ll, { icon: L.divIcon({
        className: 'mpin-wrap', iconSize: [30, 30], iconAnchor: [15, 15],
        html: `<span class="mpin pk-${pl.kind}"><span class="pulse"></span>${iconSvg(pl.kind, 15, '#0A1D2E', 1.9)}</span>`,
      }) });
      m.bindTooltip(pl.p.name + (pl.p.modernName ? `<em>${pl.p.modernName}</em>` : ''),
        { permanent: true, direction: 'top', offset: [0, -14], className: 'map-lbl' });
      m.addTo(this.overlays);
    }

    map.invalidateSize();
    this.fit();
  }

  // Centre + zoom to the whole geography (this event's places + all context places),
  // so the map opens on the big picture with the current event highlighted within it.
  private fit() { this.centerZoom([...this.bounds, ...this.contextBounds], false); }

  private centerZoom(b: LL[], animate: boolean, pad = 0.4) {
    const map = this.map;
    if (!map || !b.length) return;
    const lats = b.map((p) => p[0]), lngs = b.map((p) => p[1]);
    const minLat = Math.min(...lats), maxLat = Math.max(...lats);
    const minLng = Math.min(...lngs), maxLng = Math.max(...lngs);
    const cLat = (minLat + maxLat) / 2, cLng = (minLng + maxLng) / 2;
    const span = Math.max(maxLat - minLat, (maxLng - minLng) * 0.72) + pad;
    let zoom = 9;
    for (const [s, z] of [[0.6, 9], [1.2, 8], [2.6, 7], [5, 6], [10, 5], [1e9, 4]] as [number, number][]) {
      if (span <= s) { zoom = z; break; }
    }
    map.setView([cLat, cLng], zoom, { animate });
  }

  // --- journey playback ---------------------------------------------------

  hasJourney = computed(() => this.routes().some((r) => r.points.length >= 2));

  /** Each route prepared with cumulative distances so a traveller can move along it. */
  journeys = computed<Journey[]>(() => this.routes()
    .map((r) => r.points.map((pt) => [pt.lat, pt.lng] as LL))
    .filter((pts) => pts.length >= 2)
    .map((pts) => {
      const cum = [0];
      for (let i = 1; i < pts.length; i++) cum.push(cum[i - 1] + haversineKm(pts[i - 1], pts[i]));
      return { pts, cum, total: cum[cum.length - 1] };
    }));

  private totalKm = computed(() => this.journeys().reduce((s, j) => s + j.total, 0));
  liveKm = computed(() => Math.round(this.totalKm() * this.progress()));

  /** "Makkah → Madīnah · 340 km" — endpoints named from the event's own places. */
  journeyLabel = computed(() => {
    const js = this.journeys();
    if (!js.length) return '';
    const last = js[js.length - 1].pts;
    const from = this.nearestPlaceName(js[0].pts[0]);
    const to = this.nearestPlaceName(last[last.length - 1]);
    const head = from && to && from !== to ? `${from} → ${to}` : this.pretty(this.routes()[0].slug);
    return `${head} · ${Math.round(this.totalKm())} km`;
  });

  private nearestPlaceName(pt: LL): string | null {
    const near = (list: { name: string; lat: number; lng: number }[]): string | null => {
      let best: string | null = null, bd = Infinity;
      for (const x of list) { const d = haversineKm(pt, [x.lat, x.lng]); if (d < bd) { bd = d; best = x.name; } }
      return bd < 150 ? best : null;   // only name it if a place is genuinely near the endpoint
    };
    // Prefer the event's own places; fall back to the whole corpus for the far endpoint.
    const own = this.placed().map((pl) => ({ name: pl.p.name, lat: pl.p.latitude!, lng: pl.p.longitude! }));
    const all = this.allPlaces.value().filter((p) => p.latitude != null)
      .map((p) => ({ name: p.name, lat: p.latitude!, lng: p.longitude! }));
    return near(own) ?? near(all);
  }

  play() {
    const js = this.journeys();
    if (!this.map || !js.length) return;
    cancelAnimationFrame(this.raf);
    this.host().nativeElement.classList.add('playing');
    this.centerZoom(js.flatMap((j) => j.pts), true, 0.8);   // zoom in to the journey
    this.playing.set(true);
    const dur = Math.max(4500, Math.min(11000, this.totalKm() * 9)); // pace by distance
    const t0 = performance.now();
    const step = (now: number) => {
      const p = Math.min(1, (now - t0) / dur);
      this.progress.set(p);
      this.renderPlay(p);
      if (p < 1 && this.playing()) this.raf = requestAnimationFrame(step);
      else this.playing.set(false);   // finished: keep the full trail + "Replay"
    };
    this.raf = requestAnimationFrame(step);
  }

  stop() { this.resetPlay(); this.fit(); }

  private resetPlay() {
    cancelAnimationFrame(this.raf);
    this.playing.set(false);
    this.progress.set(0);
    this.playLayer.clearLayers();
    this.host()?.nativeElement.classList.remove('playing');
  }

  /** Draw the growing trail and the traveller at fraction p (0..1) along every route. */
  private renderPlay(p: number) {
    this.playLayer.clearLayers();
    for (const j of this.journeys()) {
      const { pts, cum, total } = j;
      const d = p * total;
      let i = 0;
      while (i < cum.length - 2 && cum[i + 1] < d) i++;
      const seg = Math.max(1e-9, cum[i + 1] - cum[i]);
      const t = Math.max(0, Math.min(1, (d - cum[i]) / seg));
      const cur: LL = [pts[i][0] + (pts[i + 1][0] - pts[i][0]) * t, pts[i][1] + (pts[i + 1][1] - pts[i][1]) * t];
      L.polyline([...pts.slice(0, i + 1), cur], { className: 'jr-trail', interactive: false }).addTo(this.playLayer);
      const brg = Math.atan2(pts[i + 1][1] - pts[i][1], pts[i + 1][0] - pts[i][0]) * 180 / Math.PI;
      L.marker(cur, { interactive: false, zIndexOffset: 1000, icon: L.divIcon({
        className: 'jr-mark-wrap', iconSize: [26, 26], iconAnchor: [13, 13],
        html: `<span class="jr-mark"><span class="jr-pulse"></span><span class="jr-arrow" style="transform:rotate(${90 - brg}deg)">➤</span></span>`,
      }) }).addTo(this.playLayer);
    }
  }

  legend = computed(() => {
    const kinds = new Set(this.placed().map((p) => p.kind));
    const all: { k: PlaceKind; t: string }[] = [
      { k: 'sanctuary', t: 'Sanctuary' }, { k: 'holy', t: 'Holy site' }, { k: 'city', t: 'City' },
      { k: 'fortress', t: 'Fortress' }, { k: 'cave', t: 'Cave' }, { k: 'mountain', t: 'Mountain' },
      { k: 'battle', t: 'Battlefield' }, { k: 'waypoint', t: 'Waypoint' },
    ];
    return all.filter((l) => kinds.has(l.k));
  });

  pretty(v: string): string {
    return v.split('-').join(' ').split('_').map((w) => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');
  }
}
