import {
  afterNextRender, Component, computed, DestroyRef, effect, ElementRef, inject, input, viewChild,
} from '@angular/core';
import * as L from 'leaflet';
import { RelatedPlace, RouteLine } from './models';
import { placeKind, PlaceKind } from './geo/hejaz-geo';
import { iconSvg } from './icon.component';

interface Placed { p: RelatedPlace; kind: PlaceKind; }

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
      </div>
      <div class="seemap-foot">
        <div class="legend">
          @for (l of legend(); track l.k) { <span class="lg"><i class="lg-{{ l.k }}"></i>{{ l.t }}</span> }
        </div>
        <span class="disclaimer">Physical relief basemap · journeys stylised, not to scale · no figural imagery</span>
      </div>
    </div>
  `,
})
export class EventMapComponent {
  places = input<RelatedPlace[]>([]);
  routes = input<RouteLine[]>([]);

  private host = viewChild.required<ElementRef<HTMLElement>>('host');
  private map?: L.Map;
  private overlays = L.layerGroup();
  private bounds: [number, number][] = [];

  constructor() {
    // Build the map only after the browser has laid the container out.
    afterNextRender(() => requestAnimationFrame(() => this.initMap()));
    effect(() => { this.places(); this.routes(); this.draw(); });
    inject(DestroyRef).onDestroy(() => this.map?.remove());
  }

  private initMap() {
    const map = L.map(this.host().nativeElement, {
      zoomControl: false, attributionControl: true, minZoom: 2, maxZoom: 9,
    });
    L.tileLayer(
      'https://server.arcgisonline.com/ArcGIS/rest/services/World_Physical_Map/MapServer/tile/{z}/{y}/{x}',
      { attribution: 'Tiles © Esri — Physical Map', maxZoom: 9, maxNativeZoom: 8, keepBuffer: 4 },
    ).addTo(map);
    L.control.zoom({ position: 'topleft' }).addTo(map);
    L.control.scale({ position: 'bottomleft', imperial: false, maxWidth: 140 }).addTo(map);
    this.overlays.addTo(map);
    this.map = map;
    map.setView([26, 39], 5);
    map.invalidateSize();
    this.draw();
  }

  placed = computed<Placed[]>(() =>
    this.places().filter((p) => p.latitude != null && p.longitude != null)
      .map((p) => ({ p, kind: placeKind(p.slug, p.name) })));

  private draw() {
    const map = this.map;
    if (!map) return;
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

  // Centre + zoom (robust to any container-size timing, unlike fitBounds).
  private fit() {
    const map = this.map, b = this.bounds;
    if (!map || !b.length) return;
    const lats = b.map((p) => p[0]), lngs = b.map((p) => p[1]);
    const minLat = Math.min(...lats), maxLat = Math.max(...lats);
    const minLng = Math.min(...lngs), maxLng = Math.max(...lngs);
    const cLat = (minLat + maxLat) / 2, cLng = (minLng + maxLng) / 2;
    const span = Math.max(maxLat - minLat, (maxLng - minLng) * 0.72) + 0.4;
    let zoom = 9;
    for (const [s, z] of [[0.6, 9], [1.2, 8], [2.6, 7], [5, 6], [10, 5], [1e9, 4]] as [number, number][]) {
      if (span <= s) { zoom = z; break; }
    }
    map.setView([cLat, cLng], zoom, { animate: false });
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
