import { Component, computed, HostListener, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';

interface IsnadEventRef { slug: string; title: string; chronicle: string; prophet: string; glyph: string; }
interface IsnadReport {
  collection: string; collectorName: string; number: string; grade: string | null;
  companionAr: string; companionEn: string; chain: string[]; events: IsnadEventRef[];
}
interface Companion {
  key: string; ar: string; en: string; count: number; reports: IsnadReport[];
  x: number; y: number; r: number; a: number;
}
interface Collector { key: string; name: string; count: number; x: number; y: number; }

const CX = 500, CY = 356, R1 = 236;

// The Isnād — how the corpus' ḥadīth reach us. The Prophet ﷺ is the centre; the
// Companions who anchor the chains ring him, sized (with a capped range so the
// wheel stays even) by how many of these narrations each carries; the collections
// that preserved them sit below. Only the reliable anchors — Companion and
// collector — are merged into nodes; each report keeps its own full chain
// (§ sources-only). No figural image. A help panel explains the science itself.
@Component({
  selector: 'app-isnad',
  imports: [RouterLink],
  template: `
    <section class="isn">
      <div class="isn-head">
        <div class="eyebrow">The chains of transmission</div>
        <h1>How the <em>Narrations</em> Reach Us</h1>
        <p class="isn-lede">Every ḥadīth in this library is joined to the Prophet ﷺ by a chain of narrators — an
          <b>isnād</b>. Each Companion who anchors a chain rings the centre, sized by how many of these reports he
          carries; the collections that preserved them lie below. Choose a Companion to follow his narrations.</p>
        <button class="isn-help" (click)="helpOpen.set(true)">
          <span class="ih-star">✦</span> The science of ḥadīth — how a narration is preserved &amp; graded
        </button>
      </div>

      @if (data.isLoading()) { <p class="state">Tracing the chains…</p> }
      @else if (companions().length === 0) { <p class="state">No chains to show.</p> }
      @else {
        <div class="isn-stage">
          <div class="isn-sky">
            <svg viewBox="0 0 1000 720" role="img" aria-label="The chains of transmission of the ḥadīth"
                 [class.sel]="active()">
              <defs>
                <radialGradient id="prophglow" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stop-color="#FFF3CE" /><stop offset="100%" stop-color="#C8A44B" stop-opacity="0" />
                </radialGradient>
              </defs>

              <!-- the ring the Companions sit on -->
              <circle class="guide" [attr.cx]="cx" [attr.cy]="cy" [attr.r]="R1" />

              <!-- spokes: Prophet ﷺ → each Companion -->
              @for (c of companions(); track c.key) {
                <line class="spoke" [class.lit]="isLit(c.key)" [attr.x1]="cx" [attr.y1]="cy" [attr.x2]="c.x" [attr.y2]="c.y" />
              }
              <!-- when a Companion is chosen, his links down to the collections -->
              @for (l of activeLinks(); track l.id) {
                <path class="clink" [attr.d]="l.d" [attr.stroke-width]="l.w" pathLength="1" />
              }

              <!-- the collections -->
              @for (k of collectors(); track k.key) {
                <g class="coll" [class.lit]="collLit(k.key)" (mouseenter)="hoverColl.set(k.key)" (mouseleave)="hoverColl.set(null)">
                  <rect [attr.x]="k.x - 94" [attr.y]="k.y - 21" width="188" height="42" rx="11" />
                  <text class="coll-n" [attr.x]="k.x" [attr.y]="k.y - 2">{{ k.name }}</text>
                  <text class="coll-c" [attr.x]="k.x" [attr.y]="k.y + 14">{{ k.count }} narrations</text>
                </g>
              }

              <!-- the Companions -->
              @for (c of companions(); track c.key) {
                <g class="comp" [class.on]="active() === c.key" [class.dim]="active() && !isLit(c.key)"
                   [attr.transform]="'translate(' + c.x + ' ' + c.y + ')'"
                   (mouseenter)="hover.set(c.key)" (mouseleave)="hover.set(null)"
                   (click)="pick(c.key)" tabindex="0" role="button" (keydown.enter)="pick(c.key)">
                  <circle class="halo" [attr.r]="c.r + 10" fill="url(#prophglow)" />
                  <circle class="disc" [attr.r]="c.r" />
                  <circle class="ring" [attr.r]="c.r" />
                  <text class="c-ct" y="0" dy="0.35em" [attr.font-size]="Math.min(c.r * 0.85, 16)">{{ c.count }}</text>
                </g>
              }

              <!-- the Prophet ﷺ at the source of every chain -->
              <g class="proph">
                <circle class="pglow" [attr.cx]="cx" [attr.cy]="cy" r="72" fill="url(#prophglow)" />
                <circle class="pdisc" [attr.cx]="cx" [attr.cy]="cy" r="42" />
                <circle class="prim" [attr.cx]="cx" [attr.cy]="cy" r="42" />
                <text class="pglyph" [attr.x]="cx" [attr.y]="cy" dy="0.33em">ﷺ</text>
                <text class="plbl" [attr.x]="cx" [attr.y]="cy + 63">The Prophet ﷺ</text>
              </g>

              <!-- one clean label for the focused Companion, clamped inside the frame -->
              @if (tip(); as t) {
                <g class="c-tip">
                  <rect [attr.x]="t.x - t.w / 2" [attr.y]="t.y - 13" [attr.width]="t.w" height="26" rx="13" />
                  <text [attr.x]="t.x" [attr.y]="t.y + 1" text-anchor="middle" dominant-baseline="central">{{ t.name }}</text>
                </g>
              }
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
                <p class="ip-hint">Hover a Companion to see his name; select one to read the reports he transmits
                  and the events they ground.</p>
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

      @if (helpOpen()) {
        <div class="hs-scrim" (click)="helpOpen.set(false)">
          <div class="hs-modal" role="dialog" aria-modal="true" aria-label="The science of ḥadīth" (click)="$event.stopPropagation()">
            <button class="hs-close" (click)="helpOpen.set(false)" aria-label="Close">✕</button>
            <div class="hs-inner">
              <div class="hs-top">
                <div class="eyebrow">ʿIlm al-ḥadīth</div>
                <h2>How a Narration Is <em>Kept &amp; Weighed</em></h2>
                <p class="hs-sub">For fourteen centuries the words and deeds of the Prophet ﷺ have been carried not as
                  hearsay but as <b>documented testimony</b> — every report tied to a named chain of people and each
                  of them examined. This is the discipline that guards it.</p>
              </div>

              <!-- 1. Anatomy -->
              <section class="hs-sec">
                <h3><span class="hs-n">1</span> The two parts of a report</h3>
                <p>Every ḥadīth is two things: the <b>isnād</b> — the chain of people who passed it on — and the
                  <b>matn</b> — the actual wording. The isnād is checked before the matn is ever trusted.</p>
                <svg class="hs-dia" viewBox="0 0 620 168" role="img" aria-label="A report is an isnād chain plus a matn text">
                  <text class="d-tag" x="12" y="24">ISNĀD · the chain</text>
                  @for (i of [0,1,2,3,4]; track i) {
                    <line class="d-edge" [attr.x1]="70 + i*118" y1="58" [attr.x2]="70 + i*118 + 88" y2="58" />
                  }
                  @for (n of anatomy; track n.x) {
                    <circle class="d-node" [attr.cx]="n.x" cy="58" r="12" [class.d-src]="n.src" [class.d-book]="n.book" />
                    <text class="d-lbl" [attr.x]="n.x" y="90">{{ n.t }}</text>
                  }
                  <line class="d-drop" x1="70" y1="70" x2="70" y2="120" />
                  <rect class="d-matn" x="70" y="118" width="480" height="36" rx="9" />
                  <text class="d-tag" x="82" y="112">MATN · the wording</text>
                  <text class="d-matn-t" x="90" y="141">“The deeds are but by intentions …”</text>
                </svg>
              </section>

              <!-- 2. Generations -->
              <section class="hs-sec">
                <h3><span class="hs-n">2</span> The journey across generations</h3>
                <p>A report travels hand to hand: the Prophet ﷺ tells a <b>Companion</b> (ṣaḥābī); the Companion
                  teaches a <b>Successor</b> (tābiʿī); and so on, generation by generation, until a <b>collector</b>
                  such as al-Bukhārī writes it into his book with the whole chain named.</p>
                <svg class="hs-dia" viewBox="0 0 620 132" role="img" aria-label="The Prophet to a Companion to a Successor to the collector">
                  @for (g of gens; track g.x) {
                    @if (!$last) { <line class="d-edge" [attr.x1]="g.x + 24" y1="46" [attr.x2]="g.x + 122" y2="46" /> }
                    <circle class="d-node" [attr.cx]="g.x" cy="46" [attr.r]="g.big ? 20 : 15"
                            [class.d-src]="g.src" [class.d-book]="g.book" />
                    @if (g.glyph) { <text class="d-glyph" [attr.x]="g.x" y="52">{{ g.glyph }}</text> }
                    <text class="d-lbl" [attr.x]="g.x" y="82">{{ g.t }}</text>
                    <text class="d-era" [attr.x]="g.x" y="99">{{ g.e }}</text>
                  }
                </svg>
              </section>

              <!-- 3. Grading -->
              <section class="hs-sec">
                <h3><span class="hs-n">3</span> The test of authenticity</h3>
                <p>For a chain to be graded <b>Ṣaḥīḥ</b> (sound), scholars require five things at once. Fall short on
                  precision and it is <b>Ḥasan</b> (good); break a condition and it is <b>Ḍaʿīf</b> (weak).</p>
                <div class="hs-tests">
                  @for (t of tests; track t.k) {
                    <div class="hs-test"><span class="ht-k">{{ t.k }}</span><span class="ht-d">{{ t.d }}</span></div>
                  }
                </div>
                <svg class="hs-dia grade" viewBox="0 0 620 120" role="img" aria-label="From the tests to a grade of sahih, hasan or daif">
                  <rect class="d-gate" x="232" y="46" width="156" height="30" rx="8" />
                  <text class="d-gate-t" x="310" y="66">the five tests</text>
                  <line class="d-edge" x1="388" y1="61" x2="470" y2="26" />
                  <line class="d-edge" x1="388" y1="61" x2="470" y2="61" />
                  <line class="d-edge" x1="388" y1="61" x2="470" y2="96" />
                  <g class="d-grade sahih"><rect x="470" y="12" width="138" height="28" rx="14"/><text x="539" y="30">Ṣaḥīḥ · sound</text></g>
                  <g class="d-grade hasan"><rect x="470" y="47" width="138" height="28" rx="14"/><text x="539" y="65">Ḥasan · good</text></g>
                  <g class="d-grade daif"><rect x="470" y="82" width="138" height="28" rx="14"/><text x="539" y="100">Ḍaʿīf · weak</text></g>
                </svg>
              </section>

              <!-- 4. Families -->
              <section class="hs-sec">
                <h3><span class="hs-n">4</span> The families of ḥadīth</h3>
                <p>Reports are classed along three axes — by how widely they were transmitted, by how connected the
                  chain is, and by their overall standing.</p>
                <div class="hs-cards">
                  @for (f of families; track f.h) {
                    <div class="hs-card">
                      <div class="hc-h">{{ f.h }}</div>
                      @for (r of f.rows; track r.k) {
                        <div class="hc-row"><b>{{ r.k }}</b><span>{{ r.d }}</span></div>
                      }
                    </div>
                  }
                </div>
              </section>

              <!-- 5. The sciences -->
              <section class="hs-sec">
                <h3><span class="hs-n">5</span> The sciences that guarded it</h3>
                <p>To trust a chain, scholars had to know each narrator and be sure each truly met the next. Two vast
                  disciplines made that possible.</p>
                <div class="hs-cards two">
                  <div class="hs-card">
                    <div class="hc-h">ʿIlm al-rijāl — the study of the men</div>
                    <p class="hc-p">Biographical dictionaries record every narrator: when they were born and died, whom
                      they studied under, whom they taught, and where they travelled — so a chain can be checked link by
                      link to confirm teacher and student actually met.</p>
                  </div>
                  <div class="hs-card">
                    <div class="hc-h">Al-jarḥ wa al-taʿdīl — critique &amp; endorsement</div>
                    <p class="hc-p">Each narrator was graded for honesty and precision on a fine scale, from “trustworthy”
                      to “abandoned.” A single weak or dishonest link, however famous, brings the whole report down.</p>
                  </div>
                </div>
                <p class="hs-foot">This is why the network on this page can name, for every report, the exact people who
                  carried it — and why its grade can be stated with confidence.</p>
              </section>
            </div>
          </div>
        </div>
      }
    </section>
  `,
})
export class IsnadComponent {
  data = httpResource<IsnadReport[]>(() => '/api/public/isnad', { defaultValue: [] });
  readonly cx = CX; readonly cy = CY; readonly R1 = R1;
  readonly Math = Math;

  hover = signal<string | null>(null);
  picked = signal<string | null>(null);
  hoverColl = signal<string | null>(null);
  helpOpen = signal(false);
  /** The Companion in focus — a click pins it, else hover. */
  active = computed(() => this.picked() ?? this.hover());

  @HostListener('document:keydown.escape') onEsc() { this.helpOpen.set(false); }

  /** Companions merged by canonical name, sized (capped range) and placed evenly on the ring,
   *  arranged so size tapers symmetrically from the largest at top — an even, balanced wheel. */
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
    // slot order around the circle: largest at top, then alternate clockwise / anti-clockwise
    const order: number[] = [0];
    let cw = 1, ccw = n - 1;
    while (order.length < n) { order.push(cw++); if (order.length < n) order.push(ccw--); }
    return list.map((c, k) => {
      const slot = order[k];
      const a = (-90 + (slot * 360) / n) * Math.PI / 180;
      return {
        ...c, a,
        x: CX + R1 * Math.cos(a), y: CY + R1 * Math.sin(a),
        r: Math.min(30, 11 + Math.sqrt(c.count) * 5),
      };
    });
  });

  private compByKey = computed(() => new Map(this.companions().map((c) => [c.key, c])));
  activeComp = computed(() => (this.active() ? this.compByKey().get(this.active()!) ?? null : null));
  top = computed(() => this.companions().slice(0, 8));

  /** One clamped label for the focused Companion, positioned just outside his node. */
  tip = computed(() => {
    const c = this.activeComp();
    if (!c) return null;
    const name = c.en || c.ar;
    const w = Math.min(230, name.length * 7.4 + 22);
    let x = c.x + (c.r + 16) * Math.cos(c.a);
    let y = c.y + (c.r + 16) * Math.sin(c.a);
    x = Math.max(w / 2 + 6, Math.min(1000 - w / 2 - 6, x));
    y = Math.max(16, Math.min(704, y));
    return { name, w, x, y };
  });

  /** The collections, placed along the base, sized by how many narrations they preserve. */
  collectors = computed<Collector[]>(() => {
    const by = new Map<string, { name: string; count: number }>();
    for (const r of this.data.value()) {
      const e = by.get(r.collection) ?? { name: r.collectorName, count: 0 };
      e.count++; by.set(r.collection, e);
    }
    const list = [...by.entries()].sort((a, b) => b[1].count - a[1].count);
    const xs = list.length === 1 ? [CX] : list.length === 2 ? [360, 640] : [500, 214, 786];
    return list.map(([key, v], i) => ({ key, name: v.name, count: v.count, x: xs[i] ?? CX, y: 676 }));
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
    const colls = this.collectors();
    const counts = new Map<string, number>();
    for (const r of c.reports) counts.set(r.collection, (counts.get(r.collection) ?? 0) + 1);
    return [...counts.entries()].map(([coll, num]) => {
      const k = colls.find((x) => x.key === coll);
      if (!k) return null;
      const mx = (c.x + k.x) / 2, my = (c.y + k.y) / 2 + 46;
      return { id: coll, d: `M ${c.x} ${c.y} Q ${mx} ${my} ${k.x} ${k.y - 21}`, w: 1.5 + num * 1.2 };
    }).filter((l): l is { id: string; d: string; w: number } => !!l);
  });

  pick(key: string) { this.picked.set(this.picked() === key ? null : key); }
  chainLine(r: IsnadReport): string { return r.chain.join('  ←  '); }
  pretty(g: string): string { return g.charAt(0) + g.slice(1).toLowerCase(); }

  // --- static diagram data for the help panel ---
  readonly anatomy = [
    { x: 70, t: 'collector', book: true, src: false },
    { x: 188, t: '', book: false, src: false },
    { x: 306, t: '', book: false, src: false },
    { x: 424, t: 'Companion', book: false, src: false },
    { x: 542, t: 'Prophet ﷺ', book: false, src: true },
  ];
  readonly gens = [
    { x: 58, t: 'Prophet ﷺ', e: 'the source', glyph: 'ﷺ', big: true, src: true, book: false },
    { x: 184, t: 'Companion', e: 'ṣaḥābī', glyph: '', big: false, src: false, book: false },
    { x: 310, t: 'Successor', e: 'tābiʿī', glyph: '', big: false, src: false, book: false },
    { x: 436, t: 'their successor', e: 'atbāʿ al-tābiʿīn', glyph: '', big: false, src: false, book: false },
    { x: 562, t: 'collector', e: 'al-Bukhārī', glyph: '', big: true, src: false, book: true },
  ];
  readonly tests = [
    { k: 'Ittiṣāl', d: 'the chain is unbroken — each narrator truly met the next' },
    { k: 'ʿAdālah', d: 'every narrator is upright and trustworthy in character' },
    { k: 'Ḍabṭ', d: 'every narrator is precise in memory or in his written record' },
    { k: 'No shudhūdh', d: 'it does not contradict a stronger, more reliable report' },
    { k: 'No ʿillah', d: 'it carries no hidden, subtle defect in chain or text' },
  ];
  readonly families = [
    { h: 'By how widely it spread', rows: [
      { k: 'Mutawātir', d: 'so many independent chains that error or collusion is impossible — certain' },
      { k: 'Āḥād', d: 'transmitted through one or a few chains — weighed on their strength' },
    ] },
    { h: 'By the chain’s connection', rows: [
      { k: 'Musnad / muttaṣil', d: 'a continuous chain, link to link, back to the Prophet ﷺ' },
      { k: 'Mursal / munqaṭiʿ', d: 'a link is missing or a Successor skips the Companion' },
    ] },
    { h: 'By overall standing', rows: [
      { k: 'Ṣaḥīḥ · Ḥasan', d: 'accepted — sound, or good and acted upon' },
      { k: 'Ḍaʿīf · Mawḍūʿ', d: 'weak, or fabricated — the latter rejected outright' },
    ] },
  ];
}
