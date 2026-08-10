import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';
import { ProphetSurahs } from './models';

interface Star { n: number; name: string; nameAr: string; count: number; x: number; y: number; size: number; }

const CX = 500, CY = 336, SPREAD = 46;   // phyllotaxis star-field geometry

@Component({
  selector: 'app-constellation',
  imports: [RouterLink],
  template: `
    <section class="cst">
      <div class="cst-head">
        <div class="eyebrow">The prophets across the Qur’ān</div>
        <h1>The <em>Constellation</em> of the Prophets</h1>
        <p class="cst-lede">Every star is a sūrah that names a prophet. Choose a prophet to trace his
          <b>constellation</b> — the sūrahs his story is told across. Some span the whole sky; some are a single point.</p>
      </div>

      @if (data.isLoading()) { <p class="state">Mapping the sky…</p> }
      @else {
        <div class="cst-stage">
          <div class="cst-sky">
            <svg viewBox="0 0 1000 672" role="img" aria-label="The prophets across the sūrahs of the Qur’an">
              <defs>
                <radialGradient id="starglow" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stop-color="#FFF3CE" /><stop offset="100%" stop-color="#C8A44B" stop-opacity="0" />
                </radialGradient>
              </defs>
              <!-- the traced constellation -->
              @if (line(); as d) {
                <path class="cst-line" [attr.d]="d" pathLength="1" />
              }
              <!-- the sūrah stars -->
              @for (s of stars(); track s.n) {
                <g class="cst-star" [class.lit]="lit(s.n)" [class.dim]="active() && !lit(s.n)"
                   (mouseenter)="hover.set(s.n)" (mouseleave)="hover.set(null)">
                  <circle class="halo" [attr.cx]="s.x" [attr.cy]="s.y" [attr.r]="s.size + 8" fill="url(#starglow)" />
                  <circle class="dot" [attr.cx]="s.x" [attr.cy]="s.y" [attr.r]="s.size"
                          [style.animation-delay.ms]="s.n * 53 % 4000" />
                  @if (lit(s.n) || hover() === s.n) {
                    <text class="slabel" [attr.x]="s.x" [attr.y]="s.y - s.size - 7">{{ s.name }}</text>
                  }
                </g>
              }
            </svg>
          </div>

          <aside class="cst-list">
            <div class="cst-list-h">Prophets · sūrahs</div>
            @for (p of ordered(); track p.chronicle) {
              <button class="cst-p" [class.on]="active() === p.chronicle"
                      (mouseenter)="active.set(p.chronicle)" (click)="active.set(p.chronicle)">
                <span class="g">{{ p.glyph }}</span>
                <span class="nm">{{ p.prophet }}</span>
                <span class="ct">{{ p.surahs.length }}</span>
              </button>
            }
          </aside>
        </div>

        <div class="cst-caption">
          @if (activeProphet(); as p) {
            <a class="cst-open" [routerLink]="['/c', p.chronicle]">{{ p.glyph }} {{ p.prophet }} →</a>
            <span class="cst-stat">{{ p.surahs.length }} sūrahs · {{ p.total }} āyāt</span>
            <span class="cst-surahs">{{ surahList(p) }}</span>
          } @else {
            <span class="cst-hint">Hover a prophet to light his constellation. {{ stars().length }} sūrahs name a prophet.</span>
          }
        </div>
      }
    </section>
  `,
})
export class ConstellationComponent {
  private router = inject(Router);
  data = httpResource<ProphetSurahs[]>(() => '/api/public/quran-map', { defaultValue: [] });

  active = signal<string | null>(null);
  hover = signal<number | null>(null);

  /** Prophets ordered by breadth of presence (widest constellation first). */
  ordered = computed(() => [...this.data.value()].sort((a, b) => b.surahs.length - a.surahs.length || b.total - a.total));

  /** Every distinct sūrah placed as a star (phyllotaxis), sized by total citations. */
  stars = computed<Star[]>(() => {
    const map = new Map<number, { name: string; nameAr: string; count: number }>();
    for (const p of this.data.value()) {
      for (const s of p.surahs) {
        const e = map.get(s.n) ?? { name: s.name, nameAr: s.nameAr, count: 0 };
        e.count += s.count;
        map.set(s.n, e);
      }
    }
    const entries = [...map.entries()].sort((a, b) => a[0] - b[0]);
    return entries.map(([n, info], i) => {
      const ang = i * 137.508 * Math.PI / 180;
      const r = SPREAD * Math.sqrt(i + 1);
      return {
        n, name: info.name, nameAr: info.nameAr, count: info.count,
        x: CX + r * Math.cos(ang), y: CY + r * Math.sin(ang),
        size: 2 + Math.min(7.5, Math.sqrt(info.count) * 1.7),
      };
    });
  });
  private starPos = computed(() => new Map(this.stars().map((s) => [s.n, s])));

  activeProphet = computed(() => this.data.value().find((p) => p.chronicle === this.active()) ?? null);

  /** The polyline path connecting the active prophet's sūrah-stars (by sūrah order). */
  line = computed<string | null>(() => {
    const p = this.activeProphet();
    if (!p) return null;
    const pos = this.starPos();
    const pts = p.surahs.map((s) => pos.get(s.n)).filter((s): s is Star => !!s).sort((a, b) => a.n - b.n);
    if (pts.length < 1) return null;
    return pts.map((s, i) => `${i ? 'L' : 'M'} ${s.x.toFixed(1)} ${s.y.toFixed(1)}`).join(' ');
  });
  private litSet = computed(() => new Set((this.activeProphet()?.surahs ?? []).map((s) => s.n)));
  lit(n: number): boolean { return this.litSet().has(n); }

  surahList(p: ProphetSurahs): string { return p.surahs.map((s) => s.name).join(' · '); }
}
