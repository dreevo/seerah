import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';

interface IsnadEventRef { slug: string; title: string; chronicle: string; prophet: string; glyph: string; }
interface IsnadReport {
  collection: string; collectorName: string; number: string; grade: string | null;
  companionAr: string; companionEn: string; chain: string[]; events: IsnadEventRef[];
}
interface Companion { key: string; ar: string; en: string; count: number; reports: IsnadReport[];
  x: number; y: number; r: number; a: number; lx: number; ly: number; anchor: string; }
interface Collector { key: string; name: string; count: number; x: number; y: number; }

const CX = 500, CY = 348, R1 = 232;

// The Isnād — how the corpus' ḥadīth reach us. Every report we cite traces back,
// through a named chain, to a Companion who heard it and, at the origin, to the
// Prophet ﷺ. Here the Prophet ﷺ is the centre; the Companions ring him, sized by
// how many of these narrations each carries; the collections that recorded them
// sit below. Only the reliable anchors (Companion, collector) are merged into
// nodes — each report keeps its own full chain (§ sources-only). No figural image.
@Component({
  selector: 'app-isnad',
  imports: [RouterLink],
  template: `
    <section class="isn">
      <div class="isn-head">
        <div class="eyebrow">The chains of transmission</div>
        <h1>How the <em>Narrations</em> Reach Us</h1>
        <p class="isn-lede">Every ḥadīth in this library is joined to the Prophet ﷺ by a chain of narrators — an
          <b>isnād</b>. Here each Companion who anchors a chain rings the centre, sized by how many of these
          reports he carries; the collections that preserved them lie below. Choose a Companion to follow his narrations.</p>
      </div>

      @if (data.isLoading()) { <p class="state">Tracing the chains…</p> }
      @else if (companions().length === 0) { <p class="state">No chains to show.</p> }
      @else {
        <div class="isn-stage">
          <div class="isn-sky">
            <svg viewBox="0 0 1000 700" role="img" aria-label="The chains of transmission of the ḥadīth" [class.sel]="active()">
              <defs>
                <radialGradient id="prophglow" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stop-color="#FFF3CE" /><stop offset="100%" stop-color="#C8A44B" stop-opacity="0" />
                </radialGradient>
              </defs>

              <!-- spokes: Prophet ﷺ → each Companion -->
              @for (c of companions(); track c.key) {
                <line class="spoke" [class.lit]="isLit(c.key)" [attr.x1]="cx" [attr.y1]="cy" [attr.x2]="c.x" [attr.y2]="c.y" />
              }
              <!-- when a Companion is chosen, draw his links down to the collections -->
              @for (l of activeLinks(); track l.id) {
                <path class="clink" [attr.d]="l.d" [attr.stroke-width]="l.w" pathLength="1" />
              }

              <!-- the collections -->
              @for (k of collectors(); track k.key) {
                <g class="coll" [class.lit]="collLit(k.key)" (mouseenter)="hoverColl.set(k.key)" (mouseleave)="hoverColl.set(null)">
                  <rect [attr.x]="k.x - 92" [attr.y]="k.y - 20" width="184" height="40" rx="10" />
                  <text class="coll-n" [attr.x]="k.x" [attr.y]="k.y - 2">{{ k.name }}</text>
                  <text class="coll-c" [attr.x]="k.x" [attr.y]="k.y + 13">{{ k.count }} narrations</text>
                </g>
              }

              <!-- the Companions -->
              @for (c of companions(); track c.key) {
                <g class="comp" [class.on]="active() === c.key" [class.dim]="active() && !isLit(c.key)"
                   [attr.transform]="'translate(' + c.x + ' ' + c.y + ')'"
                   (mouseenter)="hover.set(c.key)" (mouseleave)="hover.set(null)"
                   (click)="pick(c.key)" tabindex="0" role="button"
                   (keydown.enter)="pick(c.key)">
                  <circle class="halo" [attr.r]="c.r + 9" fill="url(#prophglow)" />
                  <circle class="disc" [attr.r]="c.r" />
                  <circle class="ring" [attr.r]="c.r" />
                  <text class="c-ct" y="1" dominant-baseline="central" [attr.font-size]="Math.min(c.r * 0.9, 17)">{{ c.count }}</text>
                  <text class="c-lbl" [class.hide]="hover() !== c.key && active() !== c.key"
                        [attr.x]="c.lx" [attr.y]="c.ly" [attr.text-anchor]="c.anchor">{{ c.en || c.ar }}</text>
                </g>
              }

              <!-- the Prophet ﷺ at the source of every chain -->
              <g class="proph">
                <circle class="pglow" [attr.cx]="cx" [attr.cy]="cy" r="70" fill="url(#prophglow)" />
                <circle class="pdisc" [attr.cx]="cx" [attr.cy]="cy" r="42" />
                <circle class="prim" [attr.cx]="cx" [attr.cy]="cy" r="42" />
                <text class="pglyph" [attr.x]="cx" [attr.y]="cy + 2" dominant-baseline="central">ﷺ</text>
                <text class="plbl" [attr.x]="cx" [attr.y]="cy + 62">The Prophet ﷺ</text>
              </g>
            </svg>
          </div>

          <aside class="isn-panel">
            @if (activeComp(); as c) {
              <div class="ip-head">
                <div class="ip-name">{{ c.en || c.ar }}</div>
                <div class="ip-ar" dir="rtl" lang="ar">{{ c.ar }}</div>
                <div class="ip-count">{{ c.count }} {{ c.count === 1 ? 'narration' : 'narrations' }} in this library</div>
              </div>
              <div class="ip-reports">
                @for (r of c.reports; track r.collection + r.number) {
                  <div class="ip-rep">
                    <div class="ip-rep-top">
                      <span class="ip-coll">{{ r.collectorName }} <em>#{{ r.number }}</em></span>
                      @if (r.grade) { <span class="ip-grade">{{ pretty(r.grade) }}</span> }
                    </div>
                    <div class="ip-evs">
                      @for (e of r.events; track e.slug) {
                        <a class="ip-ev" [routerLink]="['/event', e.slug]"><span class="g">{{ e.glyph }}</span>{{ e.title }}</a>
                      }
                    </div>
                    <div class="ip-chain" dir="rtl" lang="ar">{{ chainLine(r) }}</div>
                  </div>
                }
              </div>
            } @else {
              <div class="ip-empty">
                <p><b>{{ companions().length }}</b> Companions carry <b>{{ data.value().length }}</b> narrations
                  into <b>{{ collectors().length }}</b> collections.</p>
                <p class="ip-hint">Hover the centre to see the source of every chain. Select a Companion to read
                  the reports he transmits and the events they ground.</p>
                <ul class="ip-top">
                  @for (c of top(); track c.key) {
                    <li (mouseenter)="hover.set(c.key)" (mouseleave)="hover.set(null)" (click)="pick(c.key)">
                      <span class="ip-top-n">{{ c.en || c.ar }}</span><span class="ip-top-c">{{ c.count }}</span>
                    </li>
                  }
                </ul>
              </div>
            }
          </aside>
        </div>
      }
    </section>
  `,
})
export class IsnadComponent {
  data = httpResource<IsnadReport[]>(() => '/api/public/isnad', { defaultValue: [] });
  readonly cx = CX; readonly cy = CY;
  readonly Math = Math;

  hover = signal<string | null>(null);
  picked = signal<string | null>(null);
  hoverColl = signal<string | null>(null);
  /** The Companion in focus — a click pins it, else hover. */
  active = computed(() => this.picked() ?? this.hover());

  /** Companions merged by canonical name, sized and placed on a ring around the Prophet ﷺ. */
  companions = computed<Companion[]>(() => {
    const by = new Map<string, IsnadReport[]>();
    for (const r of this.data.value()) {
      const arr = by.get(r.companionAr);
      if (arr) arr.push(r); else by.set(r.companionAr, [r]);
    }
    const list = [...by.entries()].map(([ar, reports]) => ({
      ar, en: reports[0].companionEn, key: ar, count: reports.length, reports,
    })).sort((a, b) => b.count - a.count);
    const n = list.length;
    return list.map((c, i) => {
      const a = (-90 + (i * 360) / n) * Math.PI / 180;
      const x = CX + R1 * Math.cos(a), y = CY + R1 * Math.sin(a);
      const r = 11 + Math.sqrt(c.count) * 7;
      const out = Math.cos(a);
      return {
        ...c, x, y, r, a,
        lx: x + (r + 7) * Math.cos(a), ly: y + (r + 7) * Math.sin(a) + 4,
        anchor: out > 0.25 ? 'start' : out < -0.25 ? 'end' : 'middle',
      };
    });
  });

  private compByKey = computed(() => new Map(this.companions().map((c) => [c.key, c])));
  activeComp = computed(() => (this.active() ? this.compByKey().get(this.active()!) ?? null : null));
  top = computed(() => this.companions().slice(0, 8));

  /** The collections, placed along the base, sized by how many narrations they preserve. */
  collectors = computed<Collector[]>(() => {
    const by = new Map<string, { name: string; count: number }>();
    for (const r of this.data.value()) {
      const e = by.get(r.collection) ?? { name: r.collectorName, count: 0 };
      e.count++; by.set(r.collection, e);
    }
    const list = [...by.entries()].sort((a, b) => b[1].count - a[1].count);
    const xs = list.length === 1 ? [CX] : list.length === 2 ? [360, 640] : [500, 210, 790];
    return list.map(([key, v], i) => ({ key, name: v.name, count: v.count, x: xs[i] ?? CX, y: 648 }));
  });

  isLit(key: string): boolean { const a = this.active(); return !a || a === key; }
  collLit(key: string): boolean {
    const c = this.activeComp();
    const hc = this.hoverColl();
    if (hc) return hc === key;
    if (!c) return true;
    return c.reports.some((r) => r.collection === key);
  }

  /** When a Companion is chosen, the curves from him down to each collection he appears in. */
  activeLinks = computed(() => {
    const c = this.activeComp();
    if (!c) return [];
    const ck = this.compByKey().get(c.key)!;
    const colls = this.collectors();
    const counts = new Map<string, number>();
    for (const r of c.reports) counts.set(r.collection, (counts.get(r.collection) ?? 0) + 1);
    return [...counts.entries()].map(([coll, n]) => {
      const k = colls.find((x) => x.key === coll);
      if (!k) return null;
      const mx = (ck.x + k.x) / 2, my = (ck.y + k.y) / 2 + 40;
      return { id: coll, d: `M ${ck.x} ${ck.y} Q ${mx} ${my} ${k.x} ${k.y - 20}`, w: 1.5 + n * 1.2 };
    }).filter((l): l is { id: string; d: string; w: number } => !!l);
  });

  pick(key: string) { this.picked.set(this.picked() === key ? null : key); }

  chainLine(r: IsnadReport): string { return r.chain.join('  ←  '); }
  pretty(g: string): string { return g.charAt(0) + g.slice(1).toLowerCase(); }
}
