import { Component, computed, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { httpResource } from '@angular/common/http';
import { ChronicleItem, TimelineItem } from './models';
import { Era, erasFor } from './chronicle-config';

interface Band { name: string; cls: string; left: number; width: number; }
interface Node { item: TimelineItem; x: number; top: number; up: boolean; stem: number; eraCls: string; }

const SPACING = [236, 300, 392];
const ZL = ['Compact', 'Comfortable', 'Spacious'];
const PAD = 150;
const AXIS_Y = 340;

@Component({
  selector: 'app-timeline',
  template: `
    <section class="hero">
      <div class="eyebrow">{{ info.value()?.subtitle || 'Interactive Chronology' }}</div>
      <div class="basmala">بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ</div>
      <h1>{{ titleMain() }} <em>{{ titleEm() }}</em></h1>
      <p class="sub">{{ info.value()?.blurb ||
        'A connected chronology — filter by period, adjust the spacing, and scroll across the story; open any event for the verses, people, and geography behind it.' }}</p>
    </section>

    <div class="tl-controls">
      @if (eras().length) {
        <div class="seg">
          <button [class.on]="era() === 'all'" (click)="era.set('all')">All Periods</button>
          @for (e of eras(); track e.name; let i = $index) {
            <button [class.on]="era() === i" (click)="era.set(i)">{{ e.name }}</button>
          }
        </div>
      }
      <div class="zoom">
        <span>SPACING</span>
        <button (click)="zoomBy(-1)" [disabled]="zoom() === 0" aria-label="Tighter">−</button>
        <span class="lvl">{{ zoomLabel() }}</span>
        <button (click)="zoomBy(1)" [disabled]="zoom() === 2" aria-label="Wider">+</button>
      </div>
    </div>

    @if (events.isLoading()) {
      <p class="state">Loading the chronology…</p>
    } @else if (events.error()) {
      <p class="state err">The chronology service is unavailable. Is the backend running on :8080?</p>
    } @else if (visible().length === 0) {
      <p class="state">No events published yet in this chronicle.</p>
    } @else {
      <div class="tl-wrap">
        <div class="tl" [style.width.px]="width()">
          @for (b of bands(); track b.name + b.left) {
            <div class="era-band {{ b.cls }}" [style.left.px]="b.left" [style.width.px]="b.width">{{ b.name }}</div>
          }
          <div class="axis" [style.width.px]="width()"></div>
          @for (n of nodes(); track n.item.id) {
            <button class="node" [class.up]="n.up" [class.dn]="!n.up"
                    [style.left.px]="n.x - 80" [style.top.px]="n.top" (click)="open(n.item)">
              @if (!n.up) { <span class="mark">◆</span> }
              <div class="card">
                @if (n.item.major) { <span class="keytag">✦ Pivotal</span> }
                <div class="cardtop">
                  <span class="era-dot {{ n.eraCls }}"></span>
                  <div class="yr">{{ yearLabel(n.item) }}</div>
                </div>
                <div class="ttl">{{ n.item.title }}</div>
                <span class="cat" [class]="'c-' + n.item.certainty">{{ label(n.item.certainty) }}</span>
              </div>
              @if (n.up) { <span class="mark">◆</span> }
              <div class="stem" [style.height.px]="n.stem" [style.top]="n.up ? '100%' : 'auto'" [style.bottom]="n.up ? 'auto' : '100%'"></div>
            </button>
          }
        </div>
      </div>
      <div class="tl-hint">{{ visible().length }} events · scroll sideways to explore · click any event to open it</div>
    }
  `,
})
export class TimelineComponent {
  chronicle = input.required<string>();
  zoom = signal(0);
  era = signal<'all' | number>('all');

  events = httpResource<TimelineItem[]>(
    () => `/api/public/timeline?locale=en&chronicle=${encodeURIComponent(this.chronicle())}`,
    { defaultValue: [] },
  );
  info = httpResource<ChronicleItem>(() => `/api/public/chronicles/${encodeURIComponent(this.chronicle())}`);

  eras = computed<Era[]>(() => erasFor(this.chronicle()));
  zoomLabel = computed(() => ZL[this.zoom()]);

  // Backend already returns events in chronological (sort_key) order — preserve it.
  private ordered = computed<TimelineItem[]>(() => this.events.value());

  visible = computed<TimelineItem[]>(() => {
    const all = this.ordered();
    const e = this.era();
    if (e === 'all') return all;
    const er = this.eras()[e];
    if (!er) return all;
    return all.filter((item, i) => er.test(item, i, all.length));
  });

  private eraOf(item: TimelineItem, index: number, total: number): Era | undefined {
    return this.eras().find((e) => e.test(item, index, total));
  }

  private x(i: number): number { return PAD + i * SPACING[this.zoom()]; }
  width = computed(() => Math.max(1200, this.x(this.visible().length - 1) + PAD));

  nodes = computed<Node[]>(() => {
    const vis = this.visible();
    return vis.map((item, i) => {
      const up = i % 2 === 0;
      const long = up ? i % 4 === 0 : i % 4 === 1;
      const h = long ? 150 : 92;
      return { item, x: this.x(i), top: up ? AXIS_Y - h - 116 : AXIS_Y + h, up, stem: h,
        eraCls: this.eraOf(item, i, vis.length)?.cls ?? 'e1' };
    });
  });

  // Group consecutive visible events sharing an era label into period bands.
  bands = computed<Band[]>(() => {
    const vis = this.visible();
    if (!this.eras().length) return [];
    const out: Band[] = [];
    const half = SPACING[this.zoom()] / 2;
    let start = 0;
    let curName = this.eraOf(vis[0], 0, vis.length)?.name ?? '';
    let curCls = this.eraOf(vis[0], 0, vis.length)?.cls ?? 'e1';
    const push = (from: number, to: number, name: string, cls: string) => {
      if (!name) return;
      out.push({ name, cls, left: this.x(from) - half, width: this.x(to) - this.x(from) + SPACING[this.zoom()] });
    };
    for (let i = 1; i < vis.length; i++) {
      const e = this.eraOf(vis[i], i, vis.length);
      if ((e?.name ?? '') !== curName) {
        push(start, i - 1, curName, curCls);
        start = i; curName = e?.name ?? ''; curCls = e?.cls ?? 'e1';
      }
    }
    push(start, vis.length - 1, curName, curCls);
    return out;
  });

  private titleParts = computed<[string, string]>(() => {
    const t = this.info.value()?.title ?? '';
    if (!t) return ['The Chronicle', ''];
    // Colour the final word gold (matches the Seerah hero look).
    const parts = t.split(' ');
    if (parts.length < 2) return [t, ''];
    return [parts.slice(0, -1).join(' '), parts[parts.length - 1]];
  });
  titleMain = computed(() => this.titleParts()[0]);
  titleEm = computed(() => this.titleParts()[1]);

  constructor(private router: Router) {}

  zoomBy(d: number) { this.zoom.update((z) => Math.max(0, Math.min(2, z + d))); }
  open(item: TimelineItem) { this.router.navigate(['/event', item.slug]); }

  yearLabel(item: TimelineItem): string {
    if (item.gregYear == null) return '';
    return `${item.gregYear} CE` + (item.hijriYear && item.hijriYear > 0 ? ` · ${item.hijriYear} AH` : '');
  }

  label(certainty: string): string {
    switch (certainty) {
      case 'MUTAWATIR': return 'Mass-transmitted';
      case 'WELL_ATTESTED': return 'Well-attested';
      case 'REPORTED': return 'Reported';
      case 'SCHOLARS_DIFFER': return 'Scholars differ';
      case 'DISPUTED': return 'Disputed';
      default: return certainty.charAt(0) + certainty.slice(1).toLowerCase();
    }
  }
}
