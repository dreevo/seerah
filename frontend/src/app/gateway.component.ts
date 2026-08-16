import {
  afterNextRender, Component, computed, effect, ElementRef, HostListener, inject, signal, viewChild,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';
import { ChronicleItem } from './models';

// A node in the prophetic genealogy. `slug` maps to a chronicle when one exists;
// otherwise it is a connector prophet (Ismāʿīl/Isḥāq/Yaʿqūb) shown to keep the
// lineage accurate. Coordinates are hand-authored in the 1600×1450 viewBox.
interface TreeNode {
  slug: string; parent: string | null; x: number; y: number; r: number; depth: number;
  connectorName?: string; note?: string; flagship?: boolean;
}

const LAYOUT: TreeNode[] = [
  { slug: 'adam', parent: null, x: 700, y: 58, r: 46, depth: 0 },
  { slug: 'idris', parent: 'adam', x: 700, y: 216, r: 40, depth: 1 },
  { slug: 'nuh', parent: 'idris', x: 700, y: 360, r: 44, depth: 2 },
  { slug: 'hud', parent: 'nuh', x: 360, y: 436, r: 40, depth: 3, note: "ʿĀd · via Sām" },
  { slug: 'salih', parent: 'nuh', x: 1040, y: 436, r: 40, depth: 3, note: 'Thamūd · via Sām' },
  { slug: 'ibrahim', parent: 'nuh', x: 700, y: 540, r: 50, depth: 3 },
  { slug: 'lut', parent: 'ibrahim', x: 452, y: 606, r: 40, depth: 4, note: 'his nephew' },
  { slug: 'shuayb', parent: 'ibrahim', x: 952, y: 606, r: 40, depth: 4, note: 'via Madyan' },
  { slug: 'ismail', parent: 'ibrahim', x: 520, y: 684, r: 24, depth: 4, connectorName: 'Ismāʿīl' },
  { slug: 'ishaq', parent: 'ibrahim', x: 892, y: 684, r: 24, depth: 4, connectorName: 'Isḥāq' },
  { slug: 'seerah', parent: 'ismail', x: 400, y: 864, r: 56, depth: 5, flagship: true },
  { slug: 'ayyub', parent: 'ishaq', x: 1262, y: 738, r: 40, depth: 5, note: 'via al-ʿĪṣ' },
  { slug: 'dhulkifl', parent: 'ayyub', x: 1444, y: 896, r: 36, depth: 6, note: 'held by some his son' },
  { slug: 'yaqub', parent: 'ishaq', x: 928, y: 804, r: 24, depth: 5, connectorName: 'Yaʿqūb' },
  { slug: 'yusuf', parent: 'yaqub', x: 690, y: 996, r: 42, depth: 6 },
  { slug: 'musa', parent: 'yaqub', x: 846, y: 1090, r: 42, depth: 6, note: 'through Lāwī' },
  { slug: 'yunus', parent: 'yaqub', x: 566, y: 1044, r: 42, depth: 6, note: 'through Bunyāmīn' },
  { slug: 'dawud', parent: 'yaqub', x: 1120, y: 972, r: 44, depth: 6, note: 'through Yahūdhā' },
  { slug: 'ilyas', parent: 'yaqub', x: 470, y: 1072, r: 38, depth: 6, note: 'house of Hārūn · via Lāwī' },
  { slug: 'alyasa', parent: 'ilyas', x: 372, y: 1240, r: 34, depth: 7, note: 'his successor' },
  { slug: 'isa', parent: 'dawud', x: 992, y: 1188, r: 42, depth: 7, note: 'through Maryam' },
  { slug: 'sulayman', parent: 'dawud', x: 1226, y: 1154, r: 42, depth: 7 },
  { slug: 'zakariyya', parent: 'dawud', x: 1446, y: 1138, r: 42, depth: 7, note: 'house of Dāwūd' },
  { slug: 'yahya', parent: 'zakariyya', x: 1486, y: 1338, r: 40, depth: 8, note: 'his son' },
];

const BRANCH_W = [15, 13, 11, 9, 6.5, 5, 4, 3.4, 3]; // by child depth

// Each node's parent, and the set of every ancestor up to Ādam — so hovering any
// prophet can light the exact line of descent that carries down to him.
const PARENT = new Map(LAYOUT.map((n) => [n.slug, n.parent] as const));
const ANCESTORS = new Map<string, Set<string>>();
for (const n of LAYOUT) {
  const set = new Set<string>();
  let cur: string | null = n.slug;
  while (cur) { set.add(cur); cur = PARENT.get(cur) ?? null; }
  ANCESTORS.set(n.slug, set);
}
// The main line of prophethood — the trunk that flows to the final Messenger ﷺ
// (Ādam → Idrīs → Nūḥ → Ibrāhīm → Ismāʿīl → Muhammad). Its edges carry the "river".
const TRUNK = new Set(['idris', 'nuh', 'ibrahim', 'ismail', 'seerah']);

// Tidy-tree horizontal layout (Reingold–Tilford flavour): pack the leaves at a
// uniform gap and centre every parent exactly over its children. The root then
// lands at the true balance point of the whole tree and the branches never bunch
// up — no matter how lopsided the genealogy is. Only x is computed here; the
// authored y (depth spacing) is kept.
const LEAF_GAP = 112;
const X: Map<string, number> = (() => {
  const kids = new Map<string, TreeNode[]>();
  for (const n of LAYOUT) {
    if (!n.parent) continue;
    if (!kids.has(n.parent)) kids.set(n.parent, []);
    kids.get(n.parent)!.push(n);
  }
  for (const list of kids.values()) list.sort((a, b) => a.x - b.x); // keep authored left→right order
  const x = new Map<string, number>();
  let cursor = 0;
  const place = (slug: string): number => {
    const cs = kids.get(slug) ?? [];
    let v: number;
    if (cs.length === 0) { v = cursor; cursor += LEAF_GAP; }
    else { const xs = cs.map((c) => place(c.slug)); v = (xs[0] + xs[xs.length - 1]) / 2; }
    x.set(slug, v);
    return v;
  };
  place('adam');
  // Pull the trunk's side-branches (peoples of ʿĀd/Thamūd/Madyan and Lūṭ) in
  // toward their parent, so they read as short wings instead of being flung to the
  // edges of Ibrāhīm's huge subtree with long horizontal branches.
  const wings: Record<string, string> = { hud: 'nuh', salih: 'nuh', lut: 'ibrahim', shuayb: 'ibrahim' };
  for (const [w, par] of Object.entries(wings)) {
    if (x.has(w) && x.has(par)) x.set(w, x.get(par)! + 0.4 * (x.get(w)! - x.get(par)!));
  }
  return x;
})();

// A viewBox centred on the root, tall enough for the depth and wide enough for
// whichever side reaches further — so the root is horizontally centred on screen.
const VIEW = (() => {
  const xs = LAYOUT.map((n) => X.get(n.slug)!);
  const ys = LAYOUT.map((n) => n.y);
  const rootX = X.get('adam')!;
  const halfW = Math.max(rootX - Math.min(...xs), Math.max(...xs) - rootX) + 78;
  const top = Math.min(...ys) - 62;
  const bottom = Math.max(...ys) + 96;
  return { x: rootX - halfW, y: top, w: 2 * halfW, h: bottom - top };
})();
const VIEWBOX = `${VIEW.x} ${VIEW.y} ${VIEW.w} ${VIEW.h}`;

@Component({
  selector: 'app-gateway',
  imports: [RouterLink],
  template: `
    <section class="gw-top">
      <div class="eyebrow">The Prophetic Library</div>
      <div class="basmala">بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ</div>
      <h1>The <em>Tree</em> of the Prophets</h1>
      <p class="gw-lede">One connected chain of guidance — from Ādam, the father of humankind, to Muhammad ﷺ,
        the final Messenger. <b>Hover a prophet to trace his descent — click to open his story or timeline.</b></p>
    </section>

    <!-- LEFT: a trisected disc — three ways to see the prophets' stories; hover a wedge to reveal it -->
    <div class="gw-lenses">
      <svg class="gw-pie" viewBox="0 0 140 140" role="group" aria-label="Three ways to explore the prophets' stories">
        <path class="pie-wedge" [class.on]="lensHover() === 'way'" d="M70 70 L70 6 A64 64 0 0 1 125.4 102 Z"
              (mouseenter)="lensHover.set('way')" (mouseleave)="lensHover.set(null)" (click)="goLens('/the-way')"
              (keydown.enter)="goLens('/the-way')" tabindex="0" role="link" aria-label="The Way of the Messengers" />
        <path class="pie-wedge" [class.on]="lensHover() === 'stars'" d="M70 70 L125.4 102 A64 64 0 0 1 14.6 102 Z"
              (mouseenter)="lensHover.set('stars')" (mouseleave)="lensHover.set(null)" (click)="goLens('/constellation')"
              (keydown.enter)="goLens('/constellation')" tabindex="0" role="link" aria-label="The Constellation" />
        <path class="pie-wedge" [class.on]="lensHover() === 'chain'" d="M70 70 L14.6 102 A64 64 0 0 1 70 6 Z"
              (mouseenter)="lensHover.set('chain')" (mouseleave)="lensHover.set(null)" (click)="goLens('/isnad')"
              (keydown.enter)="goLens('/isnad')" tabindex="0" role="link" aria-label="How the Narrations Reach Us" />
        <g class="pie-div"><line x1="70" y1="70" x2="70" y2="6" /><line x1="70" y1="70" x2="125.4" y2="102" /><line x1="70" y1="70" x2="14.6" y2="102" /></g>
        <circle class="pie-rim" cx="70" cy="70" r="64" />
        <!-- emblems (transparent to the pointer so the wedge under them still lights) -->
        <g class="pie-emb" [class.on]="lensHover() === 'way'" transform="translate(99.4 53)">
          <g transform="scale(.62) translate(-20 -12)">
            <path d="M4 12 H36" /><circle cx="4" cy="12" r="2.6"/><circle cx="12" cy="12" r="2.6"/>
            <circle cx="20" cy="12" r="2.6"/><circle cx="28" cy="12" r="2.6"/><circle cx="36" cy="12" r="2.6"/>
          </g>
        </g>
        <g class="pie-emb stars" [class.on]="lensHover() === 'stars'" transform="translate(70 104)">
          <g transform="scale(.62) translate(-20 -12)">
            <path d="M6 16 L14 8 L20 14 L26 8 L34 16" />
            <circle cx="6" cy="16" r="2.2"/><circle cx="14" cy="8" r="2.7"/><circle cx="20" cy="14" r="1.9"/>
            <circle cx="26" cy="8" r="2.7"/><circle cx="34" cy="16" r="2.2"/>
          </g>
        </g>
        <g class="pie-emb chains" [class.on]="lensHover() === 'chain'" transform="translate(40.6 53)">
          <g transform="scale(.62) translate(-20 -12)">
            <path d="M6 12 C 14 4, 18 20, 26 12 S 34 4, 38 12" />
            <circle cx="6" cy="12" r="2.7"/><circle cx="20" cy="12" r="1.9"/><circle cx="34" cy="12" r="2.7"/>
          </g>
        </g>
      </svg>
      @if (lensInfo(); as i) { <div class="gw-pie-tip"><b>{{ i.n }}</b><em>{{ i.d }}</em></div> }
      <div class="gw-pie-cap"><b>Ways to Explore</b><em>the prophets, seen three ways</em></div>
    </div>

    <!-- RIGHT: seven lanterns — the Qur'an's stories -->
    <div class="gw-lanterns">
      <svg class="qs-lanterns" viewBox="0 0 300 164" role="group" aria-label="Stories of the Qur'an — seven lanterns">
        <defs>
          <radialGradient id="lglow" cx="50%" cy="46%" r="55%">
            <stop offset="0%" stop-color="#FFE9A8" /><stop offset="50%" stop-color="#E8A33C" stop-opacity=".9" />
            <stop offset="100%" stop-color="#E8A33C" stop-opacity="0" />
          </radialGradient>
          <linearGradient id="lbodyg" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="#3a2c10" /><stop offset="100%" stop-color="#201709" />
          </linearGradient>
        </defs>
        <path class="lbranch" d="M12 18 C 96 52, 204 52, 288 18" />
        @for (l of lanterns; track l.slug) {
          <g [attr.transform]="'translate(' + l.x + ' ' + l.y + ')'">
            <g class="lswing" [class.on]="lanternHover() === l.slug" [style.animation-delay.s]="l.delay"
               (mouseenter)="lanternHover.set(l.slug)" (mouseleave)="lanternHover.set(null)"
               (click)="goStory(l.slug)" (keydown.enter)="goStory(l.slug)" tabindex="0" role="link"
               [attr.aria-label]="l.name">
              <circle class="lhalo" cx="0" cy="31" r="17" fill="url(#lglow)" [style.animation-delay.s]="l.delay" />
              <line class="lthread" x1="0" y1="0" x2="0" y2="14" />
              <circle class="lring" cx="0" cy="14" r="2" />
              <path class="lcap" d="M-5 17 L5 17 L4 20 L-4 20 Z" />
              <path class="lbody" d="M-5 20 C -5 17 5 17 5 20 L5 41 C5 44 -5 44 -5 41 Z" />
              <ellipse class="lflame" cx="0" cy="31" rx="4.3" ry="7" fill="url(#lglow)" [style.animation-delay.s]="l.delay" />
              <line class="lpane" x1="-1.8" y1="21" x2="-1.8" y2="40" /><line class="lpane" x1="1.8" y1="21" x2="1.8" y2="40" />
              <line class="lfin" x1="0" y1="44" x2="0" y2="47" /><circle class="lknob" cx="0" cy="48" r="1.5" />
              @if (lanternHover() === l.slug) { <text class="lname" x="0" y="62">{{ l.name }}</text> }
            </g>
          </g>
        }
        <text class="qsl-title" x="150" y="150" text-anchor="middle" role="link" tabindex="0"
              (click)="openStories()" (keydown.enter)="openStories()">Stories of the Qur’ān</text>
      </svg>
    </div>

    <div class="gw-trace" [class.on]="current()">
      @if (traceLabel(); as t) { <span class="gw-trace-line">{{ t }}</span> }
      @else { <span class="gw-trace-hint">The line of descent appears here as you trace it ↑</span> }
    </div>

    @if (chronicles.error()) {
      <p class="state err">The library service is unavailable. Is the backend running on :8080?</p>
    } @else {
      <p class="ptree-hint">Drag to explore the tree · pinch to zoom · tap a prophet to open</p>
      <div class="ptree" #tree>
        <svg #svg [attr.viewBox]="viewBox" role="img" aria-label="Genealogy of the prophets" [class.tracing]="current()">
          <defs>
            <radialGradient id="leaf" cx="42%" cy="34%" r="72%">
              <stop offset="0%" stop-color="#1f4a63" /><stop offset="60%" stop-color="#123a52" /><stop offset="100%" stop-color="#0b2537" />
            </radialGradient>
            <radialGradient id="leaf-flag" cx="42%" cy="34%" r="72%">
              <stop offset="0%" stop-color="#3a3a22" /><stop offset="55%" stop-color="#2a2914" /><stop offset="100%" stop-color="#171708" />
            </radialGradient>
            <linearGradient id="bark" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#C8A44B" /><stop offset="100%" stop-color="#6E551F" />
            </linearGradient>
            <filter id="soft" x="-40%" y="-40%" width="180%" height="180%">
              <feGaussianBlur stdDeviation="6" />
            </filter>
          </defs>

          <!-- drifting light motes -->
          <g class="motes">
            @for (m of motes; track m.i) {
              <circle class="mote" [attr.cx]="m.x" [attr.cy]="m.y" [attr.r]="m.r" [style.animation-delay.s]="m.d" [style.animation-duration.s]="m.dur" />
            }
          </g>

          <!-- branches grow from the root outward -->
          @for (b of branches(); track b.id) {
            <path class="branch" [class.lit]="branchLit(b.id)" [attr.d]="b.d" [attr.stroke-width]="b.w" pathLength="1" [style.animation-delay.ms]="b.delay" />
            <path class="branch-core" [class.lit]="branchLit(b.id)" [attr.d]="b.d" [attr.stroke-width]="b.w * 0.4" pathLength="1" [style.animation-delay.ms]="b.delay + 120" />
            @if (b.trunk) {
              <path class="river" [attr.d]="b.d" pathLength="1" [style.animation-delay.ms]="b.riverDelay" />
            }
          }

          <!-- prophet nodes -->
          @for (n of nodes(); track n.slug) {
            <g class="pnode {{ n.kind }}" [class.flagship]="n.flagship" [class.lit]="lit(n.slug)"
               [class.open]="current() === n.slug"
               [attr.transform]="'translate(' + n.x + ' ' + n.y + ')'" [style.animation-delay.ms]="n.delay"
               [attr.tabindex]="n.kind === 'chronicle' ? 0 : null" [attr.role]="n.kind === 'chronicle' ? 'button' : null"
               (mouseenter)="open(n.slug)" (mouseleave)="scheduleClose()"
               (focus)="open(n.slug)" (blur)="scheduleClose()"
               (click)="tap(n, $event)" (keydown.enter)="tap(n, $event)">
              @if (n.kind === 'chronicle') {
                <circle class="glow" [attr.r]="n.r + 16" filter="url(#soft)" />
                <circle class="disc" [attr.r]="n.r" [attr.fill]="n.flagship ? 'url(#leaf-flag)' : 'url(#leaf)'" />
                <circle class="rim" [attr.r]="n.r" />
                <text class="pglyph" [attr.font-size]="n.r * 0.9" y="0" dy="0.32em">{{ n.glyph }}</text>
                <text class="pname" [attr.y]="n.r + 26">{{ n.name }}</text>
                <text class="pcount" [attr.y]="n.r + 46">{{ n.count }} events</text>
                @if (n.note) { <text class="pnote" [attr.y]="-n.r - 12">{{ n.note }}</text> }
              } @else {
                <circle class="cdot" r="7" />
                <text class="cname" y="-14">{{ n.connectorName }}</text>
              }
            </g>
          }
        </svg>

        <!-- the choice that blooms from a node: enter the timeline, or play the story -->
        @if (popNode(); as n) {
          @if (popPos(); as p) {
            <div class="np-pop" [class.below]="p.below" [style.left.px]="p.x"
                 [style.top.px]="p.below ? p.y + p.rpx + 13 : p.y - p.rpx - 13"
                 (mouseenter)="hold()" (mouseleave)="scheduleClose()">
              <div class="np-head"><span class="np-glyph">{{ n.glyph }}</span>{{ n.name }}</div>
              <div class="np-actions">
                <a class="np-btn story" [routerLink]="['/c', n.slug, 'story']" (click)="pinned.set(null)">
                  <span class="np-ic">▶</span><span class="np-l"><b>Play the Story</b><em>{{ n.count }} scenes, told with āyāt</em></span>
                </a>
                <a class="np-btn" [routerLink]="['/c', n.slug]" (click)="pinned.set(null)">
                  <span class="np-ic">❯</span><span class="np-l"><b>Timeline</b><em>the connected chronology</em></span>
                </a>
              </div>
            </div>
          }
        }

      </div>
      <p class="tl-hint">Every chronicle is reviewed, cited content · No depiction of prophets or companions · Peace be upon them all</p>
    }
  `,
})
export class GatewayComponent {
  private router = inject(Router);
  chronicles = httpResource<ChronicleItem[]>(() => '/api/public/chronicles', { defaultValue: [] });

  readonly viewBox = VIEWBOX;

  // The Qur'an's stories, hung as seven lanterns from a bough beside the tree.
  // x/y place each lantern's hanging-point along the drooping branch; delay
  // staggers the sway and flame flicker so they never move in unison.
  lensHover = signal<string | null>(null);
  lanternHover = signal<string | null>(null);

  /** The name + descriptor of the pie wedge currently hovered, for the reveal card. */
  lensInfo = computed<{ n: string; d: string } | null>(() => {
    switch (this.lensHover()) {
      case 'way': return { n: 'The Way of the Messengers', d: 'one recurring pattern across the nations' };
      case 'stars': return { n: 'The Constellation', d: 'the prophets across the sūrahs' };
      case 'chain': return { n: 'How the Narrations Reach Us', d: 'the chains of isnād behind the ḥadīth' };
      default: return null;
    }
  });
  goLens(route: string) { this.router.navigate([route]); }
  readonly lanterns = [
    { slug: 'ashab-al-kahf', name: 'The Cave', x: 24, y: 22, delay: 0 },
    { slug: 'dhul-qarnayn', name: 'Dhū al-Qarnayn', x: 66, y: 33, delay: 0.7 },
    { slug: 'luqman-the-wise', name: 'Luqmān', x: 108, y: 41, delay: 1.4 },
    { slug: 'ashab-al-ukhdud', name: 'The Ditch', x: 150, y: 44, delay: 0.4 },
    { slug: 'the-people-of-saba', name: 'Sabaʾ', x: 192, y: 41, delay: 1.1 },
    { slug: 'ashab-al-sabt', name: 'The Sabbath', x: 234, y: 33, delay: 1.8 },
    { slug: 'ashab-al-fil', name: 'The Elephant', x: 276, y: 22, delay: 0.3 },
  ];
  goStory(slug: string) { this.router.navigate(['/event', slug]); }
  openStories() { this.router.navigate(['/stories']); }

  private svg = viewChild<ElementRef<SVGSVGElement>>('svg');
  private tree = viewChild<ElementRef<HTMLElement>>('tree');

  /** The prophet being hovered/focused (transient). */
  active = signal<string | null>(null);
  /** The prophet whose choice-popover is held open by a click/tap (sticky, for touch). */
  pinned = signal<string | null>(null);
  /** Whichever prophet is in focus right now — pinned wins, else hovered. */
  current = computed(() => this.active() ?? this.pinned());
  private closeT?: ReturnType<typeof setTimeout>;

  constructor() {
    // Keep the popover anchored to its node whenever the focus changes.
    effect(() => { const n = this.popNode(); if (n) this.place(n); });
    // On phones the tree is a drag-to-pan canvas — open it centred on the trunk (Ādam).
    afterNextRender(() => {
      const el = this.tree()?.nativeElement;
      if (el && matchMedia('(max-width: 760px)').matches) {
        requestAnimationFrame(() => { el.scrollLeft = (el.scrollWidth - el.clientWidth) / 2; });
      }
    });
  }

  /** A node is lit when it lies on the focused prophet's line of descent. */
  lit(slug: string): boolean {
    const a = this.current();
    return !!a && ANCESTORS.get(a)!.has(slug);
  }
  /** A branch (keyed by its child slug) is lit when the child is on that line. */
  branchLit(childSlug: string): boolean {
    const a = this.current();
    return !!a && ANCESTORS.get(a)!.has(childSlug);
  }

  // --- the node choice-popover (Timeline vs Story) -------------------------

  open(slug: string) { clearTimeout(this.closeT); this.active.set(slug); }
  hold() { clearTimeout(this.closeT); }
  /** Close on leave — but a short grace lets the pointer travel onto the popover. */
  scheduleClose() { clearTimeout(this.closeT); this.closeT = setTimeout(() => this.active.set(null), 170); }

  /** Click/tap a node: pin its popover open (so touch users get the choice too). */
  tap(n: { kind: string; slug: string }, e: Event) {
    if (n.kind !== 'chronicle') return;
    e.stopPropagation();
    this.pinned.set(this.pinned() === n.slug ? null : n.slug);
    this.active.set(this.pinned());
  }
  @HostListener('document:click', ['$event'])
  onDocClick(e: Event) {
    if (!this.pinned()) return;
    const t = e.target as HTMLElement;
    if (t.closest('.pnode') || t.closest('.np-pop')) return;
    this.pinned.set(null);
  }
  @HostListener('window:resize')
  onResize() { const n = this.popNode(); if (n) this.place(n); }

  /** The focused node, only when it's a real chronicle (connectors have no popover). */
  popNode = computed(() => {
    const s = this.current();
    if (!s) return null;
    const n = this.nodes().find((x) => x.slug === s);
    return n && n.kind === 'chronicle' ? n : null;
  });
  popPos = signal<{ x: number; y: number; rpx: number; below: boolean } | null>(null);

  /** Project a node's SVG-space centre to a pixel offset within the tree container. */
  private place(n: { x: number; y: number; r: number }) {
    const svg = this.svg()?.nativeElement, host = this.tree()?.nativeElement;
    if (!svg || !host) return;
    const ctm = svg.getScreenCTM();
    if (!ctm) return;
    const pt = svg.createSVGPoint();
    pt.x = n.x; pt.y = n.y;
    const s = pt.matrixTransform(ctm);
    const hr = host.getBoundingClientRect();
    const y = s.y - hr.top, rpx = n.r * ctm.a;
    this.popPos.set({ x: s.x - hr.left, y, rpx, below: y - rpx < 130 });
  }

  private displayName = computed(() => {
    const byslug = new Map(this.chronicles.value().map((c) => [c.slug, c]));
    const m = new Map<string, string>();
    for (const n of LAYOUT) {
      const c = byslug.get(n.slug);
      m.set(n.slug, c ? c.title.replace(/^The Story of Prophet |^The Life of the Prophet /, '') : (n.connectorName ?? n.slug));
    }
    return m;
  });

  /** The traced line spelled out, Ādam → … → the focused prophet. */
  traceLabel = computed(() => {
    const a = this.current();
    if (!a) return '';
    const path: string[] = [];
    for (let cur: string | null = a; cur; cur = PARENT.get(cur) ?? null) path.unshift(cur);
    const name = this.displayName();
    return path.map((s) => name.get(s) ?? s).join('  ›  ');
  });

  readonly motes = Array.from({ length: 36 }, (_, i) => ({
    i, x: VIEW.x + Math.random() * VIEW.w, y: VIEW.y + Math.random() * VIEW.h,
    r: 1 + Math.random() * 2.4, d: +(Math.random() * 8).toFixed(1), dur: +(7 + Math.random() * 8).toFixed(1),
  }));

  nodes = computed(() => {
    const byslug = new Map(this.chronicles.value().map((c) => [c.slug, c]));
    return LAYOUT.map((n) => {
      const c = byslug.get(n.slug);
      return {
        ...n,
        x: X.get(n.slug)!,
        kind: c ? 'chronicle' : 'connector',
        name: c?.title.replace(/^The Story of Prophet |^The Life of the Prophet /, '') ?? n.connectorName ?? '',
        glyph: c?.glyph ?? '',
        count: c?.eventCount ?? 0,
        delay: 700 + n.depth * 260,
      };
    });
  });

  branches = computed(() => {
    return LAYOUT.filter((n) => n.parent).map((n) => {
      const px = X.get(n.parent!)!, cx = X.get(n.slug)!;
      const py = LAYOUT.find((m) => m.slug === n.parent)!.y;
      const my = (py + n.y) / 2;
      return {
        id: n.slug, w: BRANCH_W[Math.min(n.depth, BRANCH_W.length - 1)],
        d: `M ${px} ${py} C ${px} ${my}, ${cx} ${my}, ${cx} ${n.y}`,
        delay: 200 + n.depth * 260,
        trunk: TRUNK.has(n.slug),
        riverDelay: 1400 + n.depth * 500,   // a bead of light travels down the trunk, in order
      };
    });
  });
}
