import { Component, computed, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { httpResource } from '@angular/common/http';
import { ChronicleItem, TimelineItem } from './models';
import { Era, erasFor } from './chronicle-config';

interface Band { name: string; cls: string; left: number; width: number; }
interface Node { item: TimelineItem; x: number; top: number; up: boolean; stem: number; eraCls: string; eraName: string; }
interface UNode { item: TimelineItem; left: number; top: number; }

const SPACING = [236, 300, 392];
const ZL = ['Compact', 'Comfortable', 'Spacious'];
const PAD = 150;
const AXIS_Y = 340;
const U_GAP = 118;      // vertical gap between undated cards on the branch
const U_TOP = 40;       // first undated card's drop below the axis

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

    @if (!hasDates() && visible().length) {
      <p class="tl-datenote">The prophets before Islam predate any recorded calendar, so this line carries no CE or AH dates — events follow the order the Qur’ān and authentic ḥadīth relate them in, and each card shows its phase in the account. Events the sources place at no fixed point branch off the end, marked <span class="qm">?</span>.</p>
    }

    @if (events.isLoading()) {
      <p class="state">Loading the chronology…</p>
    } @else if (events.error()) {
      <p class="state err">The chronology service is unavailable. Is the backend running on :8080?</p>
    } @else if (visible().length === 0) {
      <p class="state">No events published yet in this chronicle.</p>
    } @else {
      <div class="tl-wrap">
        <div class="tl" [style.width.px]="width()" [style.height.px]="tlHeight()">
          @if (hasSpine()) {
            @for (b of bands(); track b.name + b.left) {
              <div class="era-band {{ b.cls }}" [style.left.px]="b.left" [style.width.px]="b.width">{{ b.name }}</div>
            }
            <div class="axis" [style.width.px]="width()"></div>
          }
          @for (n of nodes(); track n.item.id) {
            <button class="node" [class.up]="n.up" [class.dn]="!n.up"
                    [style.left.px]="n.x - 80" [style.top.px]="n.top" (click)="open(n.item)">
              @if (!n.up) { <span class="mark">◆</span> }
              <div class="card">
                @if (n.item.major) { <span class="keytag">✦ Pivotal</span> }
                <div class="cardtop">
                  <span class="era-dot {{ n.eraCls }}"></span>
                  <div class="yr">{{ yearLabel(n.item) || n.eraName }}</div>
                </div>
                <div class="ttl">{{ n.item.title }}</div>
                <span class="cat" [class]="'c-' + n.item.certainty">{{ label(n.item.certainty) }}</span>
              </div>
              @if (n.up) { <span class="mark">◆</span> }
              <div class="stem" [style.height.px]="n.stem" [style.top]="n.up ? '100%' : 'auto'" [style.bottom]="n.up ? 'auto' : '100%'"></div>
            </button>
          }

          <!-- the "?" branch: events the sources confirm but never place in time,
               growing off the END of the line so they read as neither before nor after -->
          @if (undatedNodes().length) {
            <div class="ubranch-line" [style.left.px]="junctionX()" [style.top.px]="branchY()" [style.height.px]="branchHeight()"></div>
            <div class="ubranch-junction" [style.left.px]="junctionX() - 15" [style.top.px]="branchY() - 15">?</div>
            <div class="ubranch-cap" [style.left.px]="junctionX() + 24" [style.top.px]="branchY() - 62">
              <div class="ubc-t">Confirmed · timing not given</div>
              <div class="ubc-s">{{ hasSpine() ? 'off the dated line — neither before nor after' : 'the sources place these at no fixed time' }}</div>
            </div>
            @for (u of undatedNodes(); track u.item.id) {
              <button class="unode" [style.left.px]="u.left" [style.top.px]="u.top" (click)="open(u.item)">
                <span class="utick" [style.width.px]="u.left - junctionX()"></span>
                <div class="card ucard">
                  @if (u.item.major) { <span class="keytag">✦ Pivotal</span> }
                  <div class="ttl">{{ u.item.title }}</div>
                  <span class="cat" [class]="'c-' + u.item.certainty">{{ label(u.item.certainty) }}</span>
                  <span class="umark">?</span>
                </div>
              </button>
            }
          }
        </div>
      </div>
      <div class="tl-hint">
        {{ spine().length }} dated events{{ undatedItems().length ? ' · ' + undatedItems().length + ' undated (on the ? branch)' : '' }}
        · scroll sideways to explore · click any event to open it
      </div>
    }
  `,
})
export class TimelineComponent {
  chronicle = input.required<string>();
  zoom = signal(0);
  era = signal<'all' | number>('all');
  readonly axisY = AXIS_Y;

  events = httpResource<TimelineItem[]>(
    () => `/api/public/timeline?locale=en&chronicle=${encodeURIComponent(this.chronicle())}`,
    { defaultValue: [] },
  );
  info = httpResource<ChronicleItem>(() => `/api/public/chronicles/${encodeURIComponent(this.chronicle())}`);

  eras = computed<Era[]>(() => erasFor(this.chronicle()));
  zoomLabel = computed(() => ZL[this.zoom()]);

  // Backend already returns events in chronological (sort_key) order — preserve it.
  private ordered = computed<TimelineItem[]>(() => this.events.value());
  hasDates = computed(() => this.ordered().some((e) => e.gregYear != null));

  visible = computed<TimelineItem[]>(() => {
    const all = this.ordered();
    const e = this.era();
    if (e === 'all') return all;
    const er = this.eras()[e];
    if (!er) return all;
    return all.filter((item, i) => er.test(item, i, all.length));
  });

  /** The dated spine — events whose "when" the sources fix. Drives all positioning. */
  spine = computed<TimelineItem[]>(() => this.visible().filter((i) => !i.undated));
  /** Confirmed events with no source-given time — rendered on the detached "?" branch. */
  undatedItems = computed<TimelineItem[]>(() => this.visible().filter((i) => i.undated));

  private eraOf(item: TimelineItem, index: number, total: number): Era | undefined {
    return this.eras().find((e) => e.test(item, index, total));
  }

  private x(i: number): number { return PAD + i * SPACING[this.zoom()]; }

  hasSpine = computed(() => this.spine().length > 0);

  /** Where the branch leaves the line: just past the last dated node — or near the top-left
   *  when there is no dated line at all, so an all-undated chronicle doesn't sink to the bottom. */
  junctionX = computed(() =>
    this.hasSpine() ? this.x(this.spine().length - 1) + Math.round(SPACING[this.zoom()] * 0.62) : PAD);

  /** The branch hangs off the axis when there are dated events, else it starts near the top. */
  branchY = computed(() => (this.hasSpine() ? AXIS_Y : 108));

  undatedNodes = computed<UNode[]>(() =>
    this.undatedItems().map((item, i) => ({ item, left: this.junctionX() + 30, top: this.branchY() + U_TOP + i * U_GAP })));

  branchHeight = computed(() => {
    const n = this.undatedItems().length;
    return n ? U_TOP + (n - 1) * U_GAP + 30 : 0;
  });

  width = computed(() => Math.max(1200,
    this.x(this.spine().length - 1) + PAD,
    this.undatedItems().length ? this.junctionX() + 330 : 0));

  tlHeight = computed(() => this.hasSpine()
    ? Math.max(720, AXIS_Y + this.branchHeight() + 130)
    : this.branchY() + this.branchHeight() + 70);

  nodes = computed<Node[]>(() => {
    const vis = this.spine();
    return vis.map((item, i) => {
      const up = i % 2 === 0;
      const long = up ? i % 4 === 0 : i % 4 === 1;
      const h = long ? 150 : 92;
      const era = this.eraOf(item, i, vis.length);
      return { item, x: this.x(i), top: up ? AXIS_Y - h - 116 : AXIS_Y + h, up, stem: h,
        eraCls: era?.cls ?? 'e1', eraName: era?.name ?? '' };
    });
  });

  // Group consecutive spine events sharing an era label into period bands.
  bands = computed<Band[]>(() => {
    const vis = this.spine();
    if (!this.eras().length || !vis.length) return [];
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
